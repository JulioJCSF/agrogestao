package com.agrogestao.api.controller.referencia;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
class ValorReferenciaDiariaIT extends IntegracaoTest {
  static final String URL = "/api/v1/valores-referencia-diaria";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  Long autor;

  @BeforeEach
  void preparar() {
    autor =
        jdbc.queryForObject(
            "insert into usuario(nome,login,senha_hash,perfil) values ('Autor Fictício Referência','autor-referencia-it','hash-teste','LANCAMENTO') returning id",
            Long.class);
  }

  String corpo(String valor, String inicio, String fim) {
    return "{\"valor\":"
        + valor
        + ",\"vigenciaInicio\":\""
        + inicio
        + "\",\"vigenciaFim\":"
        + (fim == null ? "null" : "\"" + fim + "\"")
        + "}";
  }

  Number criar(String valor, String inicio, String fim) throws Exception {
    String body =
        mvc.perform(
                post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(valor, inicio, fim)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString(URL + "/")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  @Test
  void deveAceitarPeriodosAdjacentesQuandoNaoSobrepostosCt029() throws Exception {
    Number primeiro = criar("80.25", "2024-01-01", "2024-06-30");
    Number segundo = criar("90.50", "2024-07-01", "2024-12-31");
    for (String data : java.util.List.of("2024-01-01", "2024-06-30"))
      mvc.perform(get(URL + "/vigente").param("dataFato", data))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(primeiro))
          .andExpect(jsonPath("$.valor").value(80.25));
    mvc.perform(get(URL + "/vigente").param("dataFato", "2024-07-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(segundo));
    mvc.perform(get(URL + "/" + primeiro))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vigenciaFim").value("2024-06-30"));
    mvc.perform(get(URL).param("tamanho", "1").param("pagina", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens", hasSize(1)))
        .andExpect(jsonPath("$.pagina").value(1));
    assertThat(
            jdbc.queryForMap(
                    "select criado_em,atualizado_em,criado_por from valor_referencia_diaria where id=?",
                    primeiro)
                .values())
        .doesNotContainNull();
  }

  @Test
  void deveRejeitarSobreposicoesQuandoCriarOuAlterarCt030() throws Exception {
    criar("80", "2024-03-01", "2024-06-30");
    for (String[] intervalo :
        new String[][] {
          {"2024-02-01", "2024-03-01"},
          {"2024-06-30", "2024-07-31"},
          {"2024-04-01", "2024-05-31"},
          {"2024-01-01", "2024-12-31"},
          {"2024-03-01", "2024-06-30"},
          {"2024-05-01", null}
        })
      mvc.perform(
              post(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo("90", intervalo[0], intervalo[1])))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
          .andExpect(jsonPath("$.mensagem", containsString("sobreposição")));
    Number outro = criar("95", "2024-07-01", "2024-12-31");
    mvc.perform(
            put(URL + "/" + outro)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("95", "2024-06-30", "2024-12-31")))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(get(URL + "/" + outro))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vigenciaInicio").value("2024-07-01"));
  }

  @Test
  void deveAceitarDiaUnicoEVigenciaAbertaQuandoLivre() throws Exception {
    // Only this rolled-back, ephemeral test removes the legacy reference seed.
    jdbc.update("delete from diaria where valor_referencia_diaria_id is not null");
    jdbc.update("delete from valor_referencia_diaria");
    Number aberto = criar("90", "2024-07-01", null);
    mvc.perform(get(URL + "/vigente").param("dataFato", "2099-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(aberto));
    mvc.perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("95", "2024-08-01", "2024-08-31")))
        .andExpect(status().isUnprocessableEntity());
    // Closing our own period allows adjacent periods afterward.
    mvc.perform(
            put(URL + "/" + aberto)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("90", "2024-07-01", "2024-12-31")))
        .andExpect(status().isOk());
    Number dia = criar("0.01", "2024-06-30", "2024-06-30");
    mvc.perform(get(URL + "/vigente").param("dataFato", "2024-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(dia));
  }

  @Test
  void deveValidarFormatoValorEPeriodoQuandoEntradaInvalida() throws Exception {
    for (String valor : java.util.List.of("0", "-1"))
      mvc.perform(
              post(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo(valor, "2024-01-01", "2024-12-31")))
          .andExpect(status().isUnprocessableEntity());
    for (String valor : java.util.List.of("null", "0.001", "10000000000"))
      mvc.perform(
              post(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo(valor, "2024-01-01", "2024-12-31")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.campos[*].campo", hasItem("valor")));
    mvc.perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("80", "2024-02-01", "2024-01-01")))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(get(URL + "/vigente")).andExpect(status().isBadRequest());
    mvc.perform(get(URL + "/vigente").param("dataFato", "invalida"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(URL).param("tamanho", "0")).andExpect(status().isBadRequest());
    mvc.perform(get(URL + "/" + Long.MAX_VALUE)).andExpect(status().isNotFound());
    mvc.perform(get(URL + "/vigente").param("dataFato", "1900-01-01"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem", containsString("Cadastre")));
  }

  @Test
  void devePreservarDiariaQuandoAlterarValorEVigenciaRn18() throws Exception {
    Number referencia = criar("80", "2024-01-01", "2024-12-31");
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Fictício Referência','99900000188',?) returning id",
            Long.class,
            autor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,criado_por) values (?,'Sítio Fictício Referência',?) returning id",
            Long.class,
            produtor,
            autor);
    Long cultura =
        jdbc.queryForObject(
            "insert into cultura(nome,tipo_ciclo_padrao,unidade_medida,criado_por) values ('Cultura Fictícia Referência','CURTO','kg',?) returning id",
            Long.class,
            autor);
    Long plantio =
        jdbc.queryForObject(
            "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1,'CURTO','2024-01-01',?) returning id",
            Long.class,
            propriedade,
            cultura,
            autor);
    Long diaria =
        jdbc.queryForObject(
            "insert into diaria(plantio_id,valor_referencia_diaria_id,natureza,quantidade_dias,valor_unitario,data_fato,data_coleta,criado_por) values (?,?,'FAMILIAR',1,80,'2024-02-01','2024-02-01',?) returning id",
            Long.class,
            plantio,
            referencia,
            autor);
    var antes =
        jdbc.queryForMap(
            "select valor_unitario,valor_referencia_diaria_id,atualizado_em from diaria where id=?",
            diaria);
    mvc.perform(
            put(URL + "/" + referencia)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("100.25", "2024-01-01", "2024-06-30")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valor").value(100.25));
    assertThat(
            jdbc.queryForMap(
                "select valor_unitario,valor_referencia_diaria_id,atualizado_em from diaria where id=?",
                diaria))
        .isEqualTo(antes);
    assertThat(
            jdbc.queryForObject(
                "select valor_unitario from diaria where id=?", BigDecimal.class, diaria))
        .isEqualByComparingTo("80");
  }

  @Test
  void deveProtegerContraSobreposicaoQuandoSqlIgnorarService() throws Exception {
    Number id = criar("80", "2024-01-01", "2024-06-30");
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              var savepoint = connection.setSavepoint();
              try (var statement = connection.createStatement()) {
                assertThatThrownBy(
                        () ->
                            statement.executeUpdate(
                                "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) select valor,vigencia_inicio,vigencia_fim,criado_por from valor_referencia_diaria where id="
                                    + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ex_vrd_vigencia");
              } finally {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
              }
              return null;
            });
  }
}
