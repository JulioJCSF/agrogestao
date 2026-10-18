package com.agrogestao.api.controller.referencia;

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
@Tag(name = "Referências de diária")
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
public class ValorReferenciaDiariaController {

  @GetMapping("/api/v1/valores-referencia-diaria")
  @Operation(
      summary = "Lista referências de diária",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<ValorReferenciaDiariaResposta> listar(
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/valores-referencia-diaria")
  @Operation(
      summary = "Cadastra referências de diária",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<ValorReferenciaDiariaResposta> criar(
      @Valid @RequestBody ValorReferenciaDiariaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/valores-referencia-diaria/{id}")
  @Operation(
      summary = "Detalha referências de diária",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ValorReferenciaDiariaResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/valores-referencia-diaria/{id}")
  @Operation(
      summary = "Altera referências de diária",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ValorReferenciaDiariaResposta alterar(
      @PathVariable Long id, @Valid @RequestBody ValorReferenciaDiariaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/valores-referencia-diaria/vigente")
  @Operation(
      summary = "Consulta a referência vigente na data do fato",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ValorReferenciaDiariaResposta vigente(@RequestParam LocalDate dataFato) {
    throw new ApiNaoImplementadaException();
  }
}
