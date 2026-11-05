package com.agrogestao.api.service.apuracao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.ResultadoPlantioResposta;
import com.agrogestao.api.exception.RecursoNaoEncontradoException;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * CT032 a CT039 (Q00d, {@code docs/qa/casos-de-teste-apuracao.md}) com a massa base de lá. Os
 * repositórios devolvem a lista completa de registros de cada plantio, inclusive os de fora do
 * período e os estornados: o recorte é do serviço.
 */
class ApuracaoResultadoServiceTest {
  static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");
  static final Clock HOJE =
      Clock.fixed(OffsetDateTime.parse("2026-10-11T12:00:00-03:00").toInstant(), FORTALEZA);

  PlantioRepository plantios = mock(PlantioRepository.class);
  RateioDespesaRepository rateios = mock(RateioDespesaRepository.class);
  DiariaRepository diarias = mock(DiariaRepository.class);
  ProducaoRepository producoes = mock(ProducaoRepository.class);
  ItemVendaRepository itensVenda = mock(ItemVendaRepository.class);
  ApuracaoResultadoService service = servico(HOJE);

  Map<Long, List<RateioDespesa>> rateiosDoPlantio = new HashMap<>();
  Map<Long, List<Diaria>> diariasDoPlantio = new HashMap<>();
  Map<Long, List<Producao>> producoesDoPlantio = new HashMap<>();
  Map<Long, List<ItemVenda>> itensDoPlantio = new HashMap<>();

  // Massa base: Milho do catálogo (LONGO, kg). P1, P2 e P3 sobrepõem CURTO; PL e PA são longos.
  Cultura milho = new Cultura();
  Propriedade sitio = new Propriedade();
  Plantio p1, p2, p3, pl, pa;

  ApuracaoResultadoService servico(Clock relogio) {
    return new ApuracaoResultadoService(plantios, rateios, diarias, producoes, itensVenda, relogio);
  }

  @BeforeEach
  void preparar() {
    milho.setNome("Milho");
    milho.setTipoCicloPadrao(TipoCiclo.LONGO);
    milho.setUnidadeMedida("kg");
    sitio.setNome("Sítio Teste");
    p1 = plantio(1L, TipoCiclo.CURTO, null);
    p2 = plantio(2L, TipoCiclo.CURTO, null);
    p3 = plantio(3L, TipoCiclo.CURTO, null);
    pl = plantio(4L, TipoCiclo.LONGO, LocalDate.of(2026, 7, 31));
    pa = plantio(5L, TipoCiclo.LONGO, null);
    when(rateios.buscarDosPlantiosComDespesa(anyCollection()))
        .thenAnswer(inv -> dosPlantios(rateiosDoPlantio, inv.getArgument(0)));
    when(diarias.buscarDosPlantios(anyCollection()))
        .thenAnswer(inv -> dosPlantios(diariasDoPlantio, inv.getArgument(0)));
    when(producoes.buscarDosPlantios(anyCollection()))
        .thenAnswer(inv -> dosPlantios(producoesDoPlantio, inv.getArgument(0)));
    when(itensVenda.buscarDosPlantiosComVenda(anyCollection()))
        .thenAnswer(inv -> dosPlantios(itensDoPlantio, inv.getArgument(0)));
  }

  /** O que a consulta em lote devolve: os registros de todos os plantios pedidos, misturados. */
  static <T> List<T> dosPlantios(Map<Long, List<T>> porPlantio, Collection<Long> ids) {
    return ids.stream().flatMap(id -> porPlantio.getOrDefault(id, List.of()).stream()).toList();
  }

  Plantio plantio(Long id, TipoCiclo ciclo, LocalDate encerramento) {
    Plantio plantio = new Plantio();
    ReflectionTestUtils.setField(plantio, "id", id);
    plantio.setCultura(milho);
    plantio.setPropriedade(sitio);
    plantio.setTipoCiclo(ciclo);
    plantio.setArea(new BigDecimal(ciclo == TipoCiclo.CURTO ? "1.000" : "2.000"));
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    plantio.setDataEncerramento(encerramento);
    when(plantios.findById(id)).thenReturn(Optional.of(plantio));
    return plantio;
  }

