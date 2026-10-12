package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"produtorId\":1,\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\"}")
public record EmissaoRelatorioRequisicao(
    @NotNull @Positive Long produtorId,
    @NotNull LocalDate periodoInicio,
    @NotNull LocalDate periodoFim) {}
