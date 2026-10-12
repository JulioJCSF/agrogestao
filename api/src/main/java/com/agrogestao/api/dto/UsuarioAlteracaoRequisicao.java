package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"nome\":\"Operador Fictício\",\"perfil\":\"CONSULTA\",\"ativo\":false}")
public record UsuarioAlteracaoRequisicao(
    @NotBlank @Size(max = 150) String nome, @NotNull Perfil perfil, @NotNull Boolean ativo) {}
