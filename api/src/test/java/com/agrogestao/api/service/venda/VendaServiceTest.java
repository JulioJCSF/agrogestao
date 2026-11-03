package com.agrogestao.api.service.venda;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.dto.ErroResposta.CampoErro;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.relatorio.ProtecaoEmissao;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * L09b com repositórios mockados (Q00e). P1 e P3 têm produção em "kg"; P2 não tem produção e vale a
 * cultura Milho, em "kg"; P9 é de outro produtor. O repositório de venda devolve a entidade
 * recebida, e a soma do vendido acumulado inclui os itens dela, como faria o banco depois do flush.
 */
class VendaServiceTest {
  static final Long PRODUTOR = 10L, OUTRO_PRODUTOR = 20L, CLIENTE = 30L;
  static final LocalDate DATA_FATO = LocalDate.of(2026, 6, 10);

  VendaRepository repository = mock(VendaRepository.class);
  ItemVendaRepository itensVenda = mock(ItemVendaRepository.class);
  ProdutorRepository produtores = mock(ProdutorRepository.class);
  ClienteRepository clientes = mock(ClienteRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  ProducaoRepository producoes = mock(ProducaoRepository.class);
  EmissaoRelatorioRepository emissoes = mock(EmissaoRelatorioRepository.class);
  Clock relogio =
      Clock.fixed(
          OffsetDateTime.parse("2026-10-11T12:00:00-03:00").toInstant(),
          ZoneId.of("America/Fortaleza"));
  LancamentoService lancamentos = spy(new LancamentoService(relogio));
  VendaService service =
      new VendaService(
          repository,
          itensVenda,
          produtores,
          clientes,
          plantios,
          producoes,
          lancamentos,
          new ProtecaoEmissao(emissoes, produtores));

  Cultura milho = new Cultura();
  Map<Long, Plantio> cadastrados = new HashMap<>();
  Map<Long, BigDecimal> produzido = new HashMap<>();
  Map<Long, BigDecimal> vendidoAntes = new HashMap<>();
  Venda gravada;

  @BeforeEach
  void preparar() {
    milho.setNome("Milho");
    milho.setUnidadeMedida("kg");
    Produtor produtor = produtor(PRODUTOR);
    plantio(1L, produtor);
    plantio(2L, produtor);
    plantio(3L, produtor);
    plantio(9L, produtor(OUTRO_PRODUTOR));
    producao(1L, "100.000");
    producao(3L, "100.000");
    when(produtores.findById(PRODUTOR)).thenReturn(Optional.of(produtor));
    // A03b: o produtor é travado antes de a venda ser lida.
    when(produtores.buscarParaAtualizar(PRODUTOR)).thenReturn(Optional.of(produtor));
    when(repository.buscarProdutorId(anyLong()))
        .thenAnswer(
            inv -> repository.findById(inv.getArgument(0)).map(v -> v.getProdutor().getId()));
    when(clientes.findById(CLIENTE)).thenReturn(Optional.of(entidade(new Cliente(), CLIENTE)));
    when(plantios.buscarParaAtualizar(anyLong()))
        .thenAnswer(inv -> Optional.ofNullable(cadastrados.get(inv.<Long>getArgument(0))));
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              gravada = inv.getArgument(0);
              return gravada;
            });
    when(producoes.somarQuantidadePorPlantio(anyCollection()))
        .thenAnswer(inv -> totais(inv.getArgument(0), produzido, null));
    when(itensVenda.somarQuantidadePorPlantio(anyCollection()))
        .thenAnswer(inv -> totais(inv.getArgument(0), vendidoAntes, gravada));
  }

  static <T> T entidade(T objeto, Long id) {
    ReflectionTestUtils.setField(objeto, "id", id);
    return objeto;
  }

  static Produtor produtor(Long id) {
    return entidade(new Produtor(), id);
  }

  void plantio(Long id, Produtor dono) {
    Propriedade propriedade = new Propriedade();
    propriedade.setProdutor(dono);
    Plantio plantio = entidade(new Plantio(), id);
    plantio.setPropriedade(propriedade);
    plantio.setCultura(milho);
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    cadastrados.put(id, plantio);
  }

  void producao(Long plantioId, String quantidade) {
    Producao producao = new Producao();
    producao.setQuantidade(new BigDecimal(quantidade));
    producao.setUnidadeMedida("kg");
    when(producoes.findFirstByPlantioIdOrderByIdAsc(plantioId)).thenReturn(Optional.of(producao));
    produzido.put(plantioId, new BigDecimal(quantidade));
  }

  static List<TotalPorPlantio> totais(
      Collection<Long> ids, Map<Long, BigDecimal> base, Venda incluida) {
    List<TotalPorPlantio> linhas = new ArrayList<>();
    for (Long id : ids) {
      BigDecimal total = base.get(id);
      if (incluida != null)
        for (ItemVenda item : incluida.getItens())
          if (item.getPlantio().getId().equals(id))
            total = (total == null ? BigDecimal.ZERO : total).add(item.getQuantidade());
      if (total != null) linhas.add(total(id, total));
    }
    return linhas;
  }

  static TotalPorPlantio total(Long plantioId, BigDecimal total) {
    return new TotalPorPlantio() {
      public Long getPlantioId() {
        return plantioId;
      }

      public BigDecimal getTotal() {
        return total;
      }
    };
  }

  static ItemVendaRequisicao item(Long plantio, String quantidade, String unidade, String valor) {
    return new ItemVendaRequisicao(
        plantio, new BigDecimal(quantidade), unidade, new BigDecimal(valor));
  }

  static VendaRequisicao venda(ItemVendaRequisicao... itens) {
    return new VendaRequisicao(PRODUTOR, CLIENTE, DATA_FATO, List.of(itens));
  }

  void assertRegra(VendaRequisicao dto, String mensagem, String campo) {
    assertThatThrownBy(() -> service.criar(dto))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(mensagem)
        .extracting(e -> ((RegraNegocioException) e).getCampos())
        .asInstanceOf(InstanceOfAssertFactories.list(CampoErro.class))
        .extracting(CampoErro::campo)
        .containsExactly(campo);
  }

  void assertNadaGravado() {
    verify(repository, never()).saveAndFlush(any());
    verify(repository, never()).save(any());
  }

  @Test
  void deveSomarItensJaArredondadosQuandoRegistrarVendaNaUnidadeDaReferenciaCt048() {
    // Passo 1: P1 com produção em "kg".
    VendaResposta passo1 =
        service.criar(venda(item(1L, "3.333", "kg", "4.50"), item(1L, "2.335", "kg", "3.00")));
    assertThat(gravada.getItens())
        .extracting(ItemVenda::getValorTotal)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("15.00"), new BigDecimal("7.01"));
    // Defeito: 22.00 (arredondou só a soma bruta 22,0035 ou HALF_EVEN no B: 7,00); 21.99 (DOWN).
    assertThat(gravada.getValorTotal()).isEqualByComparingTo("22.01");
    assertThat(passo1.valorTotal()).isEqualByComparingTo("22.01");
    assertThat(passo1.itens()).extracting(ItemVendaResposta::plantioId).containsExactly(1L, 1L);

    // Passo 2: " KG " é a mesma unidade.
    service.criar(venda(item(1L, "4.000", " KG ", "2.50")));
    assertThat(gravada.getItens().get(0).getValorTotal()).isEqualByComparingTo("10.00");
    assertThat(gravada.getValorTotal()).isEqualByComparingTo("10.00");

    // Passo 3: P2 sem produção vale a unidade da cultura.
    service.criar(venda(item(2L, "12.500", "kg", "3.10"), item(2L, "1.000", "Kg", "3.10")));
    assertThat(gravada.getItens())
        .extracting(ItemVenda::getValorTotal)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("38.75"), new BigDecimal("3.10"));
    assertThat(gravada.getValorTotal()).isEqualByComparingTo("41.85");
    verify(repository, times(3)).saveAndFlush(any());
  }

  @Test
  void deveRejeitarUnidadeDiferenteDaReferenciaSemGravarQuandoRegistrarVendaCt049() {
    String daProducao =
        "A unidade do item (%s) é diferente da unidade da produção registrada no plantio (kg).";
    // Passo 1.
    assertRegra(
        venda(item(1L, "2.000", "saca", "60.00")),
        daProducao.formatted("saca"),
        "itens[0].unidadeMedida");
    // Passo 2: semelhança de grafia não basta.
    assertRegra(
        venda(item(1L, "2.000", "kgs", "60.00")),
        daProducao.formatted("kgs"),
        "itens[0].unidadeMedida");
    assertRegra(
        venda(item(1L, "2.000", "k g", "60.00")),
        daProducao.formatted("k g"),
        "itens[0].unidadeMedida");
    // Passo 3: sem produção, a referência é a cultura.
    assertRegra(
        venda(item(2L, "2.000", "saca", "60.00")),
        "A unidade do item (saca) é diferente da unidade da cultura do plantio (kg).",
        "itens[0].unidadeMedida");
    // Passo 4: o item em "kg" não é gravado sozinho.
    assertRegra(
        venda(item(1L, "2.000", "kg", "60.00"), item(1L, "2.000", "saca", "60.00")),
        daProducao.formatted("saca"),
        "itens[1].unidadeMedida");
    assertNadaGravado();
  }

  @Test
  void deveGravarGrafiaDaReferenciaQuandoUnidadeVierComEspacoECaixa() {
    VendaResposta resposta =
        service.criar(venda(item(1L, "1.000", " KG ", "2.00"), item(2L, "1.000", "Kg", "2.00")));
    assertThat(gravada.getItens()).extracting(ItemVenda::getUnidadeMedida).containsOnly("kg");
    assertThat(resposta.itens()).extracting(ItemVendaResposta::unidadeMedida).containsOnly("kg");
  }

  @Test
  void deveRejeitarSemGravarQuandoPlantioForDeOutroProdutor() {
    assertRegra(
        venda(item(1L, "1.000", "kg", "2.00"), item(9L, "1.000", "kg", "2.00")),
        "Todos os itens devem ser de plantios do produtor informado.",
        "itens[1].plantioId");
    assertNadaGravado();
  }

  @Test
  void deveRejeitarComCampoDoItemQuandoQuantidadeOuValorNaoForPositivo() {
    assertRegra(
        venda(item(1L, "1.000", "kg", "2.00"), item(1L, "0", "kg", "2.00")),
        "A quantidade do item deve ser maior que zero.",
        "itens[1].quantidade");
    assertRegra(
        venda(item(1L, "1.000", "kg", "-1")),
        "O valor unitário do item deve ser maior que zero.",
        "itens[0].valorUnitario");
    assertNadaGravado();
  }

  @Test
  void deveCitarOutroProdutorQuandoItemTambemTiverUnidadeErrada() {
    assertRegra(
        venda(item(9L, "1.000", "saca", "2.00")),
        "Todos os itens devem ser de plantios do produtor informado.",
        "itens[0].plantioId");
    verify(lancamentos, never()).validarDataFato(any(), anyCollection());
    assertNadaGravado();
  }

  @Test
  void deveValidarQuantidadeAntesDeProcurarVinculosQuandoMaisDeUmaFalhar() {
    // Quantidade zero (validação 2) num plantio que não existe (validação 3).
    assertRegra(
        venda(item(404L, "0", "kg", "2.00")),
        "A quantidade do item deve ser maior que zero.",
        "itens[0].quantidade");
    verifyNoInteractions(produtores, plantios);
  }

  @Test
  void deveRecusarComNaoEncontradoQuandoVinculoNaoExistir() {
    assertThatThrownBy(
            () ->
                service.criar(
                    new VendaRequisicao(
                        99L, CLIENTE, DATA_FATO, List.of(item(1L, "1.000", "kg", "2.00")))))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Produtor não encontrado.");
    assertThatThrownBy(
            () ->
                service.criar(
                    new VendaRequisicao(
                        PRODUTOR, 99L, DATA_FATO, List.of(item(1L, "1.000", "kg", "2.00")))))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Cliente não encontrado.");
    assertThatThrownBy(() -> service.criar(venda(item(404L, "1.000", "kg", "2.00"))))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Plantio não encontrado.");
    assertNadaGravado();
  }

  @Test
  void deveDelegarRn04ComTodosOsPlantiosQuandoCriarEAlterar() {
    service.criar(venda(item(2L, "1.000", "kg", "2.00"), item(1L, "1.000", "kg", "2.00")));
    verify(lancamentos)
        .validarDataFato(
            eq(DATA_FATO), argThat((Collection<Plantio> c) -> ids(c).equals(List.of(1L, 2L))));

    Venda existente = existente(50L);
    service.alterar(50L, venda(item(3L, "1.000", "kg", "2.00"), item(1L, "1.000", "kg", "2.00")));
    verify(lancamentos)
        .validarDataFato(
            eq(DATA_FATO), argThat((Collection<Plantio> c) -> ids(c).equals(List.of(1L, 3L))));
    assertThat(existente.getItens())
        .extracting(i -> i.getPlantio().getId())
        .containsExactly(3L, 1L);
  }

  @Test
  void deveRejeitarSemGravarQuandoAlgumPlantioEstiverEncerradoAntesDaData() {
    cadastrados.get(2L).setDataEncerramento(LocalDate.of(2026, 5, 31));
    assertThatThrownBy(
            () ->
                service.criar(
                    venda(item(1L, "1.000", "kg", "2.00"), item(2L, "1.000", "kg", "2.00"))))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(
            "O plantio foi encerrado em 31/05/2026. A data do fato não pode ser posterior ao"
                + " encerramento.");
    assertNadaGravado();
  }

  static List<Long> ids(Collection<Plantio> plantios) {
    return plantios.stream().map(Plantio::getId).sorted().toList();
  }

  @Test
  void deveTravarPlantiosEmOrdemCrescenteSemRepetirQuandoVendaTiverVariosItens() {
    service.criar(
        venda(
            item(3L, "1.000", "kg", "2.00"),
            item(1L, "1.000", "kg", "2.00"),
            item(3L, "2.000", "kg", "2.00")));
    InOrder ordem = inOrder(plantios, lancamentos);
    ordem.verify(plantios).buscarParaAtualizar(1L);
    ordem.verify(plantios).buscarParaAtualizar(3L);
    ordem.verify(lancamentos).validarDataFato(any(), anyCollection());
    verify(plantios, times(2)).buscarParaAtualizar(anyLong());
    // O mesmo plantio em dois itens é permitido.
    assertThat(gravada.getItens()).hasSize(3);
  }

  static final String ACIMA =
      "A quantidade vendida do plantio (150,000 kg) supera a produção registrada (100,000 kg)."
          + " Confira os registros.";

  @Test
  void deveAvisarSoQuandoVendidoAcumuladoPassarDoProduzido() {
    vendidoAntes.put(1L, new BigDecimal("60.000"));
    // 90 sozinho não passa de 100; o acumulado 150 passa. Defeito: avisos vazio ou 422.
    assertThat(service.criar(venda(item(1L, "90.000", "kg", "5.00"))).avisos())
        .containsExactly(ACIMA);
    // 60 + 30 = 90: sem aviso, lista vazia e nunca nula.
    assertThat(service.criar(venda(item(1L, "30.000", "kg", "5.00"))).avisos())
        .isNotNull()
        .isEmpty();
  }

  @Test
  void deveAvisarSoDoPlantioAcimaQuandoVendaTiverDoisPlantios() {
    vendidoAntes.put(3L, new BigDecimal("60.000"));
    VendaResposta resposta =
        service.criar(
            venda(
                item(1L, "50.000", "kg", "5.00"),
                item(3L, "40.000", "kg", "5.00"),
                item(3L, "50.000", "kg", "5.00")));
    // P1: 50 ≤ 100. P3: 60 + 40 + 50 = 150 > 100, com os dois itens somados num aviso só.
    assertThat(resposta.avisos()).containsExactly(ACIMA);
  }

  @Test
  void deveAvisarComZeroProduzidoQuandoPlantioNaoTiverProducao() {
    assertThat(service.criar(venda(item(2L, "1.500", "kg", "5.00"))).avisos())
        .containsExactly(
            "A quantidade vendida do plantio (1,500 kg) supera a produção registrada (0,000 kg)."
                + " Confira os registros.");
  }

  @Test
  void deveRecalcularAvisosQuandoDetalharVenda() {
    Venda existente = existente(50L);
    existente.getItens().get(0).setQuantidade(new BigDecimal("90.000"));
    vendidoAntes.put(1L, new BigDecimal("60.000"));
    gravada = existente;
    assertThat(service.detalhar(50L).avisos()).containsExactly(ACIMA);
  }

  Venda existente(Long id) {
    Venda venda = entidade(new Venda(), id);
    venda.setProdutor(produtores.findById(PRODUTOR).orElseThrow());
    venda.setCliente(clientes.findById(CLIENTE).orElseThrow());
    venda.setDataFato(DATA_FATO);
    venda.setDataColeta(LocalDate.of(2026, 6, 11));
    ItemVenda antigo = entidade(new ItemVenda(), 500L);
    antigo.setVenda(venda);
    antigo.setPlantio(cadastrados.get(1L));
    antigo.setQuantidade(new BigDecimal("10.000"));
    antigo.setUnidadeMedida("kg");
    antigo.setValorUnitario(new BigDecimal("4.50"));
    antigo.setValorTotal(new BigDecimal("45.00"));
    venda.getItens().add(antigo);
    venda.setValorTotal(new BigDecimal("45.00"));
    when(repository.findById(id)).thenReturn(Optional.of(venda));
    return venda;
  }

  @Test
  void deveSubstituirItensEDarFlushAntesDeInserirQuandoAlterar() {
    Venda existente = existente(50L);
    ArgumentCaptor<Venda> salva = ArgumentCaptor.forClass(Venda.class);
    VendaResposta resposta =
        service.alterar(
            50L, venda(item(2L, "2.000", "kg", "7.25"), item(2L, "1.000", "kg", "0.01")));
    InOrder ordem = inOrder(repository);
    ordem.verify(repository).flush();
    ordem.verify(repository).saveAndFlush(salva.capture());
    assertThat(salva.getValue()).isSameAs(existente);
    assertThat(existente.getItens()).extracting(ItemVenda::getId).doesNotContain(500L).hasSize(2);
    assertThat(existente.getValorTotal()).isEqualByComparingTo("14.51");
    assertThat(resposta.dataColeta()).isEqualTo(LocalDate.of(2026, 6, 11));
  }

  @Test
  void deveManterVendaQuandoAlteracaoForRejeitada() {
    Venda existente = existente(50L);
    assertThatThrownBy(() -> service.alterar(50L, venda(item(1L, "1.000", "saca", "2.00"))))
        .isInstanceOf(RegraNegocioException.class);
    assertThat(existente.getItens()).extracting(ItemVenda::getId).containsExactly(500L);
    verify(repository, never()).flush();
    assertNadaGravado();
  }

  @Test
  void deveRecusarComNaoEncontradoQuandoVendaNaoExistir() {
    assertThatThrownBy(() -> service.detalhar(77L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Venda não encontrada.");
    assertThatThrownBy(() -> service.alterar(77L, venda(item(1L, "1.000", "kg", "2.00"))))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.excluir(77L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    verify(repository, never()).delete(any(Venda.class));
  }

  @Test
  void deveRejeitarListaQuandoFimForAnteriorAoInicio() {
    assertThatThrownBy(
            () ->
                service.listar(
                    null,
                    LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 9),
                    new PaginacaoConsulta()))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("O fim do período não pode ser anterior ao início.");
    verifyNoInteractions(repository);
  }

  // A03b

  static final String EXCLUSAO_EMITIDA =
      "Esta venda já consta em relatório emitido e não pode ser excluída. Corrija pela alteração.";
  static final String DATA_EMITIDA =
      "Esta venda já consta em relatório emitido. A data do fato não pode ser alterada.";

  void emitido() {
    when(emissoes.existeEmissaoNoPeriodo(PRODUTOR, DATA_FATO)).thenReturn(true);
  }

  @Test
  void deveRecusarExclusaoSemApagarQuandoVendaEstiverEmPeriodoEmitido() {
    existente(50L);
    emitido();
    assertThatThrownBy(() -> service.excluir(50L))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(EXCLUSAO_EMITIDA);
    verify(repository, never()).delete(any(Venda.class));
  }

  @Test
  void deveExcluirQuandoVendaEstiverForaDePeriodoEmitido() {
    Venda venda = existente(50L);
    service.excluir(50L);
    verify(repository).delete(venda);
  }

  @Test
  void deveRecusarTrocaDaDataSemGravarQuandoVendaEstiverEmPeriodoEmitido() {
    Venda venda = existente(50L);
    emitido();
    VendaRequisicao outraData =
        new VendaRequisicao(
            PRODUTOR, CLIENTE, DATA_FATO.plusDays(1), List.of(item(1L, "1.000", "kg", "2.00")));
    assertThatThrownBy(() -> service.alterar(50L, outraData))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(DATA_EMITIDA);
    verify(repository, never()).saveAndFlush(any());
    assertThat(venda.getDataFato()).isEqualTo(DATA_FATO);
    assertThat(venda.getItens()).hasSize(1);
  }

  @Test
  void deveAceitarCorrecaoDaQuantidadeQuandoVendaEstiverEmPeriodoEmitido() {
    existente(50L);
    emitido();
    VendaResposta resposta = service.alterar(50L, venda(item(1L, "20.000", "kg", "4.50")));
    assertThat(resposta.valorTotal()).isEqualByComparingTo("90.00");
    verify(emissoes, never()).existeEmissaoNoPeriodo(anyLong(), any());
  }

  @Test
  void deveTravarProdutorAntesDaVendaEDosPlantiosQuandoAlterarOuExcluir() {
    existente(50L);
    service.alterar(50L, venda(item(1L, "1.000", "kg", "2.00")));
    InOrder ordem = inOrder(produtores, repository, plantios);
    ordem.verify(produtores).buscarParaAtualizar(PRODUTOR);
    ordem.verify(repository).findById(50L);
    ordem.verify(plantios).buscarParaAtualizar(1L);

    clearInvocations(produtores, repository);
    service.excluir(50L);
    InOrder exclusao = inOrder(produtores, repository);
    exclusao.verify(produtores).buscarParaAtualizar(PRODUTOR);
    exclusao.verify(repository).delete(any(Venda.class));
  }
}
