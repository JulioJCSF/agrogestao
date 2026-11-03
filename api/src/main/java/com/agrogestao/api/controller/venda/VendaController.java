package com.agrogestao.api.controller.venda;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.venda.VendaService;
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
@Tag(name = "Vendas")
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
public class VendaController {
  private final VendaService service;

  @GetMapping("/api/v1/vendas")
  @Operation(
      summary = "Lista vendas",
      description =
          "Filtros opcionais por produtor e por período da data do fato, com as bordas incluídas."
              + " Cada venda traz os avisos de vendido acima do produzido (RN29).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<VendaResposta> listar(
      @RequestParam(required = false) Long produtorId,
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(produtorId, periodoInicio, periodoFim, consulta);
  }

  @PostMapping("/api/v1/vendas")
  @Operation(
      summary = "Cadastra venda",
      description =
          "Itens de plantios do produtor informado, na unidade da produção registrada ou, sem"
              + " produção, da cultura (RN27, RN30). Totais calculados pela API. Venda acima do"
              + " saldo é aceita e sinalizada em avisos (RN29).")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<VendaResposta> criar(@Valid @RequestBody VendaRequisicao dto) {
    var resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/vendas/" + resposta.id())).body(resposta);
  }

  @GetMapping("/api/v1/vendas/{id}")
  @Operation(
      summary = "Detalha venda",
      description = "Itens em ordem de id; avisos recalculados com os acumulados atuais (RN29).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public VendaResposta detalhar(@PathVariable Long id) {
    return service.detalhar(id);
  }

  @PutMapping("/api/v1/vendas/{id}")
  @Operation(
      summary = "Altera venda",
      description =
          "Substitui todos os itens e recalcula os totais; a data de coleta não muda (RN36). Em"
              + " período já emitido para o produtor, a data do fato não pode mudar (422).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public VendaResposta alterar(@PathVariable Long id, @Valid @RequestBody VendaRequisicao dto) {
    return service.alterar(id, dto);
  }

  @DeleteMapping("/api/v1/vendas/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Exclui venda",
      description =
          "Remove a venda e os itens dela. Em período já emitido para o produtor, 422: a venda não"
              + " tem estorno e se corrige pela alteração.")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void excluir(@PathVariable Long id) {
    service.excluir(id);
  }
}
