package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"plantioId\":1,\"valorReferenciaDiariaId\":null,\"natureza\":\"PAGA\",\"quantidadeDias\":1.25,\"valorUnitario\":80,\"valorTotal\":100.00,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\"}")
public record DiariaResposta(
    Long id,
    Long plantioId,
    Long valorReferenciaDiariaId,
    NaturezaDiaria natureza,
    BigDecimal quantidadeDias,
    BigDecimal valorUnitario,
    BigDecimal valorTotal,
    LocalDate dataFato,
    LocalDate dataColeta) {
  /** O total é dias × valor unitário arredondado em 2 casas, como a despesa gerada (RN16). */
  public static DiariaResposta de(Diaria diaria) {
    return new DiariaResposta(
        diaria.getId(),
        diaria.getPlantio().getId(),
        diaria.getValorReferenciaDiaria() == null
            ? null
            : diaria.getValorReferenciaDiaria().getId(),
        diaria.getNatureza(),
        diaria.getQuantidadeDias(),
        diaria.getValorUnitario(),
        diaria
            .getQuantidadeDias()
            .multiply(diaria.getValorUnitario())
            .setScale(2, RoundingMode.HALF_UP),
        diaria.getDataFato(),
        diaria.getDataColeta());
  }
}
