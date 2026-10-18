package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"produtorId\":1,\"categoriaDespesaId\":2,\"diariaId\":null,\"descricao\":\"Despesa Fictícia\",\"dataFato\":\"2026-03-01\",\"estornada\":false,\"percentual\":1,\"valorRateado\":100.25}")
public record DespesaPlantioResposta(
    Long id,
    Long produtorId,
    Long categoriaDespesaId,
    Long diariaId,
    String descricao,
    LocalDate dataFato,
    boolean estornada,
    BigDecimal percentual,
    BigDecimal valorRateado) {}
