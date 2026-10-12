package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"plantioId\":1,\"quantidade\":100,\"unidadeMedida\":\"kg\",\"valorUnitario\":4.5}")
public record ItemVendaRequisicao(
    @NotNull @Positive Long plantioId,
    @NotNull @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
    @NotBlank @Size(max = 10) String unidadeMedida,
    @NotNull @Digits(integer = 10, fraction = 2) BigDecimal valorUnitario) {}
