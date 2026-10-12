package com.agrogestao.api.controller.despesa;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.ApiNaoImplementadaException;
import io.swagger.v3.oas.annotations.Operation;
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
      responseCode = "501",
      description = "Contrato publicado; implementação em sprint posterior",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class DespesaController {

  @GetMapping("/api/v1/despesas")
  @Operation(
      summary = "Lista despesas",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<DespesaResposta> listar(
      @RequestParam(required = false) Long produtorId,
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/despesas")
  @Operation(
      summary = "Cadastra despesas",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "201", description = "Criado com Location")
  public DespesaResposta criar(@Valid @RequestBody DespesaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/despesas/{id}")
  @Operation(
      summary = "Detalha despesas",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DespesaResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/despesas/{id}")
  @Operation(
      summary = "Altera despesas",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DespesaResposta alterar(@PathVariable Long id, @Valid @RequestBody DespesaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @DeleteMapping("/api/v1/despesas/{id}")
  @Operation(
      summary = "Exclui despesas",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/plantios/{plantioId}/despesas")
  @Operation(
      summary = "Lista os valores rateados ao plantio",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<DespesaPlantioResposta> listarPorPlantio(
      @PathVariable Long plantioId,
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/despesas/{id}/estorno")
  @Operation(
      summary = "Estorna preservando os rateios e emissões",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public DespesaResposta estornar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }
}
