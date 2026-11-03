package com.agrogestao.api.controller.usuario;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.service.usuario.UsuarioService;
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
@Tag(name = "Usuários")
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
      description = "Só ADMIN",
      content = @Content(schema = @Schema(implementation = ErroResposta.class))),
  @ApiResponse(
      responseCode = "404",
      description = "Recurso inexistente",
      content = @Content(schema = @Schema(implementation = ErroResposta.class)))
})
public class UsuarioController {
  private final UsuarioService service;

  @GetMapping("/api/v1/usuarios")
  @Operation(
      summary = "Lista usuários",
      description = "Paginado por id. Nunca traz senha nem hash.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public PaginaResposta<UsuarioResposta> listar(@Valid @ModelAttribute PaginacaoConsulta consulta) {
    return service.listar(consulta);
  }

  @PostMapping("/api/v1/usuarios")
  @Operation(
      summary = "Cadastra usuário",
      description =
          "Login gravado aparado e em minúsculas, único sem diferenciar caixa (422). Senha de 8 a"
              + " 72 caracteres e no máximo 72 bytes; gravada só como hash BCrypt.")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      description = "Criado com Location",
      headers =
          @Header(
              name = "Location",
              description = "URL do recurso criado",
              schema = @Schema(type = "string", format = "uri")))
  public ResponseEntity<UsuarioResposta> criar(@Valid @RequestBody UsuarioRequisicao dto) {
    var resposta = service.criar(dto);
    return ResponseEntity.created(URI.create("/api/v1/usuarios/" + resposta.id())).body(resposta);
  }

  @PutMapping("/api/v1/usuarios/{id}")
  @Operation(
      summary = "Altera nome, perfil e situação; nunca exclui",
      description =
          "O login não muda. Desativar ou rebaixar o último ADMIN ativo é recusado com 422."
              + " Perfil e situação valem já na próxima requisição do usuário.")
  @ApiResponse(responseCode = "200", description = "Resposta prevista no contrato")
  public UsuarioResposta alterar(
      @PathVariable Long id, @Valid @RequestBody UsuarioAlteracaoRequisicao dto) {
    return service.alterar(id, dto);
  }

  @PutMapping("/api/v1/usuarios/{id}/senha")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Redefine a senha do usuário",
      description =
          "Só ADMIN. Senha de 8 a 72 caracteres e no máximo 72 bytes. Vale também para usuário"
              + " inativo.")
  @ApiResponse(responseCode = "204", description = "Operação sem corpo")
  public void redefinirSenha(
      @PathVariable Long id, @Valid @RequestBody UsuarioSenhaRequisicao dto) {
    service.redefinirSenha(id, dto);
  }
}
