package com.agrogestao.api.controller.cliente;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.cliente.ClienteService;
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
      responseCode = "404",
      description = "Recurso inexistente",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class ClienteController {
  private final ClienteService service;

  @GetMapping("/api/v1/clientes")
  @Operation(
      summary = "Lista clientes",
      description =
          "Paginada e ordenada por id. A busca filtra o nome por trecho, sem diferenciar"
              + " maiúsculas.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<ClienteResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(busca, consulta);
  }

  @PostMapping("/api/v1/clientes")
  @Operation(
      summary = "Cadastra cliente",
      description = "Nome e tipo de canal (RF29). Nomes repetidos são aceitos.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<ClienteResposta> criar(@Valid @RequestBody ClienteRequisicao dto) {
    ClienteResposta resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/clientes/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/clientes/{id}")
  @Operation(summary = "Detalha cliente")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ClienteResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/clientes/{id}")
  @Operation(summary = "Altera cliente", description = "Troca nome e tipo de canal.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ClienteResposta alterar(@PathVariable Long id, @Valid @RequestBody ClienteRequisicao dto) {
    return service.alterar(id, dto);
  }
}
