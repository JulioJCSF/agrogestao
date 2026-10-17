package com.agrogestao.api.config;

import static org.assertj.core.api.Assertions.*;

import com.agrogestao.api.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "dev"})
class MassaDevIT extends IntegracaoTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationRunner carregarMassaSintetica;

  @Test
  void deveCriarTresProdutoresSemDuplicarQuandoPerfilDevForAtivo() throws Exception {
    ApplicationArguments args = new DefaultApplicationArguments(new String[0]);
    carregarMassaSintetica.run(args);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from produtor where nome like 'Produtor Fictício Dev %'",
                Integer.class))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from propriedade where nome like 'Propriedade Fictícia Dev %'",
                Integer.class))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from plantio pl join propriedade pr on pr.id=pl.propriedade_id where pr.nome like 'Propriedade Fictícia Dev %'",
                Integer.class))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForList(
                "select documento from produtor where nome like 'Produtor Fictício Dev %'",
                String.class))
        .containsExactlyInAnyOrder("99900000188", "99900000269", "99900000340");
  }
}
