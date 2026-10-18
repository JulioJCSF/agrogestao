package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"propriedadeId\":1,\"culturaId\":1,\"area\":1.25,\"dataInicio\":\"2026-02-01\"}")
public record PlantioRequisicao(
    @NotNull @Positive Long propriedadeId,
    @NotNull @Positive Long culturaId,
    @NotNull @Digits(integer = 9, fraction = 3) BigDecimal area,
    TipoCiclo tipoCiclo,
    @NotNull LocalDate dataInicio,
    LocalDate previsaoColheita) {}
