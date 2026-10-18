package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"nome\":\"Sítio Fictício\",\"areaTotal\":5.25,\"localizacao\":\"Área de demonstração\"}")
public record PropriedadeRequisicao(
    @NotBlank @Size(max = 150) String nome,
    @Digits(integer = 9, fraction = 3) BigDecimal areaTotal,
    @Size(max = 255) String localizacao) {}
