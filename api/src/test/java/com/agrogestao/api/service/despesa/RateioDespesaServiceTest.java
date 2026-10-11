package com.agrogestao.api.service.despesa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RateioDespesaServiceTest {

  private final RateioDespesaService service = new RateioDespesaService();

  private static PlantioRateavel plantio(long id, String area) {
    return new PlantioRateavel(id, new BigDecimal(area));
  }

  private static List<BigDecimal> decimais(String... valores) {
    return Arrays.stream(valores).map(BigDecimal::new).toList();
  }

  private static BigDecimal soma(List<ParcelaRateio> parcelas) {
    return parcelas.stream()
        .map(ParcelaRateio::valorRateado)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  @DisplayName("CT012 — rateio proporcional à área de cada plantio")
  void deveRatearProporcionalmenteQuandoAreasForemDiferentes() {
    List<ParcelaRateio> parcelas =
        service.ratear(new BigDecimal("300.00"), List.of(plantio(1, "1.500"), plantio(2, "0.500")));

    assertThat(parcelas).extracting(ParcelaRateio::plantioId).containsExactly(1L, 2L);
    assertThat(parcelas)
        .extracting(ParcelaRateio::percentual)
        .containsExactlyElementsOf(decimais("0.750000", "0.250000"));
    assertThat(parcelas)
        .extracting(ParcelaRateio::valorRateado)
        .containsExactlyElementsOf(decimais("225.00", "75.00"));
    assertThat(soma(parcelas)).isEqualByComparingTo("300.00");
  }

  @Test
  @DisplayName("CT013 — plantio não vinculado fica fora da base do rateio")
  void deveUsarSoAreaDosPlantiosVinculadosQuandoPropriedadeTiverOutrosPlantios() {
    // P3 (6,000 ha) existe na propriedade, mas não está vinculado à despesa.
    List<ParcelaRateio> parcelas =
        service.ratear(new BigDecimal("400.00"), List.of(plantio(1, "1.000"), plantio(2, "3.000")));

    assertThat(parcelas).extracting(ParcelaRateio::plantioId).containsExactly(1L, 2L);
    assertThat(parcelas)
        .extracting(ParcelaRateio::percentual)
        .containsExactlyElementsOf(decimais("0.250000", "0.750000"));
    assertThat(parcelas)
        .extracting(ParcelaRateio::valorRateado)
        .containsExactlyElementsOf(decimais("100.00", "300.00"));
    assertThat(soma(parcelas)).isEqualByComparingTo("400.00");
  }

  @Test
  @DisplayName("CT014 — despesa em plantio único recebe 100%")
  void deveAtribuirValorTotalQuandoHouverPlantioUnico() {
    List<ParcelaRateio> parcelas =
        service.ratear(new BigDecimal("57.89"), List.of(plantio(1, "0.750")));

    assertThat(parcelas)
        .containsExactly(
            new ParcelaRateio(1L, new BigDecimal("1.000000"), new BigDecimal("57.89")));
  }

  @Test
  @DisplayName("CT015 — residual vai para o menor id quando as áreas empatam")
  void deveAtribuirResidualAoMenorIdQuandoAreasEmpatarem() {
    List<ParcelaRateio> parcelas =
        service.ratear(
            new BigDecimal("100.00"),
            List.of(plantio(1, "1.000"), plantio(2, "1.000"), plantio(3, "1.000")));

    assertThat(parcelas)
        .containsExactly(
            new ParcelaRateio(1L, new BigDecimal("0.333333"), new BigDecimal("33.34")),
            new ParcelaRateio(2L, new BigDecimal("0.333333"), new BigDecimal("33.33")),
            new ParcelaRateio(3L, new BigDecimal("0.333333"), new BigDecimal("33.33")));
    assertThat(soma(parcelas)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("CT015 — residual vai para o menor id mesmo com a lista na ordem P3, P2, P1")
  void deveAtribuirResidualAoMenorIdQuandoAreasEmpataremEListaVierEmOrdemInversa() {
    List<ParcelaRateio> parcelas =
        service.ratear(
            new BigDecimal("100.00"),
            List.of(plantio(3, "1.000"), plantio(2, "1.000"), plantio(1, "1.000")));

    assertThat(parcelas)
        .containsExactly(
            new ParcelaRateio(3L, new BigDecimal("0.333333"), new BigDecimal("33.33")),
            new ParcelaRateio(2L, new BigDecimal("0.333333"), new BigDecimal("33.33")),
            new ParcelaRateio(1L, new BigDecimal("0.333333"), new BigDecimal("33.34")));
    assertThat(soma(parcelas)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("CT016 — áreas desiguais sem dízima não geram residual")
  void deveNaoAcrescentarCentavoQuandoDivisaoForExata() {
    List<ParcelaRateio> parcelas =
        service.ratear(
            new BigDecimal("100.00"),
            List.of(plantio(1, "1.000"), plantio(2, "1.000"), plantio(3, "2.000")));

    assertThat(parcelas)
        .containsExactly(
            new ParcelaRateio(1L, new BigDecimal("0.250000"), new BigDecimal("25.00")),
            new ParcelaRateio(2L, new BigDecimal("0.250000"), new BigDecimal("25.00")),
            new ParcelaRateio(3L, new BigDecimal("0.500000"), new BigDecimal("50.00")));
    assertThat(soma(parcelas)).isEqualByComparingTo("100.00");
  }

  static Stream<Arguments> casosCt017() {
    return Stream.of(
        linha(
            1,
            "100.00",
            List.of("1", "2", "4"),
            List.of("14.28", "28.57", "57.15"),
            List.of("0.142857", "0.285714", "0.571429")),
        linha(
            2,
            "10.00",
            List.of("0.5", "0.5", "2"),
            List.of("1.66", "1.66", "6.68"),
            List.of("0.166667", "0.166667", "0.666667")),
        linha(3, "50.00", List.of("3", "3", "3"), List.of("16.68", "16.66", "16.66"), null),
        linha(4, "100.00", List.of("3", "3", "1"), List.of("42.87", "42.85", "14.28"), null),
        linha(
            5,
            "123.45",
            List.of("1", "2", "3"),
            List.of("20.57", "41.15", "61.73"),
            List.of("0.166667", "0.333333", "0.500000")),
        linha(
            6,
            "1500.00",
            List.of("0.3", "0.7", "1.2"),
            List.of("204.54", "477.27", "818.19"),
            List.of("0.136364", "0.318182", "0.545455")),
        linha(
            7,
            "250.00",
            List.of("1.25", "2.5", "0.75"),
            List.of("69.44", "138.90", "41.66"),
            List.of("0.277778", "0.555556", "0.166667")),
        linha(
            8,
            "75.50",
            List.of("2.5", "1"),
            List.of("53.93", "21.57"),
            List.of("0.714286", "0.285714")),
        linha(
            9,
            "20.00",
            List.of("1", "1", "1", "1", "1", "1"),
            List.of("3.35", "3.33", "3.33", "3.33", "3.33", "3.33"),
            List.of("0.166667", "0.166667", "0.166667", "0.166667", "0.166667", "0.166667")),
        linha(10, "99.99", List.of("1", "1", "1"), List.of("33.33", "33.33", "33.33"), null),
        linha(
            11,
            "1000000.00",
            List.of("1", "1", "1"),
            List.of("333333.34", "333333.33", "333333.33"),
            null));
  }

  private static Arguments linha(
      int numero,
      String valorTotal,
      List<String> areas,
      List<String> valoresEsperados,
      List<String> percentuaisEsperados) {
    return Arguments.of(numero, valorTotal, areas, valoresEsperados, percentuaisEsperados);
  }

  @ParameterizedTest(name = "CT017 #{0} — total {1}, áreas {2}")
  @MethodSource("casosCt017")
  @DisplayName("CT017 — soma dos rateios igual ao total")
  void deveSomarExatamenteOValorTotalQuandoRatearValoresEAreasVariados(
      int numero,
      String valorTotal,
      List<String> areas,
      List<String> valoresEsperados,
      List<String> percentuaisEsperados) {
    List<PlantioRateavel> plantios = new ArrayList<>();
    for (int i = 0; i < areas.size(); i++) {
      plantios.add(plantio(i + 1, areas.get(i)));
    }

    List<ParcelaRateio> parcelas = service.ratear(new BigDecimal(valorTotal), plantios);

    assertThat(parcelas)
        .extracting(ParcelaRateio::plantioId)
        .containsExactlyElementsOf(LongStream.rangeClosed(1, areas.size()).boxed().toList());
    assertThat(parcelas)
        .extracting(ParcelaRateio::valorRateado)
        .containsExactlyElementsOf(valoresEsperados.stream().map(BigDecimal::new).toList());
    if (percentuaisEsperados != null) {
      assertThat(parcelas)
          .extracting(ParcelaRateio::percentual)
          .containsExactlyElementsOf(percentuaisEsperados.stream().map(BigDecimal::new).toList());
    }
    assertThat(soma(parcelas)).isEqualByComparingTo(valorTotal);
  }

  @Test
  @DisplayName("Contrato — valor total com escala 0 gera parcelas com escala 2")
  void deveDevolverValoresComEscalaDoisQuandoValorTotalTiverEscalaZero() {
    List<ParcelaRateio> parcelas =
        service.ratear(new BigDecimal("100"), List.of(plantio(1, "1"), plantio(2, "1")));

    assertThat(parcelas)
        .extracting(ParcelaRateio::valorRateado)
        .containsExactlyElementsOf(decimais("50.00", "50.00"));
  }

  @Test
  @DisplayName("Contrato — a saída mantém a ordem da entrada")
  void deveManterOrdemDaEntradaQuandoIdsNaoEstiveremOrdenados() {
    List<ParcelaRateio> parcelas =
        service.ratear(
            new BigDecimal("100.00"),
            List.of(plantio(20, "1"), plantio(5, "3"), plantio(12, "1"), plantio(1, "2")));

    assertThat(parcelas).extracting(ParcelaRateio::plantioId).containsExactly(20L, 5L, 12L, 1L);
    assertThat(parcelas)
        .extracting(ParcelaRateio::valorRateado)
        .containsExactlyElementsOf(decimais("14.28", "42.87", "14.28", "28.57"));
  }

  @Test
  @DisplayName("Contrato — valor total nulo é rejeitado")
  void deveLancarIllegalArgumentQuandoValorTotalForNulo() {
    assertThatThrownBy(() -> service.ratear(null, List.of(plantio(1, "1"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nulo");
  }

  @ParameterizedTest(name = "Contrato — valor total {0} é rejeitado")
  @ValueSource(strings = {"0", "0.00", "-10.00", "10.005"})
  @DisplayName("Contrato — valor total não positivo ou com mais de 2 casas é rejeitado")
  void deveLancarIllegalArgumentQuandoValorTotalForInvalido(String valorTotal) {
    assertThatThrownBy(() -> service.ratear(new BigDecimal(valorTotal), List.of(plantio(1, "1"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Valor total");
  }

  @Test
  @DisplayName("Contrato — lista de plantios nula é rejeitada")
  void deveLancarIllegalArgumentQuandoListaDePlantiosForNula() {
    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pelo menos um plantio");
  }

  @Test
  @DisplayName("Contrato — lista de plantios vazia é rejeitada")
  void deveLancarIllegalArgumentQuandoListaDePlantiosForVazia() {
    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pelo menos um plantio");
  }

  @Test
  @DisplayName("Contrato — plantio nulo na lista é rejeitado")
  void deveLancarIllegalArgumentQuandoPlantioForNulo() {
    List<PlantioRateavel> plantios = new ArrayList<>();
    plantios.add(plantio(1, "1"));
    plantios.add(null);

    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), plantios))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nulo");
  }

  @Test
  @DisplayName("Contrato — plantio sem id é rejeitado")
  void deveLancarIllegalArgumentQuandoPlantioIdForNulo() {
    List<PlantioRateavel> plantios = List.of(new PlantioRateavel(null, new BigDecimal("1")));

    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), plantios))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sem id");
  }

  @Test
  @DisplayName("Contrato — plantio com área nula é rejeitado")
  void deveLancarIllegalArgumentQuandoAreaForNula() {
    List<PlantioRateavel> plantios = List.of(new PlantioRateavel(1L, null));

    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), plantios))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("área inválida");
  }

  @ParameterizedTest(name = "Contrato — plantio com área {0} é rejeitado")
  @ValueSource(strings = {"0", "0.000", "-1.000"})
  @DisplayName("Contrato — plantio com área zero ou negativa é rejeitado")
  void deveLancarIllegalArgumentQuandoAreaNaoForPositiva(String area) {
    List<PlantioRateavel> plantios = List.of(plantio(1, "1"), plantio(2, area));

    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), plantios))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("área inválida");
  }

  @Test
  @DisplayName("Contrato — plantio repetido na lista é rejeitado")
  void deveLancarIllegalArgumentQuandoPlantioIdForRepetido() {
    List<PlantioRateavel> plantios = List.of(plantio(1, "1"), plantio(2, "1"), plantio(1, "2"));

    assertThatThrownBy(() -> service.ratear(new BigDecimal("10.00"), plantios))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("repetido");
  }
}
