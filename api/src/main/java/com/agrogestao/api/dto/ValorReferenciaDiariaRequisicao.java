package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"valor\":80,\"vigenciaInicio\":\"2026-01-01\"}")
public record ValorReferenciaDiariaRequisicao(
    @NotNull @Digits(integer = 10, fraction = 2) BigDecimal valor,
    @NotNull LocalDate vigenciaInicio,
    LocalDate vigenciaFim) {}
