package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example = "{\"plantioId\":1,\"percentual\":1,\"valorRateado\":100.25}")
public record RateioDespesaResposta(
    Long plantioId, BigDecimal percentual, BigDecimal valorRateado) {}