  static BigDecimal valor(String texto) {
    return new BigDecimal(texto);
  }

  Despesa despesa(String data, String total, boolean estornada) {
    Despesa despesa = new Despesa();
    despesa.setDataFato(LocalDate.parse(data));
    despesa.setValorTotal(valor(total));
    despesa.setEstornada(estornada);
    return despesa;
  }

  /** Grava na despesa o rateio do plantio, com percentual e valor rateado como a RN09 deixou. */
  void ratear(Despesa despesa, Plantio plantio, String percentual, String valorRateado) {
    RateioDespesa rateio = new RateioDespesa();
    rateio.setDespesa(despesa);
    rateio.setPlantio(plantio);
    rateio.setPercentual(valor(percentual));
    rateio.setValorRateado(valor(valorRateado));
    despesa.getRateios().add(rateio);
    rateiosDoPlantio.computeIfAbsent(plantio.getId(), id -> new ArrayList<>()).add(rateio);
  }

  /** Despesa rateada 100% ao plantio. */
  Despesa despesaDe(Plantio plantio, String data, String valor) {
    Despesa despesa = despesa(data, valor, false);
    ratear(despesa, plantio, "1.000000", valor);
    return despesa;
  }

  Diaria diaria(Plantio plantio, NaturezaDiaria natureza, String dias, String valor, String data) {
    Diaria diaria = new Diaria();
    diaria.setPlantio(plantio);
    diaria.setNatureza(natureza);
    diaria.setQuantidadeDias(valor(dias));
    diaria.setValorUnitario(valor(valor));
    diaria.setDataFato(LocalDate.parse(data));
    diariasDoPlantio.computeIfAbsent(plantio.getId(), id -> new ArrayList<>()).add(diaria);
    return diaria;
  }

  void producao(Plantio plantio, String quantidade, String data) {
    Producao producao = new Producao();
    producao.setPlantio(plantio);
    producao.setQuantidade(valor(quantidade));
    producao.setUnidadeMedida("kg");
    producao.setDataFato(LocalDate.parse(data));
    producoesDoPlantio.computeIfAbsent(plantio.getId(), id -> new ArrayList<>()).add(producao);
  }

  Venda venda(String data, String total) {
    Venda venda = new Venda();
    venda.setDataFato(LocalDate.parse(data));
    venda.setValorTotal(valor(total));
    return venda;
  }

  void item(Venda venda, Plantio plantio, String quantidade, String unitario, String total) {
    ItemVenda item = new ItemVenda();
    item.setVenda(venda);
    item.setPlantio(plantio);
    item.setQuantidade(valor(quantidade));
    item.setUnidadeMedida("kg");
    item.setValorUnitario(valor(unitario));
    item.setValorTotal(valor(total));
    venda.getItens().add(item);
    itensDoPlantio.computeIfAbsent(plantio.getId(), id -> new ArrayList<>()).add(item);
  }

  ResultadoPlantioResposta apurar(Plantio plantio, String mes) {
    return service.apurar(plantio.getId(), mes == null ? null : YearMonth.parse(mes));
  }

