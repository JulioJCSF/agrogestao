package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"produtorId\":1,\"propriedadeId\":1,\"culturaId\":1,\"area\":1.25,\"tipoCiclo\":\"CURTO\",\"dataInicio\":\"2026-02-01\",\"previsaoColheita\":\"2026-06-30\",\"dataEncerramento\":null,\"situacao\":\"ABERTO\"}")
public record PlantioResposta(
    Long id,
    Long produtorId,
    Long propriedadeId,
    Long culturaId,
    BigDecimal area,
    TipoCiclo tipoCiclo,
    LocalDate dataInicio,
    LocalDate previsaoColheita,
    LocalDate dataEncerramento,
    SituacaoPlantio situacao) {}
