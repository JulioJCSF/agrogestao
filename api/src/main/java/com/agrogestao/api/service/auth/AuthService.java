package com.agrogestao.api.service.auth;

import com.agrogestao.api.config.JwtProperties;
import com.agrogestao.api.dto.LoginResposta;
import com.agrogestao.api.dto.UsuarioLogadoResposta;
import com.agrogestao.api.exception.CredenciaisInvalidasException;
import com.agrogestao.api.model.Usuario;
import com.agrogestao.api.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login do operador e emissão do JWT (RF01). */
@Service
public class AuthService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;
  private final String hashFicticio;

  public AuthService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder,
      JwtProperties jwtProperties) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.jwtProperties = jwtProperties;
    this.hashFicticio = passwordEncoder.encode("senha-de-usuario-inexistente");
  }

  @Transactional(readOnly = true)
  public LoginResposta login(String login, String senha) {
    Usuario usuario = usuarioRepository.findByLogin(login).orElse(null);
    // Login inexistente também passa pelo BCrypt, para o tempo de resposta não revelar se existe.
    boolean senhaConfere =
        passwordEncoder.matches(senha, usuario == null ? hashFicticio : usuario.getSenhaHash());
    if (usuario == null || !senhaConfere || !usuario.isAtivo()) {
      throw new CredenciaisInvalidasException();
    }

    Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant expiraEm = agora.plus(jwtProperties.expiracaoMinutos(), ChronoUnit.MINUTES);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(String.valueOf(usuario.getId()))
            .issuedAt(agora)
            .expiresAt(expiraEm)
            .claim("login", usuario.getLogin())
            .claim("perfil", usuario.getPerfil().name())
            .build();
    JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

    return new LoginResposta(token, expiraEm, paraResposta(usuario));
  }

  /** Usuário dono do token. Token de usuário removido ou desativado deixa de valer aqui. */
  @Transactional(readOnly = true)
  public UsuarioLogadoResposta usuarioAtual(Long id) {
    return usuarioRepository
        .findById(id)
        .filter(Usuario::isAtivo)
        .map(AuthService::paraResposta)
        .orElseThrow(() -> new BadCredentialsException("Usuário do token inexistente ou inativo"));
  }

  private static UsuarioLogadoResposta paraResposta(Usuario usuario) {
    return new UsuarioLogadoResposta(usuario.getId(), usuario.getNome(), usuario.getPerfil());
  }
}
