package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"quantidade\":250.5,\"unidadeMedida\":\"kg\",\"dataFato\":\"2026-06-01\"}")
public record ProducaoRequisicao(
    @NotNull @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
    @NotBlank @Size(max = 10) String unidadeMedida,
    @NotNull LocalDate dataFato) {}
