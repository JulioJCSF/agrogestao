package com.agrogestao.api.controller.resultado;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.apuracao.ApuracaoResultadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Resultados")
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
      description = "Plantio não encontrado",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class ResultadoPlantioController {
  private final ApuracaoResultadoService service;

  @GetMapping("/api/v1/plantios/{id}/resultado")
  @Operation(
      summary = "Apura por ciclo; mês opcional para ciclo curto",
      description =
          "Ciclo longo: do início ao encerramento do plantio, ou até hoje se aberto; mes é"
              + " ignorado. Ciclo curto: o mês informado, ou o mês corrente (RN24).")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public ResultadoPlantioResposta apurar(
      @PathVariable Long id, @RequestParam(required = false) YearMonth mes) {
    return service.apurar(id, mes);
  }
}
