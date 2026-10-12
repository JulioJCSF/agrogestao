package com.agrogestao.api.controller.propriedade;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.propriedade.PropriedadeService;
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

  private final PropriedadeService service;

  @GetMapping("/api/v1/produtores/{produtorId}/propriedades")
  @Operation(summary = "Lista as propriedades do produtor")
  public PaginaResposta<PropriedadeResposta> listar(
      @PathVariable Long produtorId, @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(produtorId, consulta);
  }

  @PostMapping("/api/v1/produtores/{produtorId}/propriedades")
  @Operation(summary = "Cadastra propriedade do produtor")
  @ApiResponse(responseCode = "201", description = "Criada com Location")
  public ResponseEntity<PropriedadeResposta> criar(
      @PathVariable Long produtorId, @Valid @RequestBody PropriedadeRequisicao dto) {
    PropriedadeResposta resposta = service.criar(produtorId, dto);
    return ResponseEntity.created(URI.create("/api/v1/propriedades/" + resposta.id()))
        .body(resposta);
  }

  @GetMapping("/api/v1/propriedades/{id}")
  @Operation(summary = "Detalha propriedade")
  public PropriedadeResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/propriedades/{id}")
  @Operation(summary = "Altera propriedade mantendo o produtor")
  public PropriedadeResposta alterar(
      @PathVariable Long id, @Valid @RequestBody PropriedadeRequisicao dto) {
    return service.alterar(id, dto);
  }
}
