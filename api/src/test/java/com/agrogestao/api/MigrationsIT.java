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
    assertThat(flyway.info().applied()).hasSize(14);
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

  /**
   * E00: a carga inicial deixa só o admin, as categorias e as culturas; os gatilhos voltam ligados.
   */
  @Test
  void deveRemoverDadosDeExemploQuandoAplicarMigrations() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from produtor where documento in ('11111111111','22222222222')",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where login in ('titular','suplente','consulta')",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cliente where nome in ('Mercadinho Central','Feira Livre do Município',"
                    + "'CEASA Regional','Merenda Escolar Municipal','Cooperativa Agrícola Local','Comprador Avulso')",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from valor_referencia_diaria where (valor = 70.00 and vigencia_inicio = '2025-01-01'"
                    + " and vigencia_fim = '2025-12-31') or (valor = 80.00 and vigencia_inicio = '2026-01-01'"
                    + " and vigencia_fim is null)",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from emissao_relatorio where jsonb_typeof(conteudo->'produtor') = 'string'",
                Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from categoria_despesa", Integer.class))
        .isEqualTo(10);
    assertThat(jdbc.queryForObject("select count(*) from cultura", Integer.class)).isEqualTo(9);
    assertThat(
            jdbc.queryForList(
                "select tgenabled from pg_trigger where tgrelid = 'emissao_relatorio'::regclass"
                    + " and not tgisinternal",
                String.class))
        .containsExactly("O", "O");
  }

  /** A03c: os gatilhos da RN34 em emissao_relatorio e a função que eles chamam. */
  @Test
  void deveCriarGatilhosDaEmissaoQuandoAplicarMigrations() {
    assertThat(
            jdbc.queryForList(
                "select tgname from pg_trigger where tgrelid = 'emissao_relatorio'::regclass"
                    + " and not tgisinternal order by tgname",
                String.class))
        .containsExactly("tg_emissao_relatorio_imutavel", "tg_emissao_relatorio_sem_truncate");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pg_proc where proname = 'recusa_alteracao_emissao_relatorio'",
                Integer.class))
        .isOne();
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
