package com.agrogestao.api.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Autor ({@code criado_por}) dos registros (RF03, RNF07):
 *
 * <ul>
 *   <li>requisição com o token da API: o id do usuário do token ({@code sub}), sem consulta;
 *   <li>sem autenticação ou anônima (inicialização, massa dev, job): o admin do {@code
 *       ADMIN_LOGIN};
 *   <li>outra autenticação (como {@code @WithMockUser} em teste): o usuário cujo login é o nome
 *       autenticado; se não existir, falha, para nunca gravar como admin por acidente.
 * </ul>
 *
 * <p>As consultas usam JDBC para não disparar um flush JPA recursivo durante os callbacks de
 * auditoria.
 */
public class AutorDoRegistro implements AuditorAware<Long> {
  private final JdbcTemplate jdbc;
  private final String adminLogin;

  public AutorDoRegistro(JdbcTemplate jdbc, String adminLogin) {
    this.jdbc = jdbc;
    this.adminLogin = adminLogin;
  }

  @Override
  public Optional<Long> getCurrentAuditor() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao == null || autenticacao instanceof AnonymousAuthenticationToken)
      return idPorLogin(adminLogin);
    if (autenticacao instanceof JwtAuthenticationToken token)
      return Optional.of(Long.valueOf(token.getToken().getSubject()));
    String login = autenticacao.getName();
    return Optional.of(
        idPorLogin(login)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Não há usuário com o login '"
                            + login
                            + "' para ser o autor do registro. Cadastre o usuário ou autentique"
                            + " com o token da API.")));
  }

  private Optional<Long> idPorLogin(String login) {
    return jdbc
        .query("select id from usuario where login = ?", (rs, row) -> rs.getLong("id"), login)
        .stream()
        .findFirst();
  }
}
