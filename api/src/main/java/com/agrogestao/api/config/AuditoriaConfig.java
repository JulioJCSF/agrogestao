package com.agrogestao.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Auditoria das entidades (RNF07). O autor é o usuário do token; fora de requisição, o admin do
 * {@code ADMIN_LOGIN}. As regras completas estão em {@link AutorDoRegistro}.
 */
@Configuration
@EnableJpaAuditing
public class AuditoriaConfig {
  @Bean
  public AuditorAware<Long> autorDoRegistro(
      JdbcTemplate jdbc, @Value("${ADMIN_LOGIN}") String login) {
    return new AutorDoRegistro(jdbc, login);
  }
}
