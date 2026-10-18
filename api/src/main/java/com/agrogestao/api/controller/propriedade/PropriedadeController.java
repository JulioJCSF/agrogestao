package com.agrogestao.api.controller.propriedade;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.ApiNaoImplementadaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
@Tag(name = "Propriedades")
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
public class PropriedadeController {

  @GetMapping("/api/v1/produtores/{produtorId}/propriedades")
  @Operation(
      summary = "Lista as propriedades do produtor",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public PaginaResposta<PropriedadeResposta> listar(
      @PathVariable Long produtorId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    throw new ApiNaoImplementadaException();
  }

  @PostMapping("/api/v1/produtores/{produtorId}/propriedades")
  @Operation(
      summary = "Cadastra propriedade do produtor",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<PropriedadeResposta> criar(
      @PathVariable Long produtorId, @Valid @RequestBody PropriedadeRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }

  @GetMapping("/api/v1/propriedades/{id}")
  @Operation(
      summary = "Detalha propriedade",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public PropriedadeResposta detalhar(@PathVariable Long id) {
    throw new ApiNaoImplementadaException();
  }

  @PutMapping("/api/v1/propriedades/{id}")
  @Operation(
      summary = "Altera propriedade mantendo o produtor",
      description = "Esqueleto F13: responde 501 até a implementação dos cadastros.")
  public PropriedadeResposta alterar(
      @PathVariable Long id, @Valid @RequestBody PropriedadeRequisicao dto) {
    throw new ApiNaoImplementadaException();
  }
}
