package com.agrogestao.api.service.saldo;

import com.agrogestao.api.dto.SaldoPlantioResposta;
import com.agrogestao.api.exception.RecursoNaoEncontradoException;
import com.agrogestao.api.model.Plantio;
import com.agrogestao.api.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saldo informativo do plantio (RF33, RN29): produzido − vendido acumulados, sem recorte de
 * período, com sinal. É a única conta de saldo: o {@code GET /plantios/{id}/saldo} e os avisos da
 * venda usam {@link #totais}. Não bloqueia nada e não trava linha; não há entidade de estoque
 * (M03).
 *
 * <p>A apuração ({@code ApuracaoResultadoService}) recorta por período; aqui é tudo o que já foi
 * registrado. São grandezas diferentes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaldoPlantioService {
  private static final int ESCALA = 3;

  private final ProducaoRepository producoes;
  private final ItemVendaRepository itensVenda;
  private final PlantioRepository plantios;

  /** Produzido e vendido acumulados por plantio; plantio sem registro não aparece no mapa. */
  public record Totais(Map<Long, BigDecimal> produzido, Map<Long, BigDecimal> vendido) {
    public BigDecimal produzido(Long plantioId) {
      return produzido.getOrDefault(plantioId, BigDecimal.ZERO);
    }

    public BigDecimal vendido(Long plantioId) {
      return vendido.getOrDefault(plantioId, BigDecimal.ZERO);
    }
  }

  /** Uma consulta por fonte para todos os plantios informados, sem N+1. */
  public Totais totais(Set<Long> plantioIds) {
    if (plantioIds.isEmpty()) return new Totais(Map.of(), Map.of());
    return new Totais(
        porPlantio(producoes.somarQuantidadePorPlantio(plantioIds)),
        porPlantio(itensVenda.somarQuantidadePorPlantio(plantioIds)));
  }

  /** Saldo do plantio na unidade da cultura (RN38); encerrado responde normalmente. */
  public SaldoPlantioResposta saldo(Long plantioId) {
    Plantio plantio =
        plantios
            .findById(plantioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
    Totais totais = totais(Set.of(plantioId));
    BigDecimal produzido = escala(totais.produzido(plantioId));
    BigDecimal vendido = escala(totais.vendido(plantioId));
    return new SaldoPlantioResposta(
        plantioId,
        produzido,
        vendido,
        produzido.subtract(vendido),
        plantio.getCultura().getUnidadeMedida().trim());
  }

  private static BigDecimal escala(BigDecimal valor) {
    return valor.setScale(ESCALA, RoundingMode.HALF_UP);
  }

  private static Map<Long, BigDecimal> porPlantio(List<TotalPorPlantio> linhas) {
    return linhas.stream()
        .collect(Collectors.toMap(TotalPorPlantio::getPlantioId, TotalPorPlantio::getTotal));
  }
}
