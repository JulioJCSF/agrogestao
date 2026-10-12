package com.agrogestao.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;

/** Auditor provisório da F11. A F04b substituirá pelo usuário do token. */
@Configuration
@EnableJpaAuditing
public class AuditoriaConfig {
  @Bean
  public AuditorAware<Long> auditorInicial(
      JdbcTemplate jdbc, @Value("${ADMIN_LOGIN}") String login) {
    // JDBC evita disparar um flush JPA recursivo durante os callbacks de auditoria.
    return () ->
        jdbc
            .query("select id from usuario where login = ?", (rs, row) -> rs.getLong("id"), login)
            .stream()
            .findFirst();
  }
}
