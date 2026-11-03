package com.agrogestao.api.controller.despesa;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.ApiNaoImplementadaException;
import com.agrogestao.api.service.despesa.DespesaService;
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
@Tag(name = "Despesas")
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
public class DespesaController {
  private final DespesaService service;

  @GetMapping("/api/v1/despesas")
  @Operation(
      summary = "Lista despesas",
      description =
          "Filtros opcionais por produtor e por período da data do fato, com as bordas incluídas."
              + " Rateios lidos como gravados (RN10). Estornadas e geradas por diária aparecem,"
              + " com estornada e diariaId.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<DespesaResposta> listar(
      @RequestParam(required = false) Long produtorId,
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(produtorId, periodoInicio, periodoFim, consulta);
  }

  @PostMapping("/api/v1/despesas")
  @Operation(
      summary = "Lança despesa",
      description =
          "Rateia o valor entre os plantios pela área e grava o rateio. Plantios do produtor"
              + " informado, sem repetição; categoria de sistema é recusada (RF28).")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<DespesaResposta> criar(@Valid @RequestBody DespesaRequisicao dto) {
    var resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/despesas/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/despesas/{id}")
  @Operation(
      summary = "Detalha despesa",
      description = "Rateios lidos como gravados, em ordem de plantio; nunca recalculados (RN10).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DespesaResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/despesas/{id}")
  @Operation(
      summary = "Altera despesa",
      description =
          "Recalcula o rateio com as áreas atuais só se mudarem o valor ou os plantios; senão"
              + " mantém o rateio gravado (RN05).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DespesaResposta alterar(@PathVariable Long id, @Valid @RequestBody DespesaRequisicao dto) {
    return service.alterar(id, dto);
  }

  @DeleteMapping("/api/v1/despesas/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Exclui despesa",
      description =
          "Remove a despesa manual e os rateios dela (RN11). Despesa gerada por diária é recusada"
              + " com 422 (RN16).")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    service.excluir(id);
  }

  @GetMapping("/api/v1/plantios/{plantioId}/despesas")
  @Operation(
      summary = "Lista os valores rateados ao plantio",
      description =
          "Uma linha por despesa rateada ao plantio, com percentual e valor rateado gravados, nunca"
              + " recalculados (RF23, RN10). Período opcional sobre a data do fato, com as bordas"
              + " incluídas. Estornadas aparecem com estornada.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<DespesaPlantioResposta> listarPorPlantio(
      @PathVariable Long plantioId,
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listarPorPlantio(plantioId, periodoInicio, periodoFim, consulta);
  }

  @ApiResponse(
      responseCode = "501",
      description = "Estorno pertence a A03b",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
  @PostMapping("/api/v1/despesas/{id}/estorno")
  @Operation(
      summary = "Estorna preservando os rateios e emissões",
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
  public ResponseEntity<DespesaResposta> estornar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }
}
