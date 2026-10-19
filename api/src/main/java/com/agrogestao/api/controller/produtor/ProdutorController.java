package com.agrogestao.api.controller.produtor;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.produtor.ProdutorService;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@Tag(name = "Produtores")
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
      responseCode = "404",
      description = "Produtor inexistente",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "422",
      description = "Regra de negócio",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class ProdutorController {

  private final ProdutorService produtorService;

  @GetMapping("/api/v1/produtores")
  @Operation(
      summary = "Lista produtores",
      description =
          "Busca por parte do nome ou do CPF (com ou sem máscara). Inclui ativos e inativos.")
  @ApiResponse(responseCode = "200", description = "Página de produtores")
  public PaginaResposta<ProdutorResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return produtorService.listar(busca, consulta.pageable());
  }

  @PostMapping("/api/v1/produtores")
  @Operation(
      summary = "Cadastra produtores",
      description = "CPF já cadastrado responde 422. Sem consentimentoEm, fica pendente.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<ProdutorResposta> criar(@Valid @RequestBody ProdutorRequisicao dto) {
    ProdutorResposta resposta = produtorService.criar(dto);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(resposta.id())
            .toUri();
    return ResponseEntity.created(location).body(resposta);
  }

  @GetMapping("/api/v1/produtores/{id}")
  @Operation(summary = "Detalha produtores")
  @ApiResponse(responseCode = "200", description = "Produtor encontrado")
  public ProdutorResposta detalhar(@PathVariable Long id) {
    return produtorService.detalhar(id);
  }

  @PutMapping("/api/v1/produtores/{id}")
  @Operation(
      summary = "Altera produtores",
      description = "Também inativa (ativo=false) e registra ou corrige a data do consentimento.")
  @ApiResponse(responseCode = "200", description = "Produtor alterado")
  public ProdutorResposta alterar(
      @PathVariable Long id, @Valid @RequestBody ProdutorRequisicao dto) {
    return produtorService.alterar(id, dto);
  }

  @DeleteMapping("/api/v1/produtores/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Exclui produtores",
      description =
          "Produtor com lançamentos, propriedades ou relatórios responde 422: inative pelo PUT.")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    produtorService.excluir(id);
  }
}
