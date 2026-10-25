package com.agrogestao.api.controller.referencia;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.referencia.ValorReferenciaDiariaService;
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
      responseCode = "404",
      description = "Referência não encontrada",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class ValorReferenciaDiariaController {
  private final ValorReferenciaDiariaService service;

  @GetMapping("/api/v1/valores-referencia-diaria")
  @Operation(
      summary = "Lista referências de diária",
      description =
          "Vigência com limites inclusivos e fim opcional; períodos não podem se sobrepor.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<ValorReferenciaDiariaResposta> listar(
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(consulta);
  }

  @PostMapping("/api/v1/valores-referencia-diaria")
  @Operation(
      summary = "Cadastra referências de diária",
      description =
          "Vigência com limites inclusivos e fim opcional; períodos não podem se sobrepor.")
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
    var resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/valores-referencia-diaria/" + resposta.id()))
        .body(resposta);
  }

  @GetMapping("/api/v1/valores-referencia-diaria/{id}")
  @Operation(
      summary = "Detalha referências de diária",
      description =
          "Vigência com limites inclusivos e fim opcional; períodos não podem se sobrepor.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ValorReferenciaDiariaResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/valores-referencia-diaria/{id}")
  @Operation(
      summary = "Altera referências de diária",
      description =
          "Vigência com limites inclusivos e fim opcional; períodos não podem se sobrepor.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ValorReferenciaDiariaResposta alterar(
      @PathVariable Long id, @Valid @RequestBody ValorReferenciaDiariaRequisicao dto) {
    return service.alterar(id, dto);
  }

  @GetMapping("/api/v1/valores-referencia-diaria/vigente")
  @Operation(
      summary = "Consulta a referência vigente na data do fato",
      description =
          "Vigência com limites inclusivos e fim opcional; períodos não podem se sobrepor.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ValorReferenciaDiariaResposta vigente(@RequestParam LocalDate dataFato) {
    return service.vigente(dataFato);
  }
}
