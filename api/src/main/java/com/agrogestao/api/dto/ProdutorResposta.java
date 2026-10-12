package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"nome\":\"Cadastro Fictício\",\"documento\":\"99900000188\",\"endereco\":\"Endereço Fictício\",\"profissao\":\"Agricultor de demonstração\",\"contato\":\"Contato Fictício\",\"consentimentoEm\":\"2026-01-01\",\"ativo\":true,\"consentimentoPendente\":false}")
public record ProdutorResposta(
    Long id,
    String nome,
    String documento,
    String endereco,
    String profissao,
    String contato,
    LocalDate consentimentoEm,
    boolean ativo,
    boolean consentimentoPendente) {}
