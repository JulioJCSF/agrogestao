package com.agrogestao.api.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Massa adicional da F12, somente dev; mantém intactas as migrations aplicadas.
 *
 * <p>Os usuários de dev usam a senha de {@code DEV_SENHA_USUARIOS}, do .env. Sem ela, ou com menos
 * de 8 caracteres, não são criados. Já existindo, ficam como estão, com a senha trocada pela tela.
 */
@Configuration
@Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "agrogestao.massa-dev",
    havingValue = "true")
public class MassaDevConfig {
  private static final Logger log = LoggerFactory.getLogger(MassaDevConfig.class);
  private static final int SENHA_MINIMA = 8;

  @Bean
  ApplicationRunner carregarMassaSintetica(
      DataSource dataSource,
      PasswordEncoder passwordEncoder,
      @Value("${agrogestao.dev-senha-usuarios:}") String senhaUsuarios) {
    return args -> {
      criarUsuarios(new JdbcTemplate(dataSource), passwordEncoder, senhaUsuarios);
      new ResourceDatabasePopulator(new ClassPathResource("db/dev/dados-sinteticos.sql"))
          .execute(dataSource);
    };
  }

  private static void criarUsuarios(JdbcTemplate jdbc, PasswordEncoder encoder, String senha) {
    if (senha == null || senha.length() < SENHA_MINIMA) {
      log.warn(
          "DEV_SENHA_USUARIOS vazia ou com menos de {} caracteres: usuários de dev não criados.",
          SENHA_MINIMA);
      return;
    }
    String[][] usuarios = {
      {"Operador Fictício Dev", "operador.dev", "LANCAMENTO"},
      {"Consulta Fictícia Dev", "consulta.dev", "CONSULTA"}
    };
    for (String[] usuario : usuarios)
      jdbc.update(
          "insert into usuario(nome,login,senha_hash,perfil) values (?,?,?,?)"
              + " on conflict (login) do nothing",
          usuario[0],
          usuario[1],
          encoder.encode(senha),
          usuario[2]);
  }
}
