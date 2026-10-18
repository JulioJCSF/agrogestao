package com.agrogestao.api.controller.diaria;

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
@Tag(name = "Diárias")
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
public class DiariaController {

  @GetMapping("/api/v1/plantios/{plantioId}/diarias")
  @Operation(summary = "Diárias", description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<DiariaResposta> listar(
      @PathVariable Long plantioId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/plantios/{plantioId}/diarias")
  @Operation(summary = "Diárias", description = "Esqueleto F13: responde 501 até a implementação.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<DiariaResposta> criar(
      @PathVariable Long plantioId, @Valid @RequestBody DiariaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/diarias/{id}")
  @Operation(summary = "Diárias", description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DiariaResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/diarias/{id}")
  @Operation(summary = "Diárias", description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DiariaResposta alterar(@PathVariable Long id, @Valid @RequestBody DiariaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @DeleteMapping("/api/v1/diarias/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Diárias", description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }
}
