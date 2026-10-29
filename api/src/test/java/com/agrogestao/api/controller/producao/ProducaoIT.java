package com.agrogestao.api.controller.producao;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.RelogioAjustavel;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class ProducaoIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  Long autor, plantio, outro;

  @BeforeEach
  void preparar() {
    new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
    autor =
        jdbc.queryForObject(
            "select id from usuario where login=?", Long.class, UsuariosTeste.LANCAMENTO);
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Produção','99900000188',?) returning id",
            Long.class,
            autor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,criado_por) values (?,'Sítio Teste Produção',?) returning id",
            Long.class,
            produtor,
            autor);
    Long cultura = jdbc.queryForObject("select id from cultura where nome='Milho'", Long.class);
    plantio = plantio(propriedade, cultura);
    outro = plantio(propriedade, cultura);
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
  }

  Long plantio(Long propriedade, Long cultura) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1,'LONGO','2024-02-01',?) returning id",
        Long.class,
        propriedade,
        cultura,
        autor);
  }

  @AfterEach
  void restaurarRelogio() {
    relogio.restaurar();
  }

  ResultActions enviar(MockHttpServletRequestBuilder metodo, String corpo) throws Exception {
    return mvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  String corpo(String quantidade, String data) {
    return "{\"quantidade\":" + quantidade + ",\"dataFato\":\"" + data + "\"}";
  }

  Number criar(Long idPlantio, String quantidade) throws Exception {
    return JsonPath.read(
        enviar(
                post("/api/v1/plantios/" + idPlantio + "/producoes"),
                corpo(quantidade, "2024-03-10"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  @Test
  void deveUsarUnidadeDaCulturaEAuditoriaQuandoRegistrarColheita() throws Exception {
    var resposta =
        enviar(post("/api/v1/plantios/" + plantio + "/producoes"), corpo("250.125", "2024-03-10"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quantidade").value(250.125))
            .andExpect(jsonPath("$.unidadeMedida").value("kg"))
            .andExpect(jsonPath("$.plantioId").value(plantio))
            .andExpect(jsonPath("$.dataColeta").value("2026-10-11"))
            .andReturn()
            .getResponse();
    Number id = JsonPath.read(resposta.getContentAsString(), "$.id");
    assertThat(resposta.getHeader("Location")).isEqualTo("/api/v1/producoes/" + id);
    mvc.perform(get(resposta.getHeader("Location")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidade").value(250.125));
    var auditoria =
        jdbc.queryForMap("select criado_por,criado_em,atualizado_em from producao where id=?", id);
    assertThat(auditoria.get("criado_por")).isEqualTo(autor);
    assertThat(auditoria.get("criado_em")).isNotNull();
    assertThat(auditoria.get("atualizado_em")).isNotNull();
  }

  @Test
  void devePaginarSomenteOPlantioQuandoHouverVariasColheitas() throws Exception {
    Number primeiro = criar(plantio, "1.000"), segundo = criar(plantio, "0.001");
    criar(outro, "99.000");
    mvc.perform(
            get("/api/v1/plantios/" + plantio + "/producoes")
                .param("tamanho", "1")
                .param("pagina", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.pagina").value(1))
        .andExpect(jsonPath("$.itens[*].id", contains(segundo)));
    mvc.perform(delete("/api/v1/producoes/" + primeiro))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    mvc.perform(get("/api/v1/producoes/" + primeiro)).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/plantios/" + plantio + "/producoes"))
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get("/api/v1/plantios/" + outro + "/producoes"))
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void deveManterColetaEAutorQuandoAlterarQuantidadeUnidadeEData() throws Exception {
    Number id = criar(plantio, "1.000");
    relogio.fixar(Instant.parse("2026-10-20T12:00:00Z"));
    enviar(
            put("/api/v1/producoes/" + id),
            "{\"quantidade\":12.345,\"unidadeMedida\":\" KG \",\"dataFato\":\"2024-03-12\",\"dataColeta\":\"2020-01-01\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidade").value(12.345))
        .andExpect(jsonPath("$.unidadeMedida").value("kg"))
        .andExpect(jsonPath("$.dataFato").value("2024-03-12"))
        .andExpect(jsonPath("$.dataColeta").value("2026-10-11"));
    assertThat(
            jdbc.queryForObject("select data_coleta from producao where id=?", LocalDate.class, id))
        .isEqualTo(LocalDate.of(2026, 10, 11));
    assertThat(jdbc.queryForObject("select criado_por from producao where id=?", Long.class, id))
        .isEqualTo(autor);
  }

  @Test
  void deveValidarDatasEmCriacaoEAlteracaoQuandoPlantioEstiverEncerrado() throws Exception {
    jdbc.update("update plantio set data_encerramento='2024-06-30' where id=?", plantio);
    Number id = criar(plantio, "1.000");
    for (var metodo :
        List.of(
            post("/api/v1/plantios/" + plantio + "/producoes"), put("/api/v1/producoes/" + id))) {
      enviar(metodo, corpo("10", "2024-07-01"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.mensagem", containsString("30/06/2024")));
    }
    enviar(put("/api/v1/producoes/" + id), corpo("10", "2024-06-30")).andExpect(status().isOk());
    enviar(post("/api/v1/plantios/" + outro + "/producoes"), corpo("10", "2026-10-11"))
        .andExpect(status().isCreated());
    enviar(post("/api/v1/plantios/" + outro + "/producoes"), corpo("10", "2026-10-12"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRejeitarSemGravarQuandoQuantidadeOuFormatoForInvalido() throws Exception {
    String url = "/api/v1/plantios/" + plantio + "/producoes";
    for (String quantidade : List.of("0", "-1"))
      enviar(post(url), corpo(quantidade, "2024-03-10"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
    for (String invalido :
        List.of(
            "{}",
            corpo("1.0001", "2024-03-10"),
            corpo("1000000000", "2024-03-10"),
            "{\"quantidade\":1,\"dataFato\":\"ontem\"}",
            "{\"quantidade\":1,\"dataFato\":\"2024-03-10\",\"unidadeMedida\":\"   \"}"))
      enviar(post(url), invalido)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from producao where plantio_id=?", Long.class, plantio))
        .isZero();
    for (String tamanho : List.of("0", "101"))
      mvc.perform(get(url).param("tamanho", tamanho)).andExpect(status().isBadRequest());
  }

  @Test
  void deveRejeitarUnidadeDiferenteQuandoCriarOuAlterarColheita() throws Exception {
    Number id = criar(plantio, "1");
    String diferente = "{\"quantidade\":2,\"unidadeMedida\":\"saca\",\"dataFato\":\"2024-03-10\"}";
    enviar(post("/api/v1/plantios/" + plantio + "/producoes"), diferente)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.campos[*].campo", hasItem("unidadeMedida")));
    enviar(put("/api/v1/producoes/" + id), diferente).andExpect(status().isUnprocessableEntity());
    assertThat(
            jdbc.queryForObject("select unidade_medida from producao where id=?", String.class, id))
        .isEqualTo("kg");
  }

  @Test
  void devePreservarUnidadeQuandoCulturaOuPlantioJaTiverProducao() throws Exception {
    criar(plantio, "1");
    Long cultura =
        jdbc.queryForObject("select cultura_id from plantio where id=?", Long.class, plantio);
    enviar(
            put("/api/v1/culturas/" + cultura),
            "{\"nome\":\"Milho\",\"tipoCicloPadrao\":\"LONGO\",\"unidadeMedida\":\"saca\"}")
        .andExpect(status().isUnprocessableEntity());
    Long nova =
        jdbc.queryForObject(
            "insert into cultura(nome,tipo_ciclo_padrao,unidade_medida,criado_por) values ('Cultura Teste Sacas','LONGO','saca',?) returning id",
            Long.class,
            autor);
    Long propriedade =
        jdbc.queryForObject("select propriedade_id from plantio where id=?", Long.class, plantio);
    enviar(
            put("/api/v1/plantios/" + plantio),
            "{\"propriedadeId\":"
                + propriedade
                + ",\"culturaId\":"
                + nova
                + ",\"area\":1,\"dataInicio\":\"2024-02-01\"}")
        .andExpect(status().isUnprocessableEntity());
    assertThat(
            jdbc.queryForObject("select cultura_id from plantio where id=?", Long.class, plantio))
        .isEqualTo(cultura);
  }

  @Test
  void deveResponder404QuandoPlantioOuProducaoNaoExistir() throws Exception {
    mvc.perform(get("/api/v1/plantios/999999999/producoes")).andExpect(status().isNotFound());
    enviar(post("/api/v1/plantios/999999999/producoes"), corpo("1", "2024-03-10"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/producoes/999999999")).andExpect(status().isNotFound());
    enviar(put("/api/v1/producoes/999999999"), corpo("1", "2024-03-10"))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/producoes/999999999")).andExpect(status().isNotFound());
  }
}
