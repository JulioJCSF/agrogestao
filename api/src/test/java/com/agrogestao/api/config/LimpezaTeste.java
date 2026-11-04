package com.agrogestao.api.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Limpeza de emissões nos *IT. O banco recusa DELETE em {@code emissao_relatorio} (RN34, A03c);
 * aqui os gatilhos ficam desligados só na transação da limpeza, com {@code SET LOCAL
 * session_replication_role = replica}, que exige superusuário (o do Testcontainers). O {@code SET
 * LOCAL} acaba no commit: a conexão volta ao pool com os gatilhos ligados. Só teste usa isto.
 */
public final class LimpezaTeste {
  private LimpezaTeste() {}

  public static void apagarEmissoes(JdbcTemplate jdbc, Long produtorId) {
    new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()))
        .executeWithoutResult(
            status -> {
              jdbc.execute("set local session_replication_role = replica");
              jdbc.update("delete from emissao_relatorio where produtor_id = ?", produtorId);
            });
  }
}
