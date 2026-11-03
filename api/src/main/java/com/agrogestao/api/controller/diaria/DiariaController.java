package com.agrogestao.api.controller.diaria;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.diaria.DiariaService;
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
      responseCode = "404",
      description = "Plantio ou diária não encontrado",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class DiariaController {
  private final DiariaService service;

  @GetMapping("/api/v1/plantios/{plantioId}/diarias")
  @Operation(summary = "Lista as diárias do plantio")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<DiariaResposta> listar(
      @PathVariable Long plantioId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(plantioId, consulta);
  }

  @PostMapping("/api/v1/plantios/{plantioId}/diarias")
  @Operation(
      summary = "Registra diária paga ou familiar",
      description =
          "A paga gera despesa e rateio integral na mesma transação. A familiar usa a referência vigente na data do fato.")
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
    var resposta = service.criar(plantioId, dto);
    return ResponseEntity.created(URI.create("/api/v1/diarias/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/diarias/{id}")
  @Operation(summary = "Detalha uma diária")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DiariaResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/diarias/{id}")
  @Operation(
      summary = "Altera uma diária",
      description =
          "Mantém a natureza; acompanha a despesa paga. Na familiar, preserva o valor copiado, salvo quando a data do fato é corrigida.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DiariaResposta alterar(@PathVariable Long id, @Valid @RequestBody DiariaRequisicao dto) {
    return service.alterar(id, dto);
  }

  @DeleteMapping("/api/v1/diarias/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Exclui diária e sua despesa gerada")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    service.excluir(id);
  }
}
