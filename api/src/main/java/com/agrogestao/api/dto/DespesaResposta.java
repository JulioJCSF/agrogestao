package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"produtorId\":1,\"categoriaDespesaId\":2,\"diariaId\":null,\"descricao\":\"Despesa Fictícia\",\"valorTotal\":100.25,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\",\"estornada\":false,\"rateios\":[{\"plantioId\":1,\"percentual\":1,\"valorRateado\":100.25}]}")
public record DespesaResposta(
    Long id,
    Long produtorId,
    Long categoriaDespesaId,
    Long diariaId,
    String descricao,
    BigDecimal valorTotal,
    LocalDate dataFato,
    LocalDate dataColeta,
    boolean estornada,
    List<RateioDespesaResposta> rateios) {
  public static DespesaResposta de(Despesa d) {
    return de(d, d.getRateios());
  }

  /** Com os rateios gravados informados, em ordem de plantio (RN10: nunca recalculados). */
  public static DespesaResposta de(Despesa d, List<RateioDespesa> gravados) {
    return new DespesaResposta(
        d.getId(),
        d.getProdutor().getId(),
        d.getCategoriaDespesa().getId(),
        d.getDiaria() == null ? null : d.getDiaria().getId(),
        d.getDescricao(),
        d.getValorTotal(),
        d.getDataFato(),
        d.getDataColeta(),
        d.isEstornada(),
        gravados.stream()
            .sorted(Comparator.comparing(rateio -> rateio.getPlantio().getId()))
            .map(
                r ->
                    new RateioDespesaResposta(
                        r.getPlantio().getId(), r.getPercentual(), r.getValorRateado()))
            .toList());
  }
}
