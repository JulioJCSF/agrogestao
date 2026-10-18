package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"produtorId\":1,\"clienteId\":null,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\",\"valorTotal\":100.25,\"itens\":[{\"id\":1,\"plantioId\":1,\"quantidade\":1.5,\"unidadeMedida\":\"kg\",\"valorUnitario\":80,\"valorTotal\":100.25}],\"avisos\":[]}")
public record VendaResposta(
    Long id,
    Long produtorId,
    Long clienteId,
    LocalDate dataFato,
    LocalDate dataColeta,
    BigDecimal valorTotal,
    List<ItemVendaResposta> itens,
    List<String> avisos) {}
