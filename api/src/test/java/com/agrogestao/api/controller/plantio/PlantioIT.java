package com.agrogestao.api.controller.plantio;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.jayway.jsonpath.JsonPath;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser
class PlantioIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired jakarta.persistence.EntityManager entityManager;
  Long autor, produtor, propriedade, cultura;

  @BeforeEach
  void preparar() {
    autor =
        jdbc.queryForObject(
            "insert into usuario(nome,login,senha_hash,perfil) values ('Autor Fictício Plantio','autor-plantio-it','hash-teste','LANCAMENTO') returning id",
            Long.class);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Fictício Plantio','99900000188',?) returning id",
            Long.class,
            autor);
    propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,criado_por) values (?,'Sítio Fictício Plantio',?) returning id",
            Long.class,
            produtor,
            autor);
    cultura =
        jdbc.queryForObject(
            "insert into cultura(nome,tipo_ciclo_padrao,unidade_medida,criado_por) values ('Cultura Fictícia Plantio','CURTO','kg',?) returning id",
            Long.class,
            autor);
  }

  String corpo(String area, String inicio) {
    return "{\"propriedadeId\":"
        + propriedade
        + ",\"culturaId\":"
        + cultura
        + ",\"area\":"
        + area
        + ",\"dataInicio\":\""
        + inicio
        + "\"}";
  }

  Number criar(String area, String inicio) throws Exception {
    String resposta =
        mvc.perform(
                post("/api/v1/plantios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(area, inicio)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/v1/plantios/")))
            .andExpect(jsonPath("$.tipoCiclo").value("CURTO"))
            .andExpect(jsonPath("$.situacao").value("ABERTO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(resposta, "$.id");
  }

  @Test
  void deveGravarPrecisaoEAuditoriaQuandoAreaPositiva() throws Exception {
    for (String area : java.util.List.of("2.500", "0.001")) {
      Number id = criar(area, "2026-02-01");
      assertThat(
              jdbc.queryForObject(
                  "select area from plantio where id=?", java.math.BigDecimal.class, id))
          .isEqualByComparingTo(area);
      assertThat(
              jdbc.queryForMap(
                      "select criado_em,atualizado_em,criado_por from plantio where id=?", id)
                  .values())
          .doesNotContainNull();
      mvc.perform(get("/api/v1/plantios/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.produtorId").value(produtor));
    }
  }

  @Test
  void deveRejeitarSemGravarQuandoAreaOuDataInvalida() throws Exception {
    for (String area : java.util.List.of("0", "-1"))
      mvc.perform(
              post("/api/v1/plantios")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo(area, "2026-02-01")))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
    for (String area : java.util.List.of("null", "0.0004", "1000000000"))
      mvc.perform(
              post("/api/v1/plantios")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo(area, "2026-02-01")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.campos[*].campo", hasItem("area")));
    mvc.perform(
            post("/api/v1/plantios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("1", "2999-01-01")))
        .andExpect(status().isUnprocessableEntity());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from plantio where propriedade_id=?", Long.class, propriedade))
        .isZero();
  }

  @Test
  void deveCombinarFiltrosEPaginarQuandoConsultarProdutor() throws Exception {
    Number primeiro = criar("1", "2026-02-01");
    Number segundo = criar("1", "2026-03-01");
    criar("1", "2026-04-01");
    jdbc.update("update plantio set data_encerramento='2026-06-30' where id=?", segundo);
    entityManager.clear();
    mvc.perform(
            get("/api/v1/plantios")
                .param("produtorId", produtor.toString())
                .param("culturaId", cultura.toString())
                .param("periodoInicio", "2026-02-01")
                .param("periodoFim", "2026-03-01")
                .param("tamanho", "1")
                .param("pagina", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[0].id").value(segundo));
    mvc.perform(
            get("/api/v1/plantios")
                .param("produtorId", produtor.toString())
                .param("situacao", "ENCERRADO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].id").value(segundo));
    mvc.perform(
            get("/api/v1/plantios")
                .param("produtorId", produtor.toString())
                .param("situacao", "ABERTO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[0].id").value(primeiro));
    mvc.perform(get("/api/v1/plantios").param("produtorId", Long.toString(Long.MAX_VALUE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  void deveManterRateioQuandoAlterarAreaECiclo() throws Exception {
    Number id = criar("2.500", "2026-02-01");
    Long categoria =
        jdbc.queryForObject(
            "insert into categoria_despesa(nome,criado_por) values ('Categoria Fictícia Plantio',?) returning id",
            Long.class,
            autor);
    Long despesa =
        jdbc.queryForObject(
            "insert into despesa(produtor_id,categoria_despesa_id,descricao,valor_total,data_fato,data_coleta,criado_por) values (?,?,'Despesa Fictícia',100,'2026-02-01','2026-02-01',?) returning id",
            Long.class,
            produtor,
            categoria,
            autor);
    jdbc.update(
        "insert into rateio_despesa(despesa_id,plantio_id,percentual,valor_rateado,criado_por) values (?,?,0.25,25,?)",
        despesa,
        id,
        autor);
    String body =
        corpo("5.125", "2026-02-01")
            .replace("}", ",\"tipoCiclo\":\"LONGO\",\"previsaoColheita\":\"2026-06-30\"}");
    mvc.perform(put("/api/v1/plantios/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.area").value(5.125))
        .andExpect(jsonPath("$.tipoCiclo").value("LONGO"));
    assertThat(
            jdbc.queryForObject(
                "select percentual from rateio_despesa where plantio_id=?",
                java.math.BigDecimal.class,
                id))
        .isEqualByComparingTo("0.25");
    assertThat(
            jdbc.queryForObject(
                "select valor_rateado from rateio_despesa where plantio_id=?",
                java.math.BigDecimal.class,
                id))
        .isEqualByComparingTo("25");
  }

  @Test
  void devePreservarRn03QuandoAlterarPlantioEncerrado() throws Exception {
    Number id = criar("1", "2026-02-01");
    jdbc.update("update plantio set data_encerramento='2026-06-30' where id=?", id);
    entityManager.clear();
    for (String inicio : java.util.List.of("2026-02-01", "2026-06-30"))
      mvc.perform(
              put("/api/v1/plantios/" + id)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo("1", inicio)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.dataEncerramento").value("2026-06-30"));
    mvc.perform(
            put("/api/v1/plantios/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("1", "2026-07-01")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
    assertThat(
            jdbc.queryForObject(
                "select data_inicio::text from plantio where id=?", String.class, id))
        .isEqualTo("2026-06-30");
  }

  @Test
  void deveProtegerAreaEEncerramentoQuandoSqlIgnorarService() throws Exception {
    Number id = criar("1", "2026-02-01");
    for (String atribuicao : java.util.List.of("area=0", "data_encerramento='2026-01-31'")) {
      jdbc.execute(
          (ConnectionCallback<Void>)
              connection -> {
                var savepoint = connection.setSavepoint();
                try (var statement = connection.createStatement()) {
                  assertThatThrownBy(
                          () ->
                              statement.executeUpdate(
                                  "update plantio set " + atribuicao + " where id=" + id))
                      .isInstanceOf(SQLException.class);
                } finally {
                  connection.rollback(savepoint);
                  connection.releaseSavepoint(savepoint);
                }
                return null;
              });
    }
  }

  @Test
  void deveValidarEntradaEVinculosQuandoRequisicaoInvalida() throws Exception {
    mvc.perform(get("/api/v1/plantios/" + Long.MAX_VALUE)).andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/plantios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    corpo("1", "2026-02-01")
                        .replace("\"culturaId\":" + cultura, "\"culturaId\":" + Long.MAX_VALUE)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/plantios").param("tamanho", "101")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/plantios").param("situacao", "INVALIDA"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/plantios")
                .param("periodoInicio", "2026-03-01")
                .param("periodoFim", "2026-02-01"))
        .andExpect(status().isUnprocessableEntity());
  }
}
