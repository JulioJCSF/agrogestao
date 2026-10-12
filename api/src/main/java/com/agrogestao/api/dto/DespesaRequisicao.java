package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"produtorId\":1,\"categoriaDespesaId\":2,\"descricao\":\"Sementes fictícias\",\"valorTotal\":100,\"dataFato\":\"2026-02-01\",\"plantioIds\":[1,2]}")
public record DespesaRequisicao(
    @NotNull @Positive Long produtorId,
    @NotNull @Positive Long categoriaDespesaId,
    @NotBlank @Size(max = 255) String descricao,
    @NotNull @Digits(integer = 10, fraction = 2) BigDecimal valorTotal,
    @NotNull LocalDate dataFato,
    List<@NotNull @Positive Long> plantioIds) {}
