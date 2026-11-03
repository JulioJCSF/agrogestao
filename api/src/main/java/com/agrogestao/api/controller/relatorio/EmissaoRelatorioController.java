package com.agrogestao.api.controller.relatorio;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.relatorio.EmissaoRelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
      responseCode = "403",
      description = "Perfil sem permissão (CONSULTA não emite)",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "404",
      description = "Recurso inexistente",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class EmissaoRelatorioController {
  private final EmissaoRelatorioService service;

  @PostMapping("/api/v1/relatorios")
  @Operation(
      summary = "Emite e grava conteúdo imutável",
      description =
          "Apura cada plantio ativo no período com o período informado e grava o conteúdo, com as"
              + " duas ressalvas (RN31 a RN34). O corpo só tem produtor e período; conteúdo e data"
              + " de emissão enviados são ignorados. Produtor sem consentimento: 422 (RN37)."
              + " Reemitir cria outra emissão; não há PUT nem DELETE.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<EmissaoRelatorioResposta> emitir(
      @Valid @RequestBody EmissaoRelatorioRequisicao dto) {
    var resposta = service.emitir(dto);
    return ResponseEntity.created(URI.create("/api/v1/relatorios/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/produtores/{produtorId}/relatorios")
  @Operation(
      summary = "Lista o histórico de emissões",
      description = "Da mais recente para a mais antiga (id decrescente), com o conteúdo gravado.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<EmissaoRelatorioResposta> listar(
      @PathVariable Long produtorId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(produtorId, consulta);
  }

  @GetMapping("/api/v1/relatorios/{id}")
  @Operation(
      summary = "Lê o conteúdo gravado da emissão",
      description = "Devolve o que foi gravado na emissão, sem reapurar (RN34).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public EmissaoRelatorioResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }
}
