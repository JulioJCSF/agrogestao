package com.agrogestao.api.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.repository.SituacaoUsuario;
import com.agrogestao.api.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class ConversorDeTokenTest {
  UsuarioRepository usuarios = mock(UsuarioRepository.class);
  ConversorDeToken conversor = new ConversorDeToken(usuarios);

  static Jwt token(String sub, String perfilNaClaim) {
    return Jwt.withTokenValue("token-sintetico")
        .header("alg", "HS256")
        .subject(sub)
        .claim("perfil", perfilNaClaim)
        .build();
  }

  @Test
  void deveUsarPerfilDoBancoQuandoClaimDisserOutro() {
    when(usuarios.findSituacaoById(5L))
        .thenReturn(Optional.of(new SituacaoUsuario(Perfil.CONSULTA, true)));
    Jwt jwt = token("5", "ADMIN");
    var autenticacao = conversor.convert(jwt);
    assertThat(autenticacao.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_CONSULTA");
    assertThat(autenticacao.getPrincipal()).isSameAs(jwt);
    assertThat(autenticacao.getName()).isEqualTo("5");
  }

  @Test
  void deveRejeitarQuandoUsuarioEstiverInativo() {
    when(usuarios.findSituacaoById(5L))
        .thenReturn(Optional.of(new SituacaoUsuario(Perfil.LANCAMENTO, false)));
    assertThatThrownBy(() -> conversor.convert(token("5", "LANCAMENTO")))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void deveRejeitarQuandoUsuarioNaoExistir() {
    when(usuarios.findSituacaoById(5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> conversor.convert(token("5", "ADMIN")))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void deveRejeitarSemConsultarQuandoSubNaoForNumerico() {
    assertThatThrownBy(() -> conversor.convert(token("admin", "ADMIN")))
        .isInstanceOf(AuthenticationException.class);
    verifyNoInteractions(usuarios);
  }
}
