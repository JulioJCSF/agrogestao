package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example = "{\"id\":1,\"valor\":80,\"vigenciaInicio\":\"2026-01-01\",\"vigenciaFim\":null}")
public record ValorReferenciaDiariaResposta(
    Long id, BigDecimal valor, LocalDate vigenciaInicio, LocalDate vigenciaFim) {}
