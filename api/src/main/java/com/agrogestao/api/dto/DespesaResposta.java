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
        "{\"id\":1,\"produtorId\":1,\"categoriaDespesaId\":2,\"diariaId\":null,\"descricao\":\"Despesa Fictícia\",\"valorTotal\":100.25,\"dataFato\":\"2026-03-01\",\"dataColeta\":\"2026-03-02\",\"estornada\":null,\"rateios\":[{\"plantioId\":1,\"percentual\":1,\"valorRateado\":100.25}]}")
public record DespesaResposta(
    Long id,
    Long produtorId,
    Long categoriaDespesaId,
    Long diariaId,
    String descricao,
    BigDecimal valorTotal,
    LocalDate dataFato,
    LocalDate dataColeta,
    boolean estornada,
    List<RateioDespesaResposta> rateios) {}
