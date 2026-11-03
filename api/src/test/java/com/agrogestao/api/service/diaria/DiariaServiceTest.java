package com.agrogestao.api.service.diaria;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class DiariaServiceTest {
  DiariaRepository repository = mock(DiariaRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  ValorReferenciaDiariaRepository referencias = mock(ValorReferenciaDiariaRepository.class);
  CategoriaDespesaRepository categorias = mock(CategoriaDespesaRepository.class);
  DespesaRepository despesas = mock(DespesaRepository.class);
  RateioDespesaRepository rateios = mock(RateioDespesaRepository.class);
  EmissaoRelatorioRepository emissoes = mock(EmissaoRelatorioRepository.class);
  LancamentoService lancamentos =
      new LancamentoService(
          Clock.fixed(Instant.parse("2026-10-12T01:30:00Z"), ZoneId.of("America/Fortaleza")));
  DiariaService service =
      new DiariaService(
          repository, plantios, referencias, categorias, despesas, rateios, emissoes, lancamentos);
  Plantio plantio = new Plantio();
  ValorReferenciaDiaria vr1 = referencia(1L, "80.25", "2026-01-01", "2026-06-30");
  ValorReferenciaDiaria vr2 = referencia(2L, "90.50", "2026-07-01", null);
  List<Diaria> gravadas = new ArrayList<>();
  List<Despesa> geradas = new ArrayList<>();
  List<RateioDespesa> rateados = new ArrayList<>();

  @BeforeEach
  void preparar() {
    ReflectionTestUtils.setField(plantio, "id", 7L);
    Produtor produtor = new Produtor();
    ReflectionTestUtils.setField(produtor, "id", 8L);
    Propriedade propriedade = new Propriedade();
    propriedade.setProdutor(produtor);
    plantio.setPropriedade(propriedade);
    when(plantios.buscarParaAtualizar(7L)).thenReturn(Optional.of(plantio));
    when(referencias.buscarVigente(any()))
        .thenAnswer(
            inv -> {
              LocalDate data = inv.getArgument(0);
              return List.of(vr1, vr2).stream()
                  .filter(
                      r ->
                          !data.isBefore(r.getVigenciaInicio())
                              && (r.getVigenciaFim() == null || !data.isAfter(r.getVigenciaFim())))
                  .findFirst();
            });
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              Diaria diaria = inv.getArgument(0);
              if (diaria.getId() == null)
                ReflectionTestUtils.setField(diaria, "id", (long) gravadas.size() + 1);
              lancamentos.atribuirDataColeta(diaria);
              gravadas.add(diaria);
              return diaria;
            });
    when(repository.findById(anyLong()))
        .thenAnswer(
            inv -> gravadas.stream().filter(d -> d.getId().equals(inv.getArgument(0))).findFirst());
    when(repository.buscarParaAtualizar(anyLong()))
        .thenAnswer(inv -> repository.findById(inv.getArgument(0)));
    when(despesas.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              Despesa d = inv.getArgument(0);
              lancamentos.atribuirDataColeta(d);
              geradas.add(d);
              return d;
            });
    when(rateios.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              RateioDespesa r = inv.getArgument(0);
              rateados.add(r);
              return r;
            });
    CategoriaDespesa categoria = new CategoriaDespesa();
    categoria.setNome("Diária");
    categoria.setSistema(true);
    when(categorias.findByNomeIgnoreCaseAndSistemaTrue("Diária"))
        .thenReturn(Optional.of(categoria));
  }

  static ValorReferenciaDiaria referencia(Long id, String valor, String inicio, String fim) {
    ValorReferenciaDiaria r = new ValorReferenciaDiaria();
    ReflectionTestUtils.setField(r, "id", id);
    r.setValor(new BigDecimal(valor));
    r.setVigenciaInicio(LocalDate.parse(inicio));
    r.setVigenciaFim(fim == null ? null : LocalDate.parse(fim));
    return r;
  }

  static DiariaRequisicao dto(NaturezaDiaria natureza, String dias, String valor, String data) {
    return new DiariaRequisicao(
        natureza,
        new BigDecimal(dias),
        valor == null ? null : new BigDecimal(valor),
        LocalDate.parse(data));
  }

  @Test
  void deveCopiarReferenciaDaDataDoFatoQuandoFamiliarCt023() {
    for (String data : List.of("2026-01-01", "2026-06-30")) {
      var diaria = service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1", null, data));
      assertThat(diaria.valorUnitario()).isEqualByComparingTo("80.25");
      assertThat(diaria.valorReferenciaDiariaId()).isEqualTo(1L);
      assertThat(diaria.valorTotal()).isEqualByComparingTo("80.25");
    }
    var nova = service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1.25", null, "2026-07-01"));
    assertThat(nova.valorReferenciaDiariaId()).isEqualTo(2L);
    assertThat(nova.valorTotal()).isEqualByComparingTo("113.13");
    var informada = service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1", "50", "2026-03-10"));
    assertThat(informada.valorUnitario()).isEqualByComparingTo("80.25");
    assertThat(geradas).isEmpty();
    assertThat(rateados).isEmpty();
  }

  @Test
  void deveRejeitarFamiliarSemGravarQuandoSemReferenciaCt024() {
    doReturn(Optional.empty()).when(referencias).buscarVigente(any());
    for (String data : List.of("2026-03-15", "2025-12-31"))
      assertThatThrownBy(() -> service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1", null, data)))
          .isInstanceOf(RegraNegocioException.class)
          .hasMessageContaining("Cadastre");
    assertThat(gravadas).isEmpty();
    assertThat(geradas).isEmpty();
    assertThat(service.criar(7L, dto(NaturezaDiaria.PAGA, "2", "90", "2026-03-15")).valorTotal())
        .isEqualByComparingTo("180.00");
  }

  @Test
  void deveUsarValorInformadoQuandoPagaCt025() {
    var paga = service.criar(7L, dto(NaturezaDiaria.PAGA, "2", "95", "2026-03-10"));
    assertThat(paga.valorReferenciaDiariaId()).isNull();
    assertThat(paga.valorUnitario()).isEqualByComparingTo("95");
    assertThat(paga.valorTotal()).isEqualByComparingTo("190");
    assertThat(geradas.getFirst().getValorTotal()).isEqualByComparingTo("190");
    assertThat(rateados.getFirst().getValorRateado()).isEqualByComparingTo("190");
    assertThat(
            service.criar(7L, dto(NaturezaDiaria.PAGA, "0.75", "90.50", "2026-03-11")).valorTotal())
        .isEqualByComparingTo("67.88");
    verifyNoInteractions(referencias);
  }

  @Test
  void deveRejeitarPagaQuandoValorAusenteOuNaoPositivoCt025() {
    for (String valor : Arrays.asList(null, "0", "-1"))
      assertThatThrownBy(
              () -> service.criar(7L, dto(NaturezaDiaria.PAGA, "1", valor, "2026-03-10")))
          .isInstanceOfSatisfying(
              RegraNegocioException.class,
              ex ->
                  assertThat(ex.getCampos())
                      .extracting(ErroResposta.CampoErro::campo)
                      .contains("valorUnitario"));
    assertThat(gravadas).isEmpty();
    assertThat(geradas).isEmpty();
  }

  @Test
  void devePreservarValorCopiadoQuandoReferenciaForAlteradaCt031() {
    vr1.setValor(new BigDecimal("80"));
    var primeira = service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1.50", null, "2026-03-10"));
    vr1.setValor(new BigDecimal("95"));
    assertThat(service.detalhar(primeira.id()).valorUnitario()).isEqualByComparingTo("80");
    assertThat(service.detalhar(primeira.id()).valorTotal()).isEqualByComparingTo("120");
    var segunda = service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1.50", null, "2026-03-20"));
    assertThat(segunda.valorTotal()).isEqualByComparingTo("142.50");
    assertThat(gravadas).hasSize(2);
  }

  @Test
  void deveManterValorAoCorrigirDiasEReaplicarQuandoDataDoFatoMudar() {
    var primeira = service.criar(7L, dto(NaturezaDiaria.FAMILIAR, "1", null, "2026-03-10"));
    vr1.setValor(new BigDecimal("95"));
    assertThat(
            service
                .alterar(primeira.id(), dto(NaturezaDiaria.FAMILIAR, "2", "1", "2026-03-10"))
                .valorUnitario())
        .isEqualByComparingTo("80.25");
    var nova =
        service.alterar(primeira.id(), dto(NaturezaDiaria.FAMILIAR, "2", null, "2026-07-01"));
    assertThat(nova.valorReferenciaDiariaId()).isEqualTo(2L);
    assertThat(nova.valorTotal()).isEqualByComparingTo("181.00");
  }

  @Test
  void deveRejeitarSemGravarQuandoDataForPosteriorAoEncerramentoOuHoje() {
    plantio.setDataEncerramento(LocalDate.of(2026, 6, 30));
    for (String data : List.of("2026-07-01", "2026-10-12"))
      assertThatThrownBy(() -> service.criar(7L, dto(NaturezaDiaria.PAGA, "1", "90", data)))
          .isInstanceOf(RegraNegocioException.class);
    assertThat(gravadas).isEmpty();
    assertThat(geradas).isEmpty();
    assertThat(service.criar(7L, dto(NaturezaDiaria.PAGA, "1", "90", "2026-06-30")).dataFato())
        .isEqualTo(LocalDate.of(2026, 6, 30));
  }
}
