package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example = "{\"nome\":\"Categoria Fictícia\"}")
public record CategoriaDespesaRequisicao(@NotBlank @Size(max = 80) String nome) {}
