package com.agrogestao.api.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AutorDoRegistroTest {
  static final String SQL = "select id from usuario where login = ?";

  JdbcTemplate jdbc = mock(JdbcTemplate.class);
  AutorDoRegistro autor = new AutorDoRegistro(jdbc, "admin-sintetico");

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  void idDoLogin(String login, Long... ids) {
    when(jdbc.query(eq(SQL), any(RowMapper.class), eq(login))).thenReturn(List.of(ids));
  }

  static void autenticar(Authentication autenticacao) {
    SecurityContextHolder.getContext().setAuthentication(autenticacao);
  }

  @Test
  void deveUsarUsuarioDoTokenSemConsultarQuandoRequisicaoTiverToken() {
    Jwt jwt = Jwt.withTokenValue("token-sintetico").header("alg", "HS256").subject("42").build();
    autenticar(
        new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_LANCAMENTO")));
    assertThat(autor.getCurrentAuditor()).contains(42L);
    verifyNoInteractions(jdbc);
  }

  @Test
  void deveUsarAdminQuandoNaoHouverAutenticacao() {
    idDoLogin("admin-sintetico", 1L);
    assertThat(autor.getCurrentAuditor()).contains(1L);
  }

  @Test
  void deveUsarAdminQuandoAutenticacaoForAnonima() {
    idDoLogin("admin-sintetico", 1L);
    autenticar(
        new AnonymousAuthenticationToken(
            "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertThat(autor.getCurrentAuditor()).contains(1L);
  }

  @Test
  void deveResolverPeloLoginQuandoAutenticacaoForDeOutroTipo() {
    idDoLogin("lancamento-sintetico", 7L);
    autenticar(
        UsernamePasswordAuthenticationToken.authenticated(
            "lancamento-sintetico", null, AuthorityUtils.createAuthorityList("ROLE_LANCAMENTO")));
    assertThat(autor.getCurrentAuditor()).contains(7L);
  }

  @Test
  void deveFalharSemCairNoAdminQuandoLoginNaoExistir() {
    idDoLogin("nao-existe");
    idDoLogin("admin-sintetico", 1L);
    autenticar(
        UsernamePasswordAuthenticationToken.authenticated(
            "nao-existe", null, AuthorityUtils.createAuthorityList("ROLE_LANCAMENTO")));
    assertThatThrownBy(autor::getCurrentAuditor)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("'nao-existe'");
    verify(jdbc, never()).query(eq(SQL), any(RowMapper.class), eq("admin-sintetico"));
  }
}
