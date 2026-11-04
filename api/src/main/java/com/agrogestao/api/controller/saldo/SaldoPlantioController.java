package com.agrogestao.api.controller.saldo;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.saldo.SaldoPlantioService;
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
@Tag(name = "Saldo informativo")
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
public class SaldoPlantioController {
  private final SaldoPlantioService service;

  @GetMapping("/api/v1/plantios/{id}/saldo")
  @Operation(
      summary = "Saldo informativo",
      description =
          "Produção registrada menos quantidade vendida do plantio, acumuladas sem recorte de"
              + " período, na unidade da cultura. Pode ser negativo e nunca bloqueia venda (RF33,"
              + " RN29). Plantio encerrado responde normalmente.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public SaldoPlantioResposta consultar(@PathVariable Long id) {
    return service.saldo(id);
  }
}
