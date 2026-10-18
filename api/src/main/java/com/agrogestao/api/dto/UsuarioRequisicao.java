package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"nome\":\"Operador Fictício\",\"login\":\"operador-ficticio\",\"senha\":\"apenas-exemplo-sintetico\",\"perfil\":\"LANCAMENTO\"}")
public record UsuarioRequisicao(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Size(max = 100) String login,
    @NotBlank @Size(min = 8, max = 72) String senha,
    @NotNull Perfil perfil) {}
