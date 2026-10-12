package com.agrogestao.api.controller.categoria;

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

  @GetMapping("/api/v1/categorias-despesa")
  @Operation(
      summary = "Lista com busca e paginação",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public PaginaResposta<CategoriaDespesaResposta> listar(
      @RequestParam(defaultValue = "") String busca,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/categorias-despesa/{id}")
  @Operation(
      summary = "Detalha o cadastro",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public CategoriaDespesaResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/categorias-despesa")
  @Operation(
      summary = "Cria o cadastro",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  @ApiResponse(responseCode = "201", description = "Criado com Location")
  public ResponseEntity<CategoriaDespesaResposta> criar(
      @Valid @RequestBody CategoriaDespesaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/categorias-despesa/{id}")
  @Operation(
      summary = "Altera o cadastro",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public CategoriaDespesaResposta alterar(
      @PathVariable Long id, @Valid @RequestBody CategoriaDespesaRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @DeleteMapping("/api/v1/categorias-despesa/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Exclui categoria personalizada sem despesas",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public void excluir(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }
}
