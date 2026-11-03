package com.agrogestao.api.service.despesa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.relatorio.ProtecaoEmissao;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** "Hoje" é 11/10/2026 em Fortaleza; data do fato padrão 2026-03-10 (massa da Q00b). */
class DespesaServiceTest {
  static final LocalDate FATO = LocalDate.of(2026, 3, 10);
  static final String SEM_PLANTIO = "A despesa precisa estar vinculada a pelo menos um plantio.";

  DespesaRepository repo = mock(DespesaRepository.class);
  ProdutorRepository produtores = mock(ProdutorRepository.class);
  CategoriaDespesaRepository categorias = mock(CategoriaDespesaRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  RateioDespesaRepository rateios = mock(RateioDespesaRepository.class);
  EmissaoRelatorioRepository emissoes = mock(EmissaoRelatorioRepository.class);
  RateioDespesaService rateio = spy(new RateioDespesaService());
  LancamentoService lancamentos =
      spy(
          new LancamentoService(
              Clock.fixed(Instant.parse("2026-10-12T01:30:00Z"), ZoneId.of("America/Fortaleza"))));
  DespesaService service =
      new DespesaService(
          repo,
          produtores,
          categorias,
          plantios,
          rateios,
          rateio,
          lancamentos,
          new ProtecaoEmissao(emissoes, produtores));

  Produtor produtor = comId(new Produtor(), 1L);
  Produtor vizinho = comId(new Produtor(), 9L);
  CategoriaDespesa adubo = comId(new CategoriaDespesa(), 2L);
  CategoriaDespesa diaria = comId(new CategoriaDespesa(), 3L);
  Plantio p1 = plantio(11L, "1.000", produtor);
  Plantio p2 = plantio(12L, "1.000", produtor);
  Plantio p3 = plantio(13L, "1.000", produtor);
  Plantio outro = plantio(19L, "1.000", vizinho);

  static <T extends EntidadeAuditada> T comId(T entidade, Long id) {
    ReflectionTestUtils.setField(entidade, "id", id);
    return entidade;
  }

  static Plantio plantio(Long id, String area, Produtor dono) {
    Propriedade propriedade = new Propriedade();
    propriedade.setProdutor(dono);
    Plantio plantio = comId(new Plantio(), id);
    plantio.setPropriedade(propriedade);
    plantio.setArea(new BigDecimal(area));
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    return plantio;
  }

  @BeforeEach
  void preparar() {
    adubo.setNome("Adubo e fertilizantes");
    diaria.setNome("Diária");
    diaria.setSistema(true);
    when(produtores.findById(1L)).thenReturn(Optional.of(produtor));
    // A03b: o produtor é travado antes de a despesa ser lida.
    when(produtores.buscarParaAtualizar(1L)).thenReturn(Optional.of(produtor));
    when(repo.buscarProdutorId(anyLong()))
        .thenAnswer(inv -> repo.findById(inv.getArgument(0)).map(d -> d.getProdutor().getId()));
    when(categorias.findById(2L)).thenReturn(Optional.of(adubo));
    when(categorias.findById(3L)).thenReturn(Optional.of(diaria));
    when(plantios.findAllById(any()))
        .thenAnswer(
            inv -> {
              Collection<Long> ids = inv.getArgument(0);
              return List.of(p1, p2, p3, outro).stream()
                  .filter(p -> ids.contains(p.getId()))
                  .toList();
            });
    // L04b: os plantios são lidos travados.
    when(plantios.buscarParaAtualizar(anyLong()))
        .thenAnswer(
            inv ->
                List.of(p1, p2, p3, outro).stream()
                    .filter(p -> p.getId().equals(inv.getArgument(0)))
                    .findFirst());
    when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  static DespesaRequisicao entrada(String valor, Long categoria, List<Long> plantioIds) {
    return new DespesaRequisicao(
        1L, categoria, "Adubo de teste", new BigDecimal(valor), FATO, plantioIds);
  }

  static DespesaRequisicao entrada(String valor, List<Long> plantioIds) {
    return entrada(valor, 2L, plantioIds);
  }

  void rejeita(DespesaRequisicao dto, Class<? extends RuntimeException> tipo, String mensagem) {
    assertThatThrownBy(() -> service.criar(dto)).isInstanceOf(tipo).hasMessage(mensagem);
    verify(repo, never()).saveAndFlush(any());
    verify(repo, never()).save(any());
    verifyNoInteractions(rateios);
    verify(rateio, never()).ratear(any(), any());
  }

  Despesa gravada() {
    ArgumentCaptor<Despesa> captor = ArgumentCaptor.forClass(Despesa.class);
    verify(repo, atLeastOnce()).saveAndFlush(captor.capture());
    return captor.getValue();
  }

  @Test
  void deveGravarRateioIntegralQuandoDespesaTiverUmPlantio() {
    var resposta = service.criar(entrada("150.00", List.of(11L)));
    Despesa despesa = gravada();
    assertThat(despesa.getValorTotal()).isEqualByComparingTo("150.00");
    assertThat(despesa.getCategoriaDespesa()).isSameAs(adubo);
    assertThat(despesa.getDataFato()).isEqualTo(FATO);
    assertThat(despesa.isEstornada()).isFalse();
    assertThat(despesa.getDiaria()).isNull();
    assertThat(despesa.getRateios()).hasSize(1);
    RateioDespesa linha = despesa.getRateios().get(0);
    assertThat(linha.getPlantio()).isSameAs(p1);
    assertThat(linha.getDespesa()).isSameAs(despesa);
    assertThat(linha.getPercentual()).isEqualByComparingTo("1.000000");
    assertThat(linha.getValorRateado()).isEqualByComparingTo("150.00");
    assertThat(resposta.rateios())
        .singleElement()
        .satisfies(r -> assertThat(r.valorRateado()).isEqualByComparingTo("150.00"));
  }

  @Test
  void deveRejeitarSemGravarQuandoDespesaNaoTiverPlantio() {
    rejeita(entrada("150.00", List.of()), RegraNegocioException.class, SEM_PLANTIO);
    rejeita(entrada("150.00", null), RegraNegocioException.class, SEM_PLANTIO);
  }

  @Test
  void deveRejeitarSemGravarQuandoValorNaoForPositivo() {
    for (String valor : List.of("0.00", "-10.00"))
      rejeita(
          entrada(valor, List.of(11L)),
          RegraNegocioException.class,
          "O valor da despesa deve ser maior que zero.");
  }

  @Test
  void deveRejeitarSemGravarQuandoPlantioSeRepetir() {
    rejeita(
        entrada("90.00", List.of(11L, 12L, 11L)),
        RegraNegocioException.class,
        "O mesmo plantio foi informado mais de uma vez.");
  }

  @Test
  void deveRejeitarSemGravarQuandoCategoriaForDeSistema() {
    rejeita(
        entrada("90.00", 3L, List.of(11L)),
        RegraNegocioException.class,
        "A categoria Diária é lançada automaticamente pela diária paga e não pode ser usada em"
            + " despesa manual.");
  }

  @Test
  void deveRejeitarSemGravarQuandoPlantioForDeOutroProdutor() {
    rejeita(
        entrada("90.00", List.of(11L, 19L)),
        RegraNegocioException.class,
        "Todos os plantios da despesa devem ser do produtor informado.");
  }

  @Test
  void deveRejeitarSemGravarQuandoValorNaoCobrirUmCentavoPorPlantio() {
    rejeita(
        entrada("0.02", List.of(11L, 12L, 13L)),
        RegraNegocioException.class,
        "O valor da despesa não cobre um centavo por plantio. Vincule menos plantios.");
  }

  @Test
  void deveAceitarUmCentavoPorPlantioQuandoValorCobrirExatamente() {
    var resposta = service.criar(entrada("0.03", List.of(11L, 12L, 13L)));
    assertThat(resposta.rateios())
        .extracting(RateioDespesaResposta::valorRateado)
        .allSatisfy(v -> assertThat(v).isEqualByComparingTo("0.01"));
  }

  @Test
  void deveResponder404SemGravarQuandoVinculoNaoExistir() {
    rejeita(
        new DespesaRequisicao(
            8L, 2L, "Adubo de teste", new BigDecimal("90.00"), FATO, List.of(11L)),
        RecursoNaoEncontradoException.class,
        "Produtor não encontrado.");
    rejeita(
        entrada("90.00", 7L, List.of(11L)),
        RecursoNaoEncontradoException.class,
        "Categoria de despesa não encontrada.");
    rejeita(
        entrada("90.00", List.of(11L, 99L)),
        RecursoNaoEncontradoException.class,
        "Plantio não encontrado.");
  }

  @Test
  void deveCitarPlantioRepetidoQuandoTambemForCategoriaDeSistema() {
    rejeita(
        entrada("90.00", 3L, List.of(11L, 11L)),
        RegraNegocioException.class,
        "O mesmo plantio foi informado mais de uma vez.");
  }

  @Test
  void deveDelegarRn04ERn35QuandoLancarOuAlterar() {
    service.criar(entrada("90.00", List.of(12L, 11L)));
    verify(lancamentos).validarDataFato(FATO, List.of(p2, p1));
    Despesa despesa = gravada();
    when(repo.findById(5L)).thenReturn(Optional.of(despesa));
    service.alterar(5L, entrada("90.00", List.of(11L, 12L, 13L)));
    verify(lancamentos).validarDataFato(FATO, List.of(p1, p2, p3));
  }

  @Test
  void deveRejeitarSemGravarQuandoDataDoFatoForFutura() {
    rejeita(
        new DespesaRequisicao(
            1L,
            2L,
            "Adubo de teste",
            new BigDecimal("90.00"),
            LocalDate.of(2026, 10, 12),
            List.of(11L)),
        RegraNegocioException.class,
        "A data do fato não pode ser posterior a hoje.");
  }

  /** Cada entrada que o RateioDespesaService rejeita é barrada antes, com 400 ou 422. */
  @Test
  void deveBarrarAntesDoRateioQuandoEntradaFariaRateioLancarExcecao() {
    rejeita(
        entrada("0.00", List.of(11L)),
        RegraNegocioException.class,
        "O valor da despesa deve ser maior que zero.");
    rejeita(entrada("90.00", List.of()), RegraNegocioException.class, SEM_PLANTIO);
    rejeita(
        entrada("90.00", List.of(11L, 11L)),
        RegraNegocioException.class,
        "O mesmo plantio foi informado mais de uma vez.");
    try (var fabrica = Validation.buildDefaultValidatorFactory()) {
      Validator validador = fabrica.getValidator();
      assertThat(validador.validate(entrada("90.001", List.of(11L))))
          .extracting(v -> v.getPropertyPath().toString())
          .containsExactly("valorTotal");
      assertThat(
              validador.validate(
                  new DespesaRequisicao(1L, 2L, "Adubo de teste", null, FATO, List.of(11L))))
          .extracting(v -> v.getPropertyPath().toString())
          .containsExactly("valorTotal");
      List<Long> comNulo = new ArrayList<>(List.of(11L));
      comNulo.add(null);
      assertThat(validador.validate(entrada("90.00", comNulo))).isNotEmpty();
    }
  }

  @Test
  void deveAceitarQuandoProdutorNaoTiverConsentimento() {
    assertThat(produtor.getConsentimentoEm()).isNull();
    assertThatNoException().isThrownBy(() -> service.criar(entrada("90.00", List.of(11L))));
  }

  @Test
  void deveManterRateioGravadoQuandoAlteracaoNaoMudarValorNemPlantios() {
    service.criar(entrada("90.00", List.of(11L, 12L)));
    Despesa despesa = gravada();
    List<RateioDespesa> antes = List.copyOf(despesa.getRateios());
    p2.setArea(new BigDecimal("0.500"));
    when(repo.findById(5L)).thenReturn(Optional.of(despesa));
    var dto =
        new DespesaRequisicao(
            1L, 2L, "Descrição corrigida", new BigDecimal("90.00"), FATO, List.of(12L, 11L));
    service.alterar(5L, dto);
    assertThat(despesa.getDescricao()).isEqualTo("Descrição corrigida");
    assertThat(despesa.getRateios()).containsExactlyElementsOf(antes);
    assertThat(despesa.getRateios())
        .extracting(RateioDespesa::getValorRateado)
        .containsExactly(new BigDecimal("45.00"), new BigDecimal("45.00"));
    verify(rateio, times(1)).ratear(any(), any());
  }

  @Test
  void deveRecalcularNaMesmaLinhaQuandoValorMudar() {
    service.criar(entrada("90.00", List.of(11L, 12L)));
    Despesa despesa = gravada();
    RateioDespesa linhaP1 = despesa.getRateios().get(0);
    p2.setArea(new BigDecimal("0.500"));
    when(repo.findById(5L)).thenReturn(Optional.of(despesa));
    service.alterar(5L, entrada("120.00", List.of(11L, 12L)));
    assertThat(despesa.getRateios().get(0)).isSameAs(linhaP1);
    assertThat(despesa.getRateios())
        .extracting(RateioDespesa::getValorRateado)
        .containsExactly(new BigDecimal("80.00"), new BigDecimal("40.00"));
  }

  @Test
  void deveResponder404QuandoDespesaNaoExistir() {
    when(repo.findById(5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.alterar(5L, entrada("90.00", List.of(11L))))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Despesa não encontrada.");
    assertThatThrownBy(() -> service.detalhar(5L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  // L04b

  Despesa existente(Long id, Diaria geradora) {
    service.criar(entrada("90.00", List.of(11L, 12L)));
    Despesa despesa = gravada();
    comId(despesa, id);
    despesa.setDiaria(geradora);
    when(repo.findById(id)).thenReturn(Optional.of(despesa));
    clearInvocations(plantios, repo);
    return despesa;
  }

  @Test
  void deveRecusarExclusaoSemApagarQuandoDespesaForGeradaPorDiaria() {
    existente(5L, comId(new Diaria(), 40L));
    assertThatThrownBy(() -> service.excluir(5L))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("Esta despesa foi gerada por uma diária paga. Altere ou exclua a diária.");
    verify(repo, never()).delete(any(Despesa.class));
  }

  @Test
  void deveExcluirComRateiosSemTravarPlantioQuandoDespesaForManual() {
    Despesa despesa = existente(5L, null);
    service.excluir(5L);
    verify(repo).delete(despesa);
    verifyNoInteractions(plantios);
  }

  @Test
  void deveResponder404QuandoExcluirDespesaInexistente() {
    when(repo.findById(5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.excluir(5L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Despesa não encontrada.");
    verify(repo, never()).delete(any(Despesa.class));
  }

  @Test
  void deveTravarPlantiosEmOrdemCrescenteAntesDaRn04QuandoLancar() {
    service.criar(entrada("90.00", List.of(13L, 11L)));
    var ordem = inOrder(plantios, lancamentos);
    ordem.verify(plantios).buscarParaAtualizar(11L);
    ordem.verify(plantios).buscarParaAtualizar(13L);
    ordem.verify(lancamentos).validarDataFato(eq(FATO), anyCollection());
    verify(plantios, times(2)).buscarParaAtualizar(anyLong());
  }

  @Test
  void deveTravarPlantiosAtuaisENovosEmOrdemQuandoAlterar() {
    existente(5L, null);
    service.alterar(5L, entrada("90.00", List.of(12L, 13L)));
    var ordem = inOrder(plantios, lancamentos);
    ordem.verify(plantios).buscarParaAtualizar(11L);
    ordem.verify(plantios).buscarParaAtualizar(12L);
    ordem.verify(plantios).buscarParaAtualizar(13L);
    ordem.verify(lancamentos).validarDataFato(eq(FATO), anyCollection());
    verify(plantios, times(3)).buscarParaAtualizar(anyLong());
  }

  @Test
  void deveRecusarListasQuandoPeriodoForInvertido() {
    LocalDate inicio = LocalDate.of(2026, 3, 31), fim = LocalDate.of(2026, 3, 1);
    String mensagem = "O fim do período não pode ser anterior ao início.";
    assertThatThrownBy(() -> service.listar(1L, inicio, fim, new PaginacaoConsulta()))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(mensagem);
    assertThatThrownBy(() -> service.listarPorPlantio(11L, inicio, fim, new PaginacaoConsulta()))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(mensagem);
    verifyNoInteractions(rateios);
  }

  @Test
  void deveResponder404QuandoListarDespesasDePlantioInexistente() {
    assertThatThrownBy(() -> service.listarPorPlantio(99L, null, null, new PaginacaoConsulta()))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Plantio não encontrado.");
    verifyNoInteractions(rateios);
  }

  // A03b

  static final String JA_ESTORNADA = "Esta despesa já foi estornada.";
  static final String ESTORNADA =
      "Esta despesa foi estornada e não pode mais ser alterada nem excluída.";
  static final String EXCLUSAO_EMITIDA =
      "Esta despesa já consta em relatório emitido e não pode ser excluída. Use o estorno.";
  static final String DATA_EMITIDA =
      "Esta despesa já consta em relatório emitido. A data do fato não pode ser alterada.";
  static final String GERADA =
      "Esta despesa foi gerada por uma diária paga. Altere ou exclua a diária.";

  void emitido() {
    when(emissoes.existeEmissaoNoPeriodo(1L, FATO)).thenReturn(true);
  }

  void rejeitaSemGravar(org.assertj.core.api.ThrowableAssert.ThrowingCallable chamada, String msg) {
    assertThatThrownBy(chamada).isInstanceOf(RegraNegocioException.class).hasMessage(msg);
    verify(repo, never()).saveAndFlush(any());
    verify(repo, never()).save(any());
    verify(repo, never()).delete(any(Despesa.class));
  }

  static DespesaRequisicao comData(LocalDate data) {
    return new DespesaRequisicao(
        1L, 2L, "Adubo de teste", new BigDecimal("90.00"), data, List.of(11L, 12L));
  }

  @Test
  void deveMarcarEstornadaPreservandoRateiosSemCriarDespesaQuandoEstornar() {
    Despesa despesa = existente(5L, null);
    emitido();
    clearInvocations(rateio);
    DespesaResposta resposta = service.estornar(5L);
    assertThat(resposta.estornada()).isTrue();
    assertThat(resposta.id()).isEqualTo(5L);
    assertThat(resposta.rateios())
        .extracting(RateioDespesaResposta::valorRateado)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("45.00"), new BigDecimal("45.00"));
    assertThat(despesa.isEstornada()).isTrue();
    verify(repo).saveAndFlush(same(despesa));
    verify(repo, never()).save(any());
    verify(repo, never()).delete(any(Despesa.class));
    verifyNoInteractions(rateio);
  }

  @Test
  void deveEstornarQuandoDespesaForGeradaPorDiaria() {
    existente(5L, comId(new Diaria(), 40L));
    assertThat(service.estornar(5L).estornada()).isTrue();
  }

  @Test
  void deveRecusarSemGravarQuandoEstornarDeNovo() {
    existente(5L, null).setEstornada(true);
    rejeitaSemGravar(() -> service.estornar(5L), JA_ESTORNADA);
  }

  @Test
  void deveResponder404SemTravarQuandoEstornarDespesaInexistente() {
    when(repo.findById(77L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.estornar(77L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Despesa não encontrada.");
    verify(produtores, never()).buscarParaAtualizar(anyLong());
  }

  @Test
  void deveRecusarAlterarEExcluirComMensagemDeEstornadaQuandoTambemEstiverEmitidaEGerada() {
    existente(5L, comId(new Diaria(), 40L)).setEstornada(true);
    emitido();
    rejeitaSemGravar(() -> service.excluir(5L), ESTORNADA);
    rejeitaSemGravar(() -> service.alterar(5L, comData(FATO.plusDays(1))), ESTORNADA);
  }

  @Test
  void deveRecusarComRn16AntesDoPeriodoEmitidoQuandoDespesaForGeradaPorDiaria() {
    existente(5L, comId(new Diaria(), 40L));
    emitido();
    rejeitaSemGravar(() -> service.excluir(5L), GERADA);
    rejeitaSemGravar(() -> service.alterar(5L, comData(FATO.plusDays(1))), GERADA);
  }

  @Test
  void deveRecusarExclusaoComAMensagemDoCt020QuandoEstiverEmPeriodoEmitido() {
    existente(5L, null);
    emitido();
    rejeitaSemGravar(() -> service.excluir(5L), EXCLUSAO_EMITIDA);
    verify(emissoes).existeEmissaoNoPeriodo(1L, FATO);
  }

  @Test
  void deveExcluirQuandoDataDoFatoEstiverForaDePeriodoEmitido() {
    Despesa despesa = existente(5L, null);
    service.excluir(5L);
    verify(repo).delete(despesa);
  }

  @Test
  void deveRecusarTrocaDaDataSemGravarQuandoEstiverEmPeriodoEmitido() {
    Despesa despesa = existente(5L, null);
    emitido();
    rejeitaSemGravar(() -> service.alterar(5L, comData(FATO.plusDays(1))), DATA_EMITIDA);
    assertThat(despesa.getDataFato()).isEqualTo(FATO);
  }

  @Test
  void deveAceitarCorrecaoDoValorERecalcularRateioQuandoEstiverEmPeriodoEmitido() {
    existente(5L, null);
    emitido();
    DespesaResposta resposta = service.alterar(5L, entrada("120.00", List.of(11L, 12L)));
    assertThat(resposta.valorTotal()).isEqualByComparingTo("120.00");
    assertThat(resposta.rateios())
        .extracting(RateioDespesaResposta::valorRateado)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("60.00"), new BigDecimal("60.00"));
    verify(rateio).ratear(eq(new BigDecimal("120.00")), anyList());
    // A data não mudou: nem consulta o período emitido.
    verify(emissoes, never()).existeEmissaoNoPeriodo(anyLong(), any());
  }

  @Test
  void deveTravarProdutorAntesDaDespesaEDosPlantiosQuandoAlterarExcluirOuEstornar() {
    existente(5L, null);
    service.alterar(5L, entrada("90.00", List.of(11L, 12L)));
    var ordem = inOrder(produtores, repo, plantios);
    ordem.verify(produtores).buscarParaAtualizar(1L);
    ordem.verify(repo).findById(5L);
    ordem.verify(plantios).buscarParaAtualizar(11L);

    clearInvocations(produtores, repo, plantios);
    service.estornar(5L);
    var estorno = inOrder(produtores, repo);
    estorno.verify(produtores).buscarParaAtualizar(1L);
    estorno.verify(repo).findById(5L);

    existente(6L, null);
    service.excluir(6L);
    var exclusao = inOrder(produtores, repo, emissoes);
    exclusao.verify(produtores).buscarParaAtualizar(1L);
    exclusao.verify(repo).findById(6L);
    exclusao.verify(emissoes).existeEmissaoNoPeriodo(1L, FATO);
    exclusao.verify(repo).delete(any(Despesa.class));
  }
}
