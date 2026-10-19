package com.agrogestao.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;
import org.hibernate.validator.constraints.br.CPF;

@Schema(
    description = "Entrada; valores calculados e auditoria pertencem à API",
    example =
        "{\"nome\":\"Produtor Fictício Exemplo\",\"documento\":\"99900000188\",\"ativo\":true}")
public record ProdutorRequisicao(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank
        @Pattern(regexp = "[0-9]{11}", message = "Informe os 11 dígitos do CPF, sem máscara.")
        @CPF(message = "CPF inválido. Confira os dígitos.")
        String documento,
    @Size(max = 255) String endereco,
    @Size(max = 100) String profissao,
    @Size(max = 100) String contato,
    @PastOrPresent(message = "A data do consentimento não pode ser futura.")
        LocalDate consentimentoEm,
    @NotNull Boolean ativo) {}
