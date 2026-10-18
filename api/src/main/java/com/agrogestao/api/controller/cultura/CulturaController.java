package com.agrogestao.api.controller.cultura;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.ApiNaoImplementadaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
@Tag(name = "Culturas")
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
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class CulturaController {

  @GetMapping("/api/v1/culturas")
  @Operation(
      summary = "Lista com busca e paginação",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public PaginaResposta<CulturaResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/culturas/{id}")
  @Operation(
      summary = "Detalha o cadastro",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public CulturaResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/culturas")
  @Operation(
      summary = "Cria o cadastro",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<CulturaResposta> criar(@Valid @RequestBody CulturaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/culturas/{id}")
  @Operation(
      summary = "Altera o cadastro",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public CulturaResposta alterar(@PathVariable Long id, @Valid @RequestBody CulturaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }
}
