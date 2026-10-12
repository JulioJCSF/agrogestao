package com.agrogestao.api.controller.cliente;

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
@Tag(name = "Clientes")
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
public class ClienteController {

  @GetMapping("/api/v1/clientes")
  @Operation(
      summary = "Lista clientes",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<ClienteResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/clientes")
  @Operation(
      summary = "Cadastra clientes",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "201", description = "Criado com Location")
  public ClienteResposta criar(@Valid @RequestBody ClienteRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/clientes/{id}")
  @Operation(
      summary = "Detalha clientes",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ClienteResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/clientes/{id}")
  @Operation(
      summary = "Altera clientes",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ClienteResposta alterar(@PathVariable Long id, @Valid @RequestBody ClienteRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }
}
