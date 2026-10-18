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
        "{\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\",\"receita\":450,\"custoDesembolso\":100.25,\"resultado\":349.75,\"plantios\":[{\"plantioId\":1,\"tipoCiclo\":\"CURTO\",\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\",\"quantidadeProduzida\":250.5,\"quantidadeVendida\":100,\"unidadeMedida\":\"kg\",\"receita\":450,\"custoDesembolso\":100.25,\"valorMaoObraFamiliar\":80,\"custoComMaoObraFamiliar\":180.25,\"resultado\":349.75}]}")
public record PainelResposta(
    LocalDate periodoInicio,
    LocalDate periodoFim,
    BigDecimal receita,
    BigDecimal custoDesembolso,
    BigDecimal resultado,
    List<ResultadoPlantioResposta> plantios) {}
