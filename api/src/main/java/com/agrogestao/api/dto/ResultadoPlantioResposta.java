package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"plantioId\":1,\"tipoCiclo\":\"CURTO\",\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\",\"quantidadeProduzida\":250.5,\"quantidadeVendida\":100,\"unidadeMedida\":\"kg\",\"receita\":450,\"custoDesembolso\":100.25,\"valorMaoObraFamiliar\":80,\"custoComMaoObraFamiliar\":180.25,\"resultado\":349.75}")
public record ResultadoPlantioResposta(
    Long plantioId,
    TipoCiclo tipoCiclo,
    LocalDate periodoInicio,
    LocalDate periodoFim,
    BigDecimal quantidadeProduzida,
    BigDecimal quantidadeVendida,
    String unidadeMedida,
    BigDecimal receita,
    BigDecimal custoDesembolso,
    BigDecimal valorMaoObraFamiliar,
    BigDecimal custoComMaoObraFamiliar,
    BigDecimal resultado) {}
