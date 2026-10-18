package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"nome\":\"Cultura Fictícia\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}")
public record CulturaRequisicao(
    @NotBlank @Size(max = 100) String nome,
    @NotNull TipoCiclo tipoCicloPadrao,
    @NotBlank @Size(max = 10) String unidadeMedida) {}
