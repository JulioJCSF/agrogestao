package com.agrogestao.api.controller.producao;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.producao.ProducaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Produção")
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
      responseCode = "404",
      description = "Plantio ou produção não encontrado",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class ProducaoController {
  private final ProducaoService service;

  @GetMapping("/api/v1/plantios/{plantioId}/producoes")
  @Operation(summary = "Lista as produções do plantio")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<ProducaoResposta> listar(
      @PathVariable Long plantioId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(plantioId, consulta);
  }

  @PostMapping("/api/v1/plantios/{plantioId}/producoes")
  @Operation(summary = "Registra uma colheita no plantio")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<ProducaoResposta> criar(
      @PathVariable Long plantioId, @Valid @RequestBody ProducaoRequisicao dto) {
    var resposta = service.criar(plantioId, dto);
    return ResponseEntity.created(URI.create("/api/v1/producoes/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/producoes/{id}")
  @Operation(summary = "Detalha uma produção")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ProducaoResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/producoes/{id}")
  @Operation(summary = "Altera uma produção")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ProducaoResposta alterar(
      @PathVariable Long id, @Valid @RequestBody ProducaoRequisicao dto) {
    return service.alterar(id, dto);
  }

  @DeleteMapping("/api/v1/producoes/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Exclui uma produção")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    service.excluir(id);
  }
}
