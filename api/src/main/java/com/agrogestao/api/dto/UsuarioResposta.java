package com.agrogestao.api.dto;

import com.agrogestao.api.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;

@Schema(
    description = "Resposta do contrato do MVP",
    example =
        "{\"id\":1,\"nome\":\"Cadastro Fictício\",\"login\":\"usuario-ficticio\",\"perfil\":\"LANCAMENTO\",\"ativo\":true}")
public record UsuarioResposta(Long id, String nome, String login, Perfil perfil, boolean ativo) {}
