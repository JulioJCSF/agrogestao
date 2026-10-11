package com.agrogestao.api.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agrogestao.api.config.JwtConfig;
import com.agrogestao.api.config.JwtProperties;
import com.agrogestao.api.dto.LoginResposta;
import com.agrogestao.api.dto.UsuarioLogadoResposta;
import com.agrogestao.api.exception.CredenciaisInvalidasException;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.Usuario;
import com.agrogestao.api.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

  private static final String SENHA = "senha-correta";
  private static final JwtProperties PROPRIEDADES =
      new JwtProperties("segredo-de-teste-com-mais-de-32-caracteres", 60);

  private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
  // Custo mínimo do BCrypt, só para o teste não ficar lento.
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
  private JwtDecoder jwtDecoder;
  private AuthService authService;

  @BeforeEach
  void configurar() {
    JwtConfig jwtConfig = new JwtConfig();
    SecretKey chave = jwtConfig.chaveJwt(PROPRIEDADES);
    jwtDecoder = jwtConfig.jwtDecoder(chave);
    authService =
        new AuthService(
            usuarioRepository, passwordEncoder, jwtConfig.jwtEncoder(chave), PROPRIEDADES);
  }

  private Usuario usuario(boolean ativo) {
    Usuario usuario =
        new Usuario(
            "Operadora de Teste",
            "operadora",
            passwordEncoder.encode(SENHA),
            Perfil.LANCAMENTO,
            ativo);
    ReflectionTestUtils.setField(usuario, "id", 7L);
    return usuario;
  }

  @Test
  void deveDevolverTokenEUsuarioQuandoCredenciaisValidas() {
    when(usuarioRepository.findByLogin("operadora")).thenReturn(Optional.of(usuario(true)));

    LoginResposta resposta = authService.login("operadora", SENHA);

    assertThat(resposta.usuario())
        .isEqualTo(new UsuarioLogadoResposta(7L, "Operadora de Teste", Perfil.LANCAMENTO));
    Jwt jwt = jwtDecoder.decode(resposta.token());
    assertThat(jwt.getSubject()).isEqualTo("7");
    assertThat(jwt.getClaimAsString("perfil")).isEqualTo("LANCAMENTO");
    assertThat(jwt.getExpiresAt()).isEqualTo(resposta.expiraEm());
    assertThat(Duration.between(Instant.now(), resposta.expiraEm()))
        .isBetween(Duration.ofMinutes(59), Duration.ofMinutes(60));
  }

  @Test
  void deveLancarCredenciaisInvalidasQuandoSenhaErrada() {
    when(usuarioRepository.findByLogin("operadora")).thenReturn(Optional.of(usuario(true)));

    assertThatThrownBy(() -> authService.login("operadora", "senha-errada"))
        .isInstanceOf(CredenciaisInvalidasException.class)
        .hasMessage("Login ou senha inválidos.");
  }

  @Test
  void deveLancarCredenciaisInvalidasQuandoLoginInexistente() {
    when(usuarioRepository.findByLogin("ninguem")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login("ninguem", SENHA))
        .isInstanceOf(CredenciaisInvalidasException.class)
        .hasMessage("Login ou senha inválidos.");
  }

  @Test
  void deveLancarCredenciaisInvalidasQuandoUsuarioInativo() {
    when(usuarioRepository.findByLogin("operadora")).thenReturn(Optional.of(usuario(false)));

    assertThatThrownBy(() -> authService.login("operadora", SENHA))
        .isInstanceOf(CredenciaisInvalidasException.class)
        .hasMessage("Login ou senha inválidos.");
  }

  @Test
  void deveDevolverUsuarioAtualQuandoAtivo() {
    when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(true)));

    assertThat(authService.usuarioAtual(7L))
        .isEqualTo(new UsuarioLogadoResposta(7L, "Operadora de Teste", Perfil.LANCAMENTO));
  }

  @Test
  void deveRecusarUsuarioAtualQuandoInativo() {
    when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(false)));

    assertThatThrownBy(() -> authService.usuarioAtual(7L))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void deveFalharNaSubidaQuandoSegredoCurto() {
    assertThatThrownBy(() -> new JwtConfig().chaveJwt(new JwtProperties("curto", 60)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SEGREDO");
  }
}
