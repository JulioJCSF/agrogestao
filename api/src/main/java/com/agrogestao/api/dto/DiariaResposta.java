package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"plantioId\":1,\"valorReferenciaDiariaId\":1,\"natureza\":\"PAGA\",\"quantidadeDias\":1.25,\"valorUnitario\":80,\"valorTotal\":100.25,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\"}")
public record DiariaResposta(
    Long id,
    Long plantioId,
    Long valorReferenciaDiariaId,
    NaturezaDiaria natureza,
    BigDecimal quantidadeDias,
    BigDecimal valorUnitario,
    BigDecimal valorTotal,
    LocalDate dataFato,
    LocalDate dataColeta) {}
