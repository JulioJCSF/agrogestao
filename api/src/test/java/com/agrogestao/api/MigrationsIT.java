package com.agrogestao.api;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationsIT extends IntegracaoTest {
  @Autowired Flyway flyway;
  @Autowired JdbcTemplate jdbc;

  @Test
  void deveAplicarMigrationsQuandoBancoEstiverLimpo() {
    assertThat(flyway.info().pending()).isEmpty();
    assertThat(flyway.info().applied()).hasSize(12);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pg_extension where extname='btree_gist'", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from cultura", Integer.class)).isEqualTo(9);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from produtor where nome like 'Produtor Fictício Dev %'",
                Integer.class))
        .isZero();
  }

  @Test
  void deveRejeitarSobreposicaoQuandoVigenciasCompartilharemUmDia() {
    Long admin =
        jdbc.queryForObject(
            "select id from usuario where login='admin-teste-sintetico'", Long.class);
    jdbc.update(
        "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) values(80,'2024-01-01','2024-01-31',?)",
        admin);
    try {
      assertThatThrownBy(
              () ->
                  jdbc.update(
                      "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) values(90,'2024-01-31','2024-02-15',?)",
                      admin))
          .isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("ex_vrd_vigencia");
      jdbc.update(
          "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) values(90,'2024-02-01','2024-02-15',?)",
          admin);
    } finally {
      jdbc.update(
          "delete from valor_referencia_diaria where vigencia_inicio between '2024-01-01' and '2024-02-15'");
    }
  }
}
