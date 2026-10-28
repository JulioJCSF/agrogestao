package com.agrogestao.api.controller.plantio;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.RelogioAjustavel;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class PlantioIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired jakarta.persistence.EntityManager entityManager;
  @Autowired RelogioAjustavel relogio;
  Long autor, produtor, propriedade, cultura;

  @BeforeEach
  void criarLancador() {
    new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
  }

  @BeforeEach
  void preparar() {
    autor =
        jdbc.queryForObject(
            "insert into usuario(nome,login,senha_hash,perfil) values ('Autor Fictício Plantio','autor-plantio-it','hash-teste','LANCAMENTO') returning id",
            Long.class);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Plantio','99900000188',?) returning id",
            Long.class,
            autor);
    propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            autor);
    cultura =
        jdbc.queryForObject(
            "insert into cultura(nome,tipo_ciclo_padrao,unidade_medida,criado_por) values ('Cultura Fictícia Plantio','CURTO','kg',?) returning id",
            Long.class,
            autor);
  }

  @AfterEach
  void restaurarRelogio() {
    relogio.restaurar();
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

  Long propriedadeDe(Long dono, String nome) {
    return jdbc.queryForObject(
        "insert into propriedade(produtor_id,nome,criado_por) values (?,?,?) returning id",
        Long.class,
        dono,
        nome,
        autor);
  }

  void ratearDespesaIntegral(Number plantio) {
    Long categoria =
        jdbc.queryForObject(
            "insert into categoria_despesa(nome,criado_por) values ('Categoria Fictícia Transferência',?) returning id",
            Long.class,
            autor);
    Long despesa =
        jdbc.queryForObject(
            "insert into despesa(produtor_id,categoria_despesa_id,descricao,valor_total,data_fato,data_coleta,criado_por) values (?,?,'Despesa Fictícia Transferência',100.00,'2026-02-01','2026-02-01',?) returning id",
            Long.class,
            produtor,
            categoria,
            autor);
    jdbc.update(
        "insert into rateio_despesa(despesa_id,plantio_id,percentual,valor_rateado,criado_por) values (?,?,1,100.00,?)",
        despesa,
        plantio,
        autor);
  }

  void assertRateioIntegral(Number plantio) {
    assertThat(
            jdbc.queryForObject(
                "select valor_rateado from rateio_despesa where plantio_id=?",
                java.math.BigDecimal.class,
                plantio))
        .isEqualByComparingTo("100.00");
  }

  String corpoEm(Long outraPropriedade) {
    return corpo("1", "2026-02-01")
        .replace(
            "\"propriedadeId\":" + propriedade + ",",
            "\"propriedadeId\":" + outraPropriedade + ",");
  }

  @Test
  void deveRejeitarSemGravarQuandoTransferirParaPropriedadeDeOutroProdutor() throws Exception {
    Long outroProdutor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Outro Produtor Fictício Plantio','99900001150',?) returning id",
            Long.class,
            autor);
    Long propriedadeDoOutro = propriedadeDe(outroProdutor, "Sítio Fictício de Outro Produtor");
    Number id = criar("1", "2026-02-01");
    ratearDespesaIntegral(id);
    var antes =
        jdbc.queryForMap(
            "select propriedade_id,area,tipo_ciclo,data_inicio,previsao_colheita,atualizado_em from plantio where id=?",
            id);
    mvc.perform(
            put("/api/v1/plantios/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoEm(propriedadeDoOutro)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value("O plantio não pode ser transferido para propriedade de outro produtor."));
    entityManager.flush();
    assertThat(
            jdbc.queryForMap(
                "select propriedade_id,area,tipo_ciclo,data_inicio,previsao_colheita,atualizado_em from plantio where id=?",
                id))
        .isEqualTo(antes)
        .containsEntry("propriedade_id", propriedade);
    assertRateioIntegral(id);
  }

  @Test
  void devePermitirQuandoTransferirParaPropriedadeDoMesmoProdutor() throws Exception {
    Long outraPropriedade = propriedadeDe(produtor, "Segundo Sítio Fictício Plantio");
    Number id = criar("1", "2026-02-01");
    ratearDespesaIntegral(id);
    mvc.perform(
            put("/api/v1/plantios/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoEm(outraPropriedade)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.propriedadeId").value(outraPropriedade))
        .andExpect(jsonPath("$.produtorId").value(produtor));
    assertThat(jdbc.queryForObject("select propriedade_id from plantio where id=?", Long.class, id))
        .isEqualTo(outraPropriedade);
    assertRateioIntegral(id);
  }

  @Test
  void deveRetornar404QuandoAlterarParaPropriedadeInexistente() throws Exception {
    Number id = criar("1", "2026-02-01");
    mvc.perform(
            put("/api/v1/plantios/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoEm(Long.MAX_VALUE)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
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

  // C06b: encerramento. A cultura fictícia é CURTO como o Feijão da Q00a; o Feijão do catálogo é
  // LONGO e quebraria o tipoCiclo afirmado em criar().

  ResultActions encerrar(Number id, String data) throws Exception {
    return mvc.perform(
        post("/api/v1/plantios/" + id + "/encerramento")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"dataEncerramento\":\"" + data + "\"}"));
  }

  String dataEncerramento(Number id) {
    return jdbc.queryForObject(
        "select data_encerramento::text from plantio where id=?", String.class, id);
  }

  ResultActions despesa(Number plantio, String valor, String dataFato) throws Exception {
    Long adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    return mvc.perform(
        post("/api/v1/despesas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Plantio\",\"valorTotal\":%s,\"dataFato\":\"%s\",\"plantioIds\":[%s]}"
                    .formatted(produtor, adubo, valor, dataFato, plantio)));
  }

  Number lancarDespesa(Number plantio, String valor, String dataFato) throws Exception {
    return JsonPath.read(
        despesa(plantio, valor, dataFato)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  void assertBloqueadoPorLancamento(Number id, String dataCitada) throws Exception {
    encerrar(id, "2026-06-30")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "Há lançamento em "
                        + dataCitada
                        + ", posterior à data de encerramento informada."));
    assertThat(dataEncerramento(id)).isNull();
  }

  @Test
  void deveEncerrarQuandoDataForNoInicioOuDepois() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    Number pl2 = criar("1.000", "2026-02-01");
    for (var caso : java.util.Map.of(pl1, "2026-06-30", pl2, "2026-02-01").entrySet()) {
      encerrar(caso.getKey(), caso.getValue())
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/v1/plantios/" + caso.getKey()))
          .andExpect(jsonPath("$.dataEncerramento").value(caso.getValue()))
          .andExpect(jsonPath("$.situacao").value("ENCERRADO"));
      mvc.perform(get("/api/v1/plantios/" + caso.getKey()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.dataEncerramento").value(caso.getValue()))
          .andExpect(jsonPath("$.situacao").value("ENCERRADO"));
      assertThat(dataEncerramento(caso.getKey())).isEqualTo(caso.getValue());
    }
    mvc.perform(
            get("/api/v1/plantios")
                .param("produtorId", produtor.toString())
                .param("situacao", "ENCERRADO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", containsInAnyOrder(pl1.intValue(), pl2.intValue())));
    mvc.perform(
            get("/api/v1/plantios")
                .param("produtorId", produtor.toString())
                .param("situacao", "ABERTO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  void deveRejeitarSemGravarQuandoEncerramentoForAnteriorAoInicio() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    encerrar(pl1, "2026-01-31")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "A data de encerramento não pode ser anterior à data de início (01/02/2026)."));
    mvc.perform(get("/api/v1/plantios/" + pl1))
        .andExpect(jsonPath("$.situacao").value("ABERTO"))
        .andExpect(jsonPath("$.dataEncerramento").value(nullValue()));
    assertThat(dataEncerramento(pl1)).isNull();
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              var savepoint = connection.setSavepoint();
              try (var statement = connection.createStatement()) {
                assertThatThrownBy(
                        () ->
                            statement.executeUpdate(
                                "update plantio set data_encerramento='2026-01-31' where id="
                                    + pl1))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_plantio_encerra");
              } finally {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
              }
              return null;
            });
  }

  @Test
  void deveRejeitarLancamentoPosteriorQuandoPlantioForEncerradoPelaApi() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    lancarDespesa(pl1, "50.00", "2026-06-15");
    encerrar(pl1, "2026-06-30").andExpect(status().isCreated());
    lancarDespesa(pl1, "50.00", "2026-06-30");
    despesa(pl1, "50.00", "2026-07-01")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "O plantio foi encerrado em 30/06/2026. A data do fato não pode ser posterior"
                        + " ao encerramento."));
    assertThat(
            jdbc.queryForList(
                "select data_fato::text from despesa where produtor_id=? order by data_fato",
                String.class,
                produtor))
        .containsExactly("2026-06-15", "2026-06-30");
  }

  @Test
  void deveRejeitarSemGravarQuandoHouverLancamentoPosterior() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    lancarDespesa(pl1, "50.00", "2026-07-10");
    jdbc.update(
        "insert into producao(plantio_id,quantidade,unidade_medida,data_fato,data_coleta,criado_por) values (?,10,'kg','2026-06-20','2026-06-20',?)",
        pl1,
        autor);
    assertBloqueadoPorLancamento(pl1, "10/07/2026");
  }

  @Test
  void deveContarDespesaEstornadaQuandoEncerrar() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    Number despesa = lancarDespesa(pl1, "50.00", "2026-07-10");
    jdbc.update("update despesa set estornada = true where id=?", despesa);
    entityManager.clear();
    assertThat(
            jdbc.queryForObject("select estornada from despesa where id=?", Boolean.class, despesa))
        .isTrue();
    assertBloqueadoPorLancamento(pl1, "10/07/2026");
  }

  @Test
  void deveRejeitarQuandoCadaFonteTiverLancamentoPosterior() throws Exception {
    Long cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Plantio','FEIRA',?) returning id",
            Long.class,
            autor);
    Number comProducao = criar("1.000", "2026-02-01");
    jdbc.update(
        "insert into producao(plantio_id,quantidade,unidade_medida,data_fato,data_coleta,criado_por) values (?,10,'kg','2026-07-01','2026-07-01',?)",
        comProducao,
        autor);
    Number comDespesa = criar("1.000", "2026-02-01");
    lancarDespesa(comDespesa, "50.00", "2026-07-02");
    Number comDiaria = criar("1.000", "2026-02-01");
    jdbc.update(
        "insert into diaria(plantio_id,natureza,quantidade_dias,valor_unitario,data_fato,data_coleta,criado_por) values (?,'PAGA',1,80,'2026-07-03','2026-07-03',?)",
        comDiaria,
        autor);
    Number comVenda = criar("1.000", "2026-02-01");
    Long venda =
        jdbc.queryForObject(
            "insert into venda(produtor_id,cliente_id,valor_total,data_fato,data_coleta,criado_por) values (?,?,80,'2026-07-04','2026-07-04',?) returning id",
            Long.class,
            produtor,
            cliente,
            autor);
    jdbc.update(
        "insert into item_venda(venda_id,plantio_id,quantidade,unidade_medida,valor_unitario,valor_total,criado_por) values (?,?,1,'kg',80,80,?)",
        venda,
        comVenda,
        autor);
    assertBloqueadoPorLancamento(comProducao, "01/07/2026");
    assertBloqueadoPorLancamento(comDespesa, "02/07/2026");
    assertBloqueadoPorLancamento(comDiaria, "03/07/2026");
    assertBloqueadoPorLancamento(comVenda, "04/07/2026");
  }

  @Test
  void deveRejeitarQuandoPlantioJaEstiverEncerrado() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    encerrar(pl1, "2026-06-30").andExpect(status().isCreated());
    encerrar(pl1, "2026-07-15")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(jsonPath("$.mensagem").value("O plantio já foi encerrado em 30/06/2026."));
    assertThat(dataEncerramento(pl1)).isEqualTo("2026-06-30");
  }

  @Test
  void deveRejeitarEncerramentoFuturoQuandoUtcJaEstiverNoDiaSeguinte() throws Exception {
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
    Number pl1 = criar("1.000", "2026-02-01");
    encerrar(pl1, "2026-10-12")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.mensagem").value("A data de encerramento não pode ser posterior a hoje."));
    assertThat(dataEncerramento(pl1)).isNull();
    encerrar(pl1, "2026-10-11").andExpect(status().isCreated());
    assertThat(dataEncerramento(pl1)).isEqualTo("2026-10-11");
  }

  @Test
  void deveResponder400Ou404QuandoEncerramentoForInvalido() throws Exception {
    Number pl1 = criar("1.000", "2026-02-01");
    for (String corpo : java.util.List.of("{}", "{\"dataEncerramento\":null}"))
      mvc.perform(
              post("/api/v1/plantios/" + pl1 + "/encerramento")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(corpo))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"))
          .andExpect(jsonPath("$.campos[*].campo", hasItem("dataEncerramento")));
    encerrar(pl1, "2026-13-01")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[*].campo", hasItem("dataEncerramento")));
    encerrar(Long.MAX_VALUE, "2026-06-30")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.mensagem").value("Plantio não encontrado."));
    assertThat(dataEncerramento(pl1)).isNull();
  }
}
