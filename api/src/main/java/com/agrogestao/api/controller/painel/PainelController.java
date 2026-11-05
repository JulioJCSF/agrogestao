package com.agrogestao.api.controller.painel;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.painel.PainelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Painel")
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
      description = "Produtor não encontrado",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "422",
      description = "Regra de negócio",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class PainelController {
  private final PainelService service;

  @GetMapping("/api/v1/painel")
  @Operation(
      summary = "Painel do produtor por período",
      description =
          "O painel é sempre de um produtor. Sem período, de 01/01 do ano corrente até hoje;"
              + " só início, até hoje; só fim, desde 01/01 do ano do fim. Entram os plantios ativos"
              + " em algum dia do período, cada um apurado no período informado; os totais são a"
              + " soma dos plantios.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PainelResposta consultar(
      @RequestParam(required = false) LocalDate periodoInicio,
      @RequestParam(required = false) LocalDate periodoFim,
      @RequestParam(required = false) @NotNull @Positive Long produtorId) {
    return service.consultar(periodoInicio, periodoFim, produtorId);
  }
}
