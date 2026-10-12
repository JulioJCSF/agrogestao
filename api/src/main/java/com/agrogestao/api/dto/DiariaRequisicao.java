package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"natureza\":\"PAGA\",\"quantidadeDias\":2,\"valorUnitario\":80,\"dataFato\":\"2026-03-01\"}")
public record DiariaRequisicao(
    @NotNull NaturezaDiaria natureza,
    @NotNull @Digits(integer = 4, fraction = 2) BigDecimal quantidadeDias,
    @Digits(integer = 10, fraction = 2) BigDecimal valorUnitario,
    @NotNull LocalDate dataFato) {}
