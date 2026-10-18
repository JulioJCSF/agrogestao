package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"plantioId\":1,\"quantidade\":1.5,\"unidadeMedida\":\"kg\",\"valorUnitario\":80,\"valorTotal\":100.25}")
public record ItemVendaResposta(
    Long id,
    Long plantioId,
    BigDecimal quantidade,
    String unidadeMedida,
    BigDecimal valorUnitario,
    BigDecimal valorTotal) {}
