package com.agrogestao.api.controller.plantio;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.model.SituacaoPlantio;
import com.agrogestao.api.service.plantio.PlantioService;
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
@Tag(name = "Plantios")
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
public class PlantioController {
  private final PlantioService service;

  @GetMapping("/api/v1/plantios")
  @Operation(
      summary = "Lista plantios",
      description = "Filtra data de início com limites inclusivos; filtros podem ser combinados.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<PlantioResposta> listar(
      @RequestParam(required = false) Long produtorId,
      @RequestParam(required = false) Long culturaId,
      @RequestParam(required = false) SituacaoPlantio situacao,
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(produtorId, culturaId, situacao, periodoInicio, periodoFim, consulta);
  }

  @PostMapping("/api/v1/plantios")
  @Operation(
      summary = "Cadastra plantio",
      description =
          "Tipo de ciclo omitido usa o padrão da cultura. Área positiva e início não futuro.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<PlantioResposta> criar(@Valid @RequestBody PlantioRequisicao dto) {
    var resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/plantios/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/plantios/{id}")
  @Operation(summary = "Detalha plantio")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PlantioResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/plantios/{id}")
  @Operation(
      summary = "Altera plantio",
      description = "Alteração de área preserva os rateios gravados e a data de encerramento.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PlantioResposta alterar(@PathVariable Long id, @Valid @RequestBody PlantioRequisicao dto) {
    return service.alterar(id, dto);
  }

  @PostMapping("/api/v1/plantios/{id}/encerramento")
  @Operation(
      summary = "Encerra plantio",
      description =
          "Encerramento não futuro, não anterior ao início nem a lançamento já gravado no plantio;"
              + " plantio já encerrado não é encerrado de novo.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<PlantioResposta> encerrar(
      @PathVariable Long id, @Valid @RequestBody EncerramentoPlantioRequisicao dto) {
    var resposta = service.encerrar(id, dto);
    return ResponseEntity.created(URI.create("/api/v1/plantios/" + resposta.id())).body(resposta);
  }
}
