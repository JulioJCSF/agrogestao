package com.agrogestao.api.controller.auth;

import com.agrogestao.api.dto.LoginRequisicao;
import com.agrogestao.api.dto.LoginResposta;
import com.agrogestao.api.dto.UsuarioLogadoResposta;
import com.agrogestao.api.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @Operation(summary = "Autentica o operador e devolve o token JWT")
  @SecurityRequirements
  public LoginResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
    return authService.login(requisicao.login(), requisicao.senha());
  }

  @GetMapping("/me")
  @Operation(summary = "Devolve o usuário dono do token")
  public UsuarioLogadoResposta me(@AuthenticationPrincipal Jwt jwt) {
    return authService.usuarioAtual(Long.valueOf(jwt.getSubject()));
  }
}
