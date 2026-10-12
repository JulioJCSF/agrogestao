package com.agrogestao.api.controller.relatorio;

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
@Tag(name = "Relatórios")
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
public class EmissaoRelatorioController {

  @PostMapping("/api/v1/relatorios")
  @Operation(
      summary = "Emite e grava conteúdo imutável",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "201", description = "Criado com Location")
  public EmissaoRelatorioResposta emitir(@Valid @RequestBody EmissaoRelatorioRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/produtores/{produtorId}/relatorios")
  @Operation(
      summary = "Lista o histórico de emissões",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<EmissaoRelatorioResposta> listar(
      @PathVariable Long produtorId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/relatorios/{id}")
  @Operation(
      summary = "Lê o conteúdo gravado da emissão",
      description = "Esqueleto F13: responde 501 até a implementação.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public EmissaoRelatorioResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }
}
