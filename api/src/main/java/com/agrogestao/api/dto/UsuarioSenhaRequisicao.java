package com.agrogestao.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(
    description = "Nova senha definida pelo administrador (M01); nunca volta em resposta",
    example = "{\"senha\":\"apenas-exemplo-sintetico\"}")
public record UsuarioSenhaRequisicao(@NotBlank @Size(min = 8, max = 72) String senha) {}
