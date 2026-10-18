package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"nome\":\"Cadastro Fictício\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}")
public record CulturaResposta(
    Long id, String nome, TipoCiclo tipoCicloPadrao, String unidadeMedida) {}
