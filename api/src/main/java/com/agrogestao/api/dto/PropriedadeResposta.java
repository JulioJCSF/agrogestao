package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"produtorId\":1,\"nome\":\"Cadastro Fictício\",\"areaTotal\":5.25,\"localizacao\":null}")
public record PropriedadeResposta(
    Long id, Long produtorId, String nome, BigDecimal areaTotal, String localizacao) {}