  @Test
  void deveSomarValorRateadoDasDespesasDoPeriodoQuandoApurarMesCt032() {
    Despesa d1 = despesa("2026-03-10", "100.00", false);
    ratear(d1, p1, "0.333333", "33.34");
    ratear(d1, p2, "0.333333", "33.33");
    ratear(d1, p3, "0.333333", "33.33");
    despesaDe(p1, "2026-03-01", "57.89");
    despesaDe(p1, "2026-03-31", "42.11");
    despesaDe(p1, "2026-02-28", "1000.00");
    despesaDe(p1, "2026-04-01", "2000.00");
    ratear(despesa("2026-03-15", "500.00", true), p1, "1.000000", "500.00");

    var resultado = apurar(p1, "2026-03");
    assertThat(resultado.periodoInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(resultado.periodoFim()).isEqualTo(LocalDate.of(2026, 3, 31));
    // Defeito: 200,00 (valor total de D1), 133,33 (percentual × total), 633,34 (somou D6
    // estornada), 33,34 (bordas exclusivas), 3.133,34 (sem recorte de período).
    assertThat(resultado.custoDesembolso()).isEqualByComparingTo("133.34");

    assertThat(apurar(p2, "2026-03").custoDesembolso()).isEqualByComparingTo("33.33");
  }

  @Test
  void deveContarDiariaPagaUmaVezQuandoHouverDespesaGeradaCt033() {
    Diaria dp = diaria(p1, NaturezaDiaria.PAGA, "1.00", "80.00", "2026-03-10");
    Despesa dg = despesaDe(p1, "2026-03-10", "80.00");
    dg.setDiaria(dp);

    var resultado = apurar(p1, "2026-03");
    // Defeito: 160,00 — a diária paga somada por fora, além da despesa gerada.
    assertThat(resultado.custoDesembolso()).isEqualByComparingTo("80.00");
    assertThat(resultado.valorMaoObraFamiliar()).isEqualByComparingTo("0.00");
    assertThat(resultado.custoComMaoObraFamiliar()).isEqualByComparingTo("80.00");
    assertThat(resultado.receita()).isEqualByComparingTo("0.00");
    assertThat(resultado.resultado()).isEqualByComparingTo("-80.00");
  }

  @Test
  void deveDeixarDiariaFamiliarForaDoDesembolsoQuandoApurarCt034() {
    diaria(p1, NaturezaDiaria.FAMILIAR, "1.00", "80.00", "2026-03-10");

    var resultado = apurar(p1, "2026-03");
    assertThat(resultado.custoDesembolso()).isEqualByComparingTo("0.00");
    assertThat(resultado.valorMaoObraFamiliar()).isEqualByComparingTo("80.00");
    assertThat(resultado.custoComMaoObraFamiliar()).isEqualByComparingTo("80.00");
    // Defeito: −80,00 — a familiar entrou no custo usado pelo resultado.
    assertThat(resultado.resultado()).isEqualByComparingTo("0.00");
  }

  @Test
  void deveMostrarCustoComMaoObraFamiliarAoLadoDoDesembolsoQuandoHouverFamiliarCt035() {
    despesaDe(p1, "2026-03-05", "250.00");
    Diaria dp = diaria(p1, NaturezaDiaria.PAGA, "2.00", "90.00", "2026-03-12");
    despesaDe(p1, "2026-03-12", "180.00").setDiaria(dp);
    diaria(p1, NaturezaDiaria.FAMILIAR, "1.50", "80.00", "2026-03-12");
    diaria(p1, NaturezaDiaria.FAMILIAR, "0.50", "80.00", "2026-03-20");
    diaria(p1, NaturezaDiaria.FAMILIAR, "1.00", "80.00", "2026-04-02");
    item(venda("2026-03-25", "500.00"), p1, "100.000", "5.00", "500.00");

    var passo1 = apurar(p1, "2026-03");
    // Defeito: desembolso 590,00 (substituído pelo custo com mão de obra) ou 610,00 (DP somada
    // por fora); resultado −90,00 (usou o custo com mão de obra); familiar 240,00 (F4 de abril).
    assertThat(passo1.custoDesembolso()).isEqualByComparingTo("430.00");
    assertThat(passo1.valorMaoObraFamiliar()).isEqualByComparingTo("160.00");
    assertThat(passo1.custoComMaoObraFamiliar()).isEqualByComparingTo("590.00");
    assertThat(passo1.receita()).isEqualByComparingTo("500.00");
    assertThat(passo1.resultado()).isEqualByComparingTo("70.00");

    diaria(p1, NaturezaDiaria.FAMILIAR, "1.25", "80.00", "2026-03-26");
    var passo3 = apurar(p1, "2026-03");
    assertThat(passo3.custoDesembolso()).isEqualByComparingTo("430.00");
    assertThat(passo3.valorMaoObraFamiliar()).isEqualByComparingTo("260.00");
    assertThat(passo3.custoComMaoObraFamiliar()).isEqualByComparingTo("690.00");
    assertThat(passo3.receita()).isEqualByComparingTo("500.00");
    assertThat(passo3.resultado()).isEqualByComparingTo("70.00");
  }

  @Test
  void deveSomarSoItensDoPlantioQuandoVendaTiverItensDeOutroPlantioCt036() {
    Venda v1 = venda("2026-03-20", "600.00");
    item(v1, p1, "100.000", "4.50", "450.00");
    item(v1, p2, "50.000", "3.00", "150.00");
    item(venda("2026-04-01", "45.00"), p1, "10.000", "4.50", "45.00");
    item(venda("2026-03-01", "45.00"), p1, "20.000", "2.25", "45.00");

    var passo1 = apurar(p1, "2026-03");
    // Defeito: 645,00 (total de V1), 540,00 (incluiu V2, de abril), 450,00 (perdeu V3, na borda).
    assertThat(passo1.receita()).isEqualByComparingTo("495.00");
    assertThat(passo1.quantidadeVendida()).isEqualByComparingTo("120.000");
    assertThat(passo1.resultado()).isEqualByComparingTo("495.00");

    var passo2 = apurar(p2, "2026-03");
    assertThat(passo2.receita()).isEqualByComparingTo("150.00");
    assertThat(passo2.quantidadeVendida()).isEqualByComparingTo("50.000");
  }

  @Test
  void deveDevolverResultadoComSinalQuandoDesembolsoSuperarReceitaCt037() {
    item(venda("2026-03-20", "112.50"), p1, "25.000", "4.50", "112.50");
    despesaDe(p1, "2026-03-05", "300.00");
    despesaDe(p1, "2026-03-06", "45.67");
    item(venda("2026-03-20", "500.00"), p2, "100.000", "5.00", "500.00");
    despesaDe(p2, "2026-03-05", "120.00");
    item(venda("2026-03-20", "150.00"), p3, "40.000", "3.75", "150.00");
    despesaDe(p3, "2026-03-05", "150.00");

    var r1 = apurar(p1, "2026-03");
    assertThat(r1.receita()).isEqualByComparingTo("112.50");
    assertThat(r1.custoDesembolso()).isEqualByComparingTo("345.67");
    // Defeito: 233,17 (sinal invertido ou valor absoluto), null, 0,00 (limitado a zero).
    assertThat(r1.resultado()).isNotNull().isEqualByComparingTo("-233.17");

    var r2 = apurar(p2, "2026-03");
    assertThat(r2.receita()).isEqualByComparingTo("500.00");
    assertThat(r2.custoDesembolso()).isEqualByComparingTo("120.00");
    assertThat(r2.resultado()).isEqualByComparingTo("380.00");

    var r3 = apurar(p3, "2026-03");
    assertThat(r3.receita()).isEqualByComparingTo("150.00");
    assertThat(r3.custoDesembolso()).isEqualByComparingTo("150.00");
    assertThat(r3.resultado()).isEqualByComparingTo("0.00");
  }

  void assertPeriodo(
      ResultadoPlantioResposta r, TipoCiclo ciclo, String inicio, String fim, String desembolso) {
    assertThat(r.tipoCiclo()).isEqualTo(ciclo);
    assertThat(r.periodoInicio()).isEqualTo(LocalDate.parse(inicio));
    assertThat(r.periodoFim()).isEqualTo(LocalDate.parse(fim));
    assertThat(r.custoDesembolso()).isEqualByComparingTo(desembolso);
  }

  @Test
  void deveDerivarPeriodoDoCicloEfetivoQuandoApurarCurtoELongoCt038() {
    despesaDe(p1, "2026-03-01", "60.00");
    despesaDe(p1, "2026-03-31", "40.00");
    despesaDe(pl, "2026-02-01", "100.00");
    despesaDe(pl, "2026-05-10", "80.00");
    despesaDe(pl, "2026-07-31", "50.00");
    despesaDe(pa, "2026-10-11", "25.00");

    // Passo 1, defeito: início 2026-02-01 — ciclo lido da cultura (Milho é LONGO).
    assertPeriodo(apurar(p1, "2026-03"), TipoCiclo.CURTO, "2026-03-01", "2026-03-31", "100.00");
    assertPeriodo(apurar(p1, "2026-02"), TipoCiclo.CURTO, "2026-02-01", "2026-02-28", "0.00");
    assertPeriodo(apurar(p1, null), TipoCiclo.CURTO, "2026-10-01", "2026-10-31", "0.00");
    // Passo 4, defeito: período de novembro — "hoje" calculado em UTC.
    service = servico(Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), FORTALEZA));
    assertPeriodo(apurar(p1, null), TipoCiclo.CURTO, "2026-10-01", "2026-10-31", "0.00");
    service = servico(HOJE);
    // Passo 5, defeito: 0,00 — o mes recortou um plantio de ciclo longo.
    assertPeriodo(apurar(pl, "2026-03"), TipoCiclo.LONGO, "2026-02-01", "2026-07-31", "230.00");
    assertPeriodo(apurar(pl, null), TipoCiclo.LONGO, "2026-02-01", "2026-07-31", "230.00");
    assertPeriodo(apurar(pa, null), TipoCiclo.LONGO, "2026-02-01", "2026-10-11", "25.00");
  }

  @Test
  void deveDeixarDeForaLancamentosForaDoPeriodoQuandoApurarCt039() {
    despesaDe(p1, "2026-02-28", "30.00");
    despesaDe(p1, "2026-03-15", "10.00");
    despesaDe(p1, "2026-04-01", "70.00");
    despesaDe(p1, "2026-09-30", "15.00");
    despesaDe(p1, "2026-10-01", "35.00");
    item(venda("2026-04-01", "45.00"), p1, "10.000", "4.50", "45.00");
    diaria(p1, NaturezaDiaria.FAMILIAR, "1.00", "80.00", "2026-02-28");
    producao(p1, "12.000", "2026-04-01");
    despesaDe(pl, "2026-01-31", "200.00");
    despesaDe(pl, "2026-02-01", "100.00");

    var passo1 = apurar(p1, "2026-03");
    // Defeito: 160,00 (P1 apurado como safra); receita 45,00 ou familiar 80,00 (só as despesas
    // foram recortadas).
    assertThat(passo1.custoDesembolso()).isEqualByComparingTo("10.00");
    assertThat(passo1.receita()).isEqualByComparingTo("0.00");
    assertThat(passo1.valorMaoObraFamiliar()).isEqualByComparingTo("0.00");
    assertThat(passo1.quantidadeProduzida()).isEqualByComparingTo("0.000");
    assertThat(passo1.resultado()).isEqualByComparingTo("-10.00");

    var passo2 = apurar(p1, null);
    assertThat(passo2.periodoInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(passo2.custoDesembolso()).isEqualByComparingTo("35.00");

    // Defeito: 300,00 — o período longo não começa no início do plantio.
    assertThat(apurar(pl, null).custoDesembolso()).isEqualByComparingTo("100.00");
  }

  @Test
  void deveLerCicloDoPlantioQuandoCulturaTiverOutroCiclo() {
    // P1 é Milho (cultura LONGO) com plantio CURTO: lido da cultura, o período seria a safra
    // (2026-02-01 a hoje) e a despesa de fevereiro entraria.
    despesaDe(p1, "2026-02-10", "999.00");
    despesaDe(p1, "2026-03-10", "10.00");
    var resultado = apurar(p1, "2026-03");
    assertThat(milho.getTipoCicloPadrao()).isEqualTo(TipoCiclo.LONGO);
    assertPeriodo(resultado, TipoCiclo.CURTO, "2026-03-01", "2026-03-31", "10.00");
  }

  @Test
  void deveApurarMesDeFortalezaQuandoUtcJaVirouOMes() {
    // 2026-11-01T01:30Z é 22h30 de 31/10 em Fortaleza.
    service = servico(Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), FORTALEZA));
    despesaDe(p1, "2026-10-31", "12.00");
    despesaDe(p1, "2026-11-01", "99.00");
    assertPeriodo(apurar(p1, null), TipoCiclo.CURTO, "2026-10-01", "2026-10-31", "12.00");
  }

  @Test
  void deveArredondarCadaDiariaFamiliarQuandoProdutoTiverTresCasas() {
    // 1,25 × 90,50 = 113,125 → 113,13 por diária; 226,26 no total. Arredondar só a soma daria
    // 226,25.
    diaria(p1, NaturezaDiaria.FAMILIAR, "1.25", "90.50", "2026-03-10");
    diaria(p1, NaturezaDiaria.FAMILIAR, "1.25", "90.50", "2026-03-11");
    var resultado = apurar(p1, "2026-03");
    assertThat(resultado.valorMaoObraFamiliar()).isEqualByComparingTo("226.26");
    assertThat(resultado.custoComMaoObraFamiliar()).isEqualByComparingTo("226.26");
    assertThat(resultado.custoDesembolso()).isEqualByComparingTo("0.00");
  }

  @Test
  void deveDevolverZerosComEscalaQuandoPlantioNaoTiverLancamentos() {
    var resultado = apurar(p1, "2026-03");
    assertThat(resultado.plantioId()).isEqualTo(1L);
    assertThat(resultado.culturaNome()).isEqualTo("Milho");
    assertThat(resultado.propriedadeNome()).isEqualTo("Sítio Teste");
    assertThat(resultado.unidadeMedida()).isEqualTo("kg");
    for (BigDecimal dinheiro :
        List.of(
            resultado.receita(),
            resultado.custoDesembolso(),
            resultado.valorMaoObraFamiliar(),
            resultado.custoComMaoObraFamiliar(),
            resultado.resultado())) assertThat(dinheiro).isEqualTo(new BigDecimal("0.00"));
    assertThat(resultado.quantidadeProduzida()).isEqualTo(new BigDecimal("0.000"));
    assertThat(resultado.quantidadeVendida()).isEqualTo(new BigDecimal("0.000"));
  }

  @Test
  void deveDevolverEscalaDoContratoQuandoValoresVieremComOutraEscala() {
    despesaDe(p1, "2026-03-10", "80.0");
    producao(p1, "1.5", "2026-03-10");
    var resultado = apurar(p1, "2026-03");
    assertThat(resultado.custoDesembolso()).isEqualTo(new BigDecimal("80.00"));
    assertThat(resultado.resultado()).isEqualTo(new BigDecimal("-80.00"));
    assertThat(resultado.quantidadeProduzida()).isEqualTo(new BigDecimal("1.500"));
  }

  @Test
  void deveRecusarComNaoEncontradoQuandoPlantioNaoExistir() {
    when(plantios.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.apurar(99L, null))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Plantio não encontrado.");
  }

  @Test
  void deveUsarPeriodoInformadoQuandoEmissaoChamarComPeriodo() {
    // A03a: o período da emissão vale mesmo para plantio curto.
    despesaDe(p1, "2026-02-10", "5.00");
    despesaDe(p1, "2026-03-10", "7.00");
    var resultado = service.apurar(p1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));
    assertPeriodo(resultado, TipoCiclo.CURTO, "2026-02-01", "2026-03-31", "12.00");
  }

  /** Massa base da Q00d nos cinco plantios: rateio, estorno, diárias, produção e venda. */
  void massaCompleta() {
    Despesa d1 = despesa("2026-03-10", "100.00", false);
    ratear(d1, p1, "0.333333", "33.34");
    ratear(d1, p2, "0.333333", "33.33");
    ratear(d1, p3, "0.333333", "33.33");
    despesaDe(p1, "2026-03-01", "57.89");
    despesaDe(p1, "2026-02-28", "1000.00");
    ratear(despesa("2026-03-15", "500.00", true), p1, "1.000000", "500.00");
    Diaria paga = diaria(p2, NaturezaDiaria.PAGA, "2.00", "90.00", "2026-03-12");
    despesaDe(p2, "2026-03-12", "180.00").setDiaria(paga);
    diaria(p2, NaturezaDiaria.FAMILIAR, "1.50", "80.00", "2026-03-12");
    diaria(p3, NaturezaDiaria.FAMILIAR, "0.333", "80.00", "2026-04-02");
    producao(p1, "250.500", "2026-03-20");
    producao(p3, "30.000", "2026-04-10");
    Venda v1 = venda("2026-03-20", "600.00");
    item(v1, p1, "100.000", "4.50", "450.00");
    item(v1, p2, "50.000", "3.00", "150.00");
    item(venda("2026-04-01", "45.00"), p3, "10.000", "4.50", "45.00");
    despesaDe(pl, "2026-03-10", "230.00");
    despesaDe(pl, "2026-08-10", "70.00");
    diaria(pl, NaturezaDiaria.FAMILIAR, "1.00", "80.00", "2026-07-31");
    item(venda("2026-07-31", "900.00"), pl, "200.000", "4.50", "900.00");
    despesaDe(pa, "2026-10-11", "25.00");
    producao(pa, "12.000", "2026-10-11");
  }

  @Test
  void deveDarOMesmoResultadoQuandoApurarEmLoteOuUmPorUm() {
    massaCompleta();
    List<Plantio> todos = List.of(p1, p2, p3, pl, pa);
    for (LocalDate[] periodo :
        new LocalDate[][] {
          {LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)},
          {LocalDate.of(2026, 2, 1), LocalDate.of(2026, 10, 11)}
        }) {
      Map<Long, ResultadoPlantioResposta> lote =
          service.apurarVarios(todos, periodo[0], periodo[1]);
      assertThat(lote.keySet()).containsExactly(1L, 2L, 3L, 4L, 5L);
      for (Plantio plantio : todos)
        assertThat(lote.get(plantio.getId()))
            .usingRecursiveComparison()
            .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .isEqualTo(service.apurar(plantio, periodo[0], periodo[1]));
    }
    // Uma consulta por fonte para os cinco plantios, mais uma por fonte em cada apuração avulsa.
    verify(rateios, times(2 + 10)).buscarDosPlantiosComDespesa(anyCollection());
  }

  @Test
  void deveDevolverMapaVazioSemConsultarQuandoListaForVazia() {
    assertThat(service.apurarVarios(List.of(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
        .isEmpty();
    verifyNoInteractions(rateios, diarias, producoes, itensVenda);
  }

  @Test
  void deveConsultarEmBlocosDeMilQuandoHouverMaisDeMilPlantios() {
    List<Plantio> muitos = new ArrayList<>();
    for (long id = 1; id <= 1001; id++) {
      Plantio plantio = new Plantio();
      ReflectionTestUtils.setField(plantio, "id", id);
      plantio.setCultura(milho);
      plantio.setPropriedade(sitio);
      plantio.setTipoCiclo(TipoCiclo.CURTO);
      muitos.add(plantio);
    }
    var apurados =
        service.apurarVarios(muitos, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
    assertThat(apurados).hasSize(1001);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<Long>> blocos = ArgumentCaptor.forClass(Collection.class);
    verify(rateios, times(2)).buscarDosPlantiosComDespesa(blocos.capture());
    assertThat(blocos.getAllValues()).extracting(Collection::size).containsExactly(1000, 1);
    verify(diarias, times(2)).buscarDosPlantios(anyCollection());
    verify(producoes, times(2)).buscarDosPlantios(anyCollection());
    verify(itensVenda, times(2)).buscarDosPlantiosComVenda(anyCollection());
  }
}
