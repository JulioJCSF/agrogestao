package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"produtorId\":1,\"clienteId\":1,\"dataFato\":\"2026-06-01\",\"itens\":[{\"plantioId\":1,\"quantidade\":100,\"unidadeMedida\":\"kg\",\"valorUnitario\":4.5}]}")
public record VendaRequisicao(
    @NotNull @Positive Long produtorId,
    @NotNull @Positive Long clienteId,
    @NotNull LocalDate dataFato,
    @NotEmpty List<@NotNull @Valid ItemVendaRequisicao> itens) {}
