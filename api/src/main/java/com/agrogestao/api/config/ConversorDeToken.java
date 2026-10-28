package com.agrogestao.api.config;

import com.agrogestao.api.repository.SituacaoUsuario;
import com.agrogestao.api.repository.UsuarioRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * O token prova a identidade ({@code sub}); perfil e situação vêm da tabela {@code usuario} a cada
 * requisição, e a claim {@code perfil} é ignorada. Rebaixar ou desativar vale na hora (F04b).
 *
 * <p>O principal continua sendo o {@link Jwt}, com o id do usuário no {@code sub}. A exceção de
 * autenticação chega ao {@code JsonPontoDeEntradaDeAutenticacao} como 401.
 */
@Component
public class ConversorDeToken implements Converter<Jwt, AbstractAuthenticationToken> {
  private final UsuarioRepository usuarios;

  public ConversorDeToken(UsuarioRepository usuarios) {
    this.usuarios = usuarios;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Long id;
    try {
      id = Long.valueOf(jwt.getSubject());
    } catch (NumberFormatException e) {
      throw new BadCredentialsException("Token sem identificador de usuário válido.");
    }
    SituacaoUsuario situacao =
        usuarios
            .findSituacaoById(id)
            .filter(SituacaoUsuario::ativo)
            .orElseThrow(
                () -> new BadCredentialsException("Usuário do token inexistente ou inativo."));
    return new JwtAuthenticationToken(
        jwt,
        List.of(new SimpleGrantedAuthority("ROLE_" + situacao.perfil().name())),
        jwt.getSubject());
  }
}
