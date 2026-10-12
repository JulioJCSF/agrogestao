package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"plantioId\":1,\"quantidadeProduzida\":250.5,\"quantidadeVendida\":100,\"saldo\":150.5,\"unidadeMedida\":\"kg\"}")
public record SaldoPlantioResposta(
    Long plantioId,
    BigDecimal quantidadeProduzida,
    BigDecimal quantidadeVendida,
    BigDecimal saldo,
    String unidadeMedida) {}
