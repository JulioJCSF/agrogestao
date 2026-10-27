package com.agrogestao.api.service.despesa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
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
  RateioDespesaService rateio = spy(new RateioDespesaService());
  LancamentoService lancamentos =
      spy(
          new LancamentoService(
              Clock.fixed(Instant.parse("2026-10-12T01:30:00Z"), ZoneId.of("America/Fortaleza"))));
  DespesaService service =
      new DespesaService(repo, produtores, categorias, plantios, rateio, lancamentos);

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
}
