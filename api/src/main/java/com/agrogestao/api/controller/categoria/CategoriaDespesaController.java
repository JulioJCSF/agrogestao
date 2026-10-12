package com.agrogestao.api.controller.categoria;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.categoria.CategoriaDespesaService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Categorias de despesa")
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
public class CategoriaDespesaController {

  private final CategoriaDespesaService service;

  @GetMapping("/api/v1/categorias-despesa")
  @Operation(summary = "Lista com busca e paginação")
  public PaginaResposta<CategoriaDespesaResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(busca, consulta);
  }

  @GetMapping("/api/v1/categorias-despesa/{id}")
  @Operation(summary = "Detalha o cadastro")
  public CategoriaDespesaResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PostMapping("/api/v1/categorias-despesa")
  @Operation(summary = "Cria o cadastro")
  @ApiResponse(responseCode = "201", description = "Criado com Location")
  public ResponseEntity<CategoriaDespesaResposta> criar(
      @Valid @RequestBody CategoriaDespesaRequisicao dto) {
    CategoriaDespesaResposta resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/categorias-despesa/" + resposta.id()))
        .body(resposta);
  }

  @PutMapping("/api/v1/categorias-despesa/{id}")
  @Operation(summary = "Altera o cadastro")
  public CategoriaDespesaResposta alterar(
      @PathVariable Long id, @Valid @RequestBody CategoriaDespesaRequisicao dto) {
    return service.alterar(id, dto);
  }

  @DeleteMapping("/api/v1/categorias-despesa/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Exclui categoria personalizada sem despesas")
  public void excluir(@PathVariable Long id) {
    service.excluir(id);
  }
}
