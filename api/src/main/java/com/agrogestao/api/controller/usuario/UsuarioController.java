package com.agrogestao.api.controller.usuario;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.ApiNaoImplementadaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Usuários")
@ApiResponses({
  @ApiResponse(
      responseCode = "400",
      description = "Formato inválido",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "401",
      description = "Autenticação necessária",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "422",
      description = "Regra de negócio",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "501",
      description = "Contrato publicado; implementação em sprint posterior",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class UsuarioController {

  @GetMapping("/api/v1/usuarios")
  @Operation(summary = "Usuários", description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<UsuarioResposta> listar(@Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/usuarios")
  @Operation(summary = "Usuários", description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "201", description = "Criado com Location")
  public UsuarioResposta criar(@Valid @RequestBody UsuarioRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/usuarios/{id}")
  @Operation(
      summary = "Altera nome, perfil e situação; nunca exclui",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public UsuarioResposta alterar(
      @PathVariable Long id, @Valid @RequestBody UsuarioAlteracaoRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }
}
