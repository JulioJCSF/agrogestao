package com.agrogestao.api.controller.cultura;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.cultura.CulturaService;
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

  private final CulturaService service;

  @GetMapping("/api/v1/culturas")
  @Operation(summary = "Lista com busca e paginação")
  public PaginaResposta<CulturaResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(busca, consulta);
  }

  @GetMapping("/api/v1/culturas/{id}")
  @Operation(summary = "Detalha o cadastro")
  public CulturaResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PostMapping("/api/v1/culturas")
  @Operation(summary = "Cria o cadastro")
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
    CulturaResposta resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/culturas/" + resposta.id())).body(resposta);
  }

  @PutMapping("/api/v1/culturas/{id}")
  @Operation(summary = "Altera o cadastro")
  public CulturaResposta alterar(@PathVariable Long id, @Valid @RequestBody CulturaRequisicao dto) {
    return service.alterar(id, dto);
  }
}
