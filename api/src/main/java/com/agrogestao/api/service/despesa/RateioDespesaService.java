package com.agrogestao.api.service.despesa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Calcula o rateio de uma despesa entre os plantios vinculados a ela, proporcionalmente à área de
 * cada um (RN07). A base é a soma das áreas dos plantios recebidos, nunca a área da propriedade.
 *
 * <p>Plantio único recebe 100% pelo mesmo cálculo, sem caso especial (RN08). Cada parcela é
 * truncada em centavos e o residual vai para o plantio de maior área; no empate, para o de menor id
 * (RN09). A soma dos valores rateados é sempre exatamente o valor total.
 *
 * <p>{@link ParcelaRateio#valorRateado()} é a fonte de verdade; o percentual é só informativo.
 */
@Service
public class RateioDespesaService {

  private static final int ESCALA_VALOR = 2;
  private static final int ESCALA_PERCENTUAL = 6;

  /** Maior área vence; no empate, o menor id é considerado o maior. */
  private static final Comparator<PlantioRateavel> DESTINO_DO_RESIDUAL =
      Comparator.comparing(PlantioRateavel::area)
          .thenComparing(PlantioRateavel::plantioId, Comparator.reverseOrder());

  /**
   * Divide {@code valorTotal} entre {@code plantios} conforme RN07, RN08 e RN09.
   *
   * @param valorTotal valor da despesa, positivo e com no máximo 2 casas decimais
   * @param plantios plantios vinculados à despesa, com id único e área positiva
   * @return uma parcela por plantio, na mesma ordem da entrada
   * @throws IllegalArgumentException se a entrada for inválida (erro de programação do chamador)
   */
  public List<ParcelaRateio> ratear(BigDecimal valorTotal, List<PlantioRateavel> plantios) {
    BigDecimal total = validarValorTotal(valorTotal);
    validarPlantios(plantios);

    BigDecimal somaAreas =
        plantios.stream().map(PlantioRateavel::area).reduce(BigDecimal.ZERO, BigDecimal::add);

    List<BigDecimal> provisorios = new ArrayList<>(plantios.size());
    for (PlantioRateavel plantio : plantios) {
      provisorios.add(
          total.multiply(plantio.area()).divide(somaAreas, ESCALA_VALOR, RoundingMode.DOWN));
    }
    BigDecimal residual =
        total.subtract(provisorios.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    Long destinoResidual = plantios.stream().max(DESTINO_DO_RESIDUAL).orElseThrow().plantioId();

    List<ParcelaRateio> parcelas = new ArrayList<>(plantios.size());
    for (int i = 0; i < plantios.size(); i++) {
      PlantioRateavel plantio = plantios.get(i);
      BigDecimal valor = provisorios.get(i);
      if (plantio.plantioId().equals(destinoResidual)) {
        valor = valor.add(residual);
      }
      BigDecimal percentual =
          plantio.area().divide(somaAreas, ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
      parcelas.add(new ParcelaRateio(plantio.plantioId(), percentual, valor));
    }

    BigDecimal somaRateada =
        parcelas.stream().map(ParcelaRateio::valorRateado).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (somaRateada.compareTo(total) != 0) {
      throw new IllegalStateException(
          "Soma do rateio (" + somaRateada + ") difere do valor total (" + total + ").");
    }
    return parcelas;
  }

  private static BigDecimal validarValorTotal(BigDecimal valorTotal) {
    if (valorTotal == null) {
      throw new IllegalArgumentException("Valor total do rateio não pode ser nulo.");
    }
    if (valorTotal.signum() <= 0) {
      throw new IllegalArgumentException(
          "Valor total do rateio deve ser positivo: " + valorTotal + ".");
    }
    if (valorTotal.stripTrailingZeros().scale() > ESCALA_VALOR) {
      throw new IllegalArgumentException(
          "Valor total do rateio deve ter no máximo 2 casas decimais: " + valorTotal + ".");
    }
    return valorTotal.setScale(ESCALA_VALOR, RoundingMode.UNNECESSARY);
  }

  private static void validarPlantios(List<PlantioRateavel> plantios) {
    if (plantios == null || plantios.isEmpty()) {
      throw new IllegalArgumentException("Rateio exige pelo menos um plantio.");
    }
    Set<Long> ids = new HashSet<>();
    for (PlantioRateavel plantio : plantios) {
      if (plantio == null) {
        throw new IllegalArgumentException("Lista de plantios do rateio contém item nulo.");
      }
      if (plantio.plantioId() == null) {
        throw new IllegalArgumentException("Plantio do rateio sem id.");
      }
      if (plantio.area() == null || plantio.area().signum() <= 0) {
        throw new IllegalArgumentException(
            "Plantio " + plantio.plantioId() + " com área inválida: " + plantio.area() + ".");
      }
      if (!ids.add(plantio.plantioId())) {
        throw new IllegalArgumentException(
            "Plantio " + plantio.plantioId() + " repetido na lista do rateio.");
      }
    }
  }
}
