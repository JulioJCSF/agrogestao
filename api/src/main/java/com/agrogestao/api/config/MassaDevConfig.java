package com.agrogestao.api.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Massa adicional da F12, somente dev; mantém intactas as migrations aplicadas. */
@Configuration
@Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "agrogestao.massa-dev",
    havingValue = "true")
public class MassaDevConfig {
  @Bean
  ApplicationRunner carregarMassaSintetica(DataSource dataSource) {
    return args ->
        new ResourceDatabasePopulator(new ClassPathResource("db/dev/dados-sinteticos.sql"))
            .execute(dataSource);
  }
}
