package com.agrogestao.api.controller.diaria;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.model.Diaria;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.Plantio;
import com.agrogestao.api.repository.DespesaRepository;
import com.agrogestao.api.repository.DiariaRepository;
import com.agrogestao.api.repository.RateioDespesaRepository;
import com.agrogestao.api.service.diaria.DiariaService;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.plantio.PlantioService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class DiariaIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  @MockitoSpyBean RateioDespesaRepository rateios;
  @MockitoSpyBean DespesaRepository despesas;
  @MockitoSpyBean DiariaRepository diarias;
  @MockitoSpyBean LancamentoService lancamentos;
  @Autowired DiariaService service;
  @Autowired PlantioService plantios;
  Long autor, produtor, propriedade, plantio, outro, referencia, adubo, categoriaDiaria;

  @BeforeEach
  void preparar() {
    autor = new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Diária','99900001230',?) returning id",
            Long.class,
            autor);
    propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste Diária',10,?) returning id",
            Long.class,
            produtor,
            autor);
    Long cultura = jdbc.queryForObject("select id from cultura where nome='Milho'", Long.class);
    plantio = plantio(cultura);
    outro = plantio(cultura);
    referencia =
        jdbc.queryForObject(
            "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) values (80,'2024-01-01','2024-12-31',?) returning id",
            Long.class,
            autor);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome='Adubo e fertilizantes'", Long.class);
    categoriaDiaria =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome='Diária' and sistema", Long.class);
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
  }

  Long plantio(Long cultura) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1,'LONGO','2024-02-01',?) returning id",
        Long.class,
        propriedade,
        cultura,
        autor);
  }

  @AfterEach
  void limpar() {
    reset(rateios, despesas, diarias, lancamentos);
    relogio.restaurar();
    jdbc.update("delete from emissao_relatorio where produtor_id=?", produtor);
    jdbc.update("delete from despesa where produtor_id=?", produtor);
    jdbc.update("delete from diaria where plantio_id in (?,?)", plantio, outro);
    jdbc.update("delete from valor_referencia_diaria where id=?", referencia);
    jdbc.update("delete from plantio where propriedade_id=?", propriedade);
    jdbc.update("delete from propriedade where id=?", propriedade);
    jdbc.update("delete from produtor where id=?", produtor);
    jdbc.update("delete from usuario where id=?", autor);
  }

  ResultActions enviar(MockHttpServletRequestBuilder metodo, String corpo) throws Exception {
    return mvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  String corpo(String natureza, String dias, String valor, String data) {
    return "{\"natureza\":\""
        + natureza
        + "\",\"quantidadeDias\":"
        + dias
        + ",\"valorUnitario\":"
        + valor
        + ",\"dataFato\":\""
        + data
        + "\",\"dataColeta\":\"2020-01-01\"}";
  }

  Number criar(String natureza, String dias, String valor) throws Exception {
    return JsonPath.read(
        enviar(
                post("/api/v1/plantios/" + plantio + "/diarias"),
                corpo(natureza, dias, valor, "2024-03-10"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  @Test
  void deveGravarNaturezaEReferenciaQuandoCriarPagaEFamiliarCt021() throws Exception {
    Number paga = criar("PAGA", "1", "90"), familiar = criar("FAMILIAR", "1", "50");
    mvc.perform(get("/api/v1/diarias/" + paga))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.natureza").value("PAGA"))
        .andExpect(jsonPath("$.valorUnitario").value(90))
        .andExpect(jsonPath("$.valorReferenciaDiariaId").value(nullValue()));
    mvc.perform(get("/api/v1/diarias/" + familiar))
        .andExpect(jsonPath("$.natureza").value("FAMILIAR"))
        .andExpect(jsonPath("$.valorUnitario").value(80))
        .andExpect(jsonPath("$.valorReferenciaDiariaId").value(referencia));
    mvc.perform(
            get("/api/v1/plantios/" + plantio + "/diarias")
                .param("tamanho", "1")
                .param("pagina", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", contains(familiar)));
    mvc.perform(get("/api/v1/plantios/" + outro + "/diarias"))
        .andExpect(jsonPath("$.total").value(0));
    assertThat(jdbc.queryForObject("select natureza from diaria where id=?", String.class, paga))
        .isEqualTo("PAGA");
    assertThat(
            jdbc.queryForObject("select natureza from diaria where id=?", String.class, familiar))
        .isEqualTo("FAMILIAR");
  }

  @Test
  void deveRejeitarSemGravarQuandoNaturezaOuPrecisaoForInvalidaCt022() throws Exception {
    String url = "/api/v1/plantios/" + plantio + "/diarias";
    for (String invalido :
        List.of(
            "{\"quantidadeDias\":1,\"dataFato\":\"2024-03-10\"}",
            "{\"natureza\":null,\"quantidadeDias\":1,\"dataFato\":\"2024-03-10\"}",
            corpo("VOLUNTARIA", "1", "90", "2024-03-10"),
            corpo("paga", "1", "90", "2024-03-10")))
      enviar(post(url), invalido)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.campos[*].campo", hasItem("natureza")));
    for (String dias : List.of("null", "1.001", "10000"))
      enviar(post(url), corpo("PAGA", dias, "90", "2024-03-10"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.campos[*].campo", hasItem("quantidadeDias")));
    for (String valor : List.of("1.001", "10000000000"))
      enviar(post(url), corpo("PAGA", "1", valor, "2024-03-10")).andExpect(status().isBadRequest());
    assertThat(contar("diaria")).isZero();
    assertThat(contar("despesa")).isZero();
    for (String tamanho : List.of("0", "101"))
      mvc.perform(get(url).param("tamanho", tamanho)).andExpect(status().isBadRequest());
  }

  long contar(String tabela) {
    return jdbc.queryForObject(
        tabela.equals("diaria")
            ? "select count(*) from diaria where plantio_id in (?,?)"
            : "select count(*) from despesa where produtor_id=?",
        Long.class,
        tabela.equals("diaria") ? new Object[] {plantio, outro} : new Object[] {produtor});
  }

  @Test
  void deveGerarDespesaEReverterTudoQuandoRateioFalharCt026() throws Exception {
    var resposta =
        enviar(
                post("/api/v1/plantios/" + plantio + "/diarias"),
                corpo("PAGA", "2.50", "80.25", "2024-03-10"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.valorTotal").value(200.63))
            .andExpect(jsonPath("$.dataColeta").value("2026-10-11"))
            .andReturn()
            .getResponse();
    Number diaria = JsonPath.read(resposta.getContentAsString(), "$.id");
    assertThat(resposta.getHeader("Location")).isEqualTo("/api/v1/diarias/" + diaria);
    var despesa = jdbc.queryForMap("select * from despesa where diaria_id=?", diaria);
    assertThat(despesa.get("categoria_despesa_id")).isEqualTo(categoriaDiaria);
    assertThat(despesa.get("produtor_id")).isEqualTo(produtor);
    assertThat((BigDecimal) despesa.get("valor_total")).isEqualByComparingTo("200.63");
    assertThat(despesa.get("data_fato").toString()).isEqualTo("2024-03-10");
    assertThat(despesa.get("data_coleta").toString()).isEqualTo("2026-10-11");
    assertThat(despesa.get("estornada")).isEqualTo(false);
    assertThat(despesa.get("criado_por")).isEqualTo(autor);
    confereRateio(despesa.get("id"), "200.63");
    doThrow(new DataIntegrityViolationException("Falha de rateio de teste"))
        .when(rateios)
        .saveAndFlush(any());
    enviar(post("/api/v1/plantios/" + plantio + "/diarias"), corpo("PAGA", "1", "90", "2024-03-11"))
        .andExpect(status().isUnprocessableEntity());
    assertThat(contar("diaria")).isEqualTo(1);
    assertThat(contar("despesa")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rateio_despesa where plantio_id=?", Long.class, plantio))
        .isEqualTo(1);
  }

  void confereRateio(Object despesa, String total) {
    var linhas = jdbc.queryForList("select * from rateio_despesa where despesa_id=?", despesa);
    assertThat(linhas).hasSize(1);
    var r = linhas.getFirst();
    assertThat(r.get("plantio_id")).isEqualTo(plantio);
    assertThat((BigDecimal) r.get("percentual")).isEqualByComparingTo("1.000000");
    assertThat((BigDecimal) r.get("valor_rateado")).isEqualByComparingTo(total);
  }

  @Test
  void deveSincronizarDespesaQuandoAlterarOuExcluirPagaCt027() throws Exception {
    Number diaria = criar("PAGA", "2", "80");
    var antes = jdbc.queryForMap("select * from despesa where diaria_id=?", diaria);
    String manual =
        "{\"produtorId\":"
            + produtor
            + ",\"categoriaDespesaId\":"
            + adubo
            + ",\"descricao\":\"Adubo de teste\",\"valorTotal\":30,\"dataFato\":\"2024-03-10\",\"plantioIds\":["
            + plantio
            + "]}";
    enviar(post("/api/v1/despesas"), manual).andExpect(status().isCreated());
    relogio.fixar(Instant.parse("2026-10-20T15:00:00Z"));
    enviar(put("/api/v1/diarias/" + diaria), corpo("PAGA", "3", "85.50", "2024-03-12"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorTotal").value(256.5))
        .andExpect(jsonPath("$.dataColeta").value("2026-10-11"));
    var depois = jdbc.queryForMap("select * from despesa where diaria_id=?", diaria);
    assertThat(depois.get("id")).isEqualTo(antes.get("id"));
    assertThat(depois.get("criado_em")).isEqualTo(antes.get("criado_em"));
    assertThat(depois.get("data_coleta")).isEqualTo(antes.get("data_coleta"));
    assertThat((BigDecimal) depois.get("valor_total")).isEqualByComparingTo("256.50");
    assertThat(depois.get("data_fato").toString()).isEqualTo("2024-03-12");
    confereRateio(depois.get("id"), "256.50");
    mvc.perform(delete("/api/v1/diarias/" + diaria))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    assertThat(contar("diaria")).isZero();
    assertThat(contar("despesa")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rateio_despesa where despesa_id=?",
                Long.class,
                antes.get("id")))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select valor_total from despesa where produtor_id=?", BigDecimal.class, produtor))
        .isEqualByComparingTo("30");
  }

  @Test
  void deveProtegerDespesaGeradaENaoGerarNaFamiliarCt028() throws Exception {
    Number diaria = criar("PAGA", "1", "90"), familiar = criar("FAMILIAR", "1", "null");
    var antes = jdbc.queryForMap("select * from despesa where diaria_id=?", diaria);
    String manual =
        "{\"produtorId\":"
            + produtor
            + ",\"categoriaDespesaId\":"
            + adubo
            + ",\"descricao\":\"Alteração indevida\",\"valorTotal\":999,\"dataFato\":\"2024-03-10\",\"plantioIds\":["
            + plantio
            + "]}";
    enviar(put("/api/v1/despesas/" + antes.get("id")), manual)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem", containsString("diária")));
    mvc.perform(delete("/api/v1/despesas/" + antes.get("id")))
        .andExpect(status().isUnprocessableEntity());
    enviar(
            post("/api/v1/despesas"),
            manual.replace(
                "\"categoriaDespesaId\":" + adubo, "\"categoriaDespesaId\":" + categoriaDiaria))
        .andExpect(status().isUnprocessableEntity());
    assertThat(jdbc.queryForMap("select * from despesa where diaria_id=?", diaria))
        .isEqualTo(antes);
    assertThat(contar("despesa")).isEqualTo(1);
    confereRateio(antes.get("id"), "90");
    mvc.perform(delete("/api/v1/diarias/" + familiar)).andExpect(status().isNoContent());
    assertThat(contar("despesa")).isEqualTo(1);
  }

  @Test
  void devePreservarHistoricoQuandoReferenciaMudarEAlteracaoMantiverData() throws Exception {
    Number familiar = criar("FAMILIAR", "1.5", "null");
    jdbc.update("update valor_referencia_diaria set valor=95 where id=?", referencia);
    mvc.perform(get("/api/v1/diarias/" + familiar))
        .andExpect(jsonPath("$.valorUnitario").value(80))
        .andExpect(jsonPath("$.valorTotal").value(120));
    enviar(put("/api/v1/diarias/" + familiar), corpo("FAMILIAR", "2", "999", "2024-03-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorUnitario").value(80))
        .andExpect(jsonPath("$.valorTotal").value(160));
    enviar(put("/api/v1/diarias/" + familiar), corpo("FAMILIAR", "2", "null", "2024-03-11"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorUnitario").value(95));
  }

  @Test
  void deveRejeitarSemMudarRegistrosQuandoNaturezaOuDatasViolaremRegras() throws Exception {
    Number paga = criar("PAGA", "1", "90");
    enviar(put("/api/v1/diarias/" + paga), corpo("FAMILIAR", "1", "null", "2024-03-10"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.campos[*].campo", hasItem("natureza")));
    Number familiar = criar("FAMILIAR", "1", "null");
    enviar(put("/api/v1/diarias/" + familiar), corpo("PAGA", "1", "90", "2024-03-10"))
        .andExpect(status().isUnprocessableEntity());
    jdbc.update("update plantio set data_encerramento='2024-06-30' where id=?", plantio);
    for (String data : List.of("2024-07-01", "2026-10-12")) {
      enviar(post("/api/v1/plantios/" + plantio + "/diarias"), corpo("PAGA", "1", "90", data))
          .andExpect(status().isUnprocessableEntity());
      enviar(put("/api/v1/diarias/" + paga), corpo("PAGA", "1", "90", data))
          .andExpect(status().isUnprocessableEntity());
    }
    enviar(put("/api/v1/diarias/" + paga), corpo("PAGA", "1", "90", "2024-06-30"))
        .andExpect(status().isOk());
    enviar(post("/api/v1/plantios/" + outro + "/diarias"), corpo("PAGA", "1", "90", "2026-10-11"))
        .andExpect(status().isCreated());
  }

  @Test
  void deveResponder422QuandoSemReferenciaValorOuDiasValidos() throws Exception {
    String url = "/api/v1/plantios/" + plantio + "/diarias";
    enviar(post(url), corpo("FAMILIAR", "1", "null", "2023-12-31"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem", containsString("Cadastre")));
    enviar(post(url), corpo("PAGA", "1", "null", "2024-03-10"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.campos[*].campo", hasItem("valorUnitario")));
    for (String dias : List.of("0", "-1"))
      enviar(post(url), corpo("PAGA", dias, "90", "2024-03-10"))
          .andExpect(status().isUnprocessableEntity());
    for (String valor : List.of("0", "-1"))
      enviar(post(url), corpo("PAGA", "1", valor, "2024-03-10"))
          .andExpect(status().isUnprocessableEntity());
    enviar(post(url), corpo("PAGA", "0.01", "0.01", "2024-03-10"))
        .andExpect(status().isUnprocessableEntity());
    enviar(post(url), corpo("PAGA", "9999.99", "9999999999.99", "2024-03-10"))
        .andExpect(status().isUnprocessableEntity());
    assertThat(contar("diaria")).isZero();
    assertThat(contar("despesa")).isZero();
  }

  @Test
  void deveProtegerIntegridadeQuandoSqlIgnorarServicoCt022Ct026() throws Exception {
    for (String natureza : Arrays.asList("OUTRA", null, "FAMILIAR")) {
      assertThatThrownBy(
              () ->
                  jdbc.update(
                      "insert into diaria(plantio_id,natureza,quantidade_dias,valor_unitario,data_fato,data_coleta,criado_por) values (?,?,1,80,'2024-03-10','2024-03-10',?)",
                      plantio,
                      natureza,
                      autor))
          .isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining(
              natureza == null
                  ? "natureza"
                  : natureza.equals("FAMILIAR") ? "ck_diaria_ref" : "ck_diaria_natureza");
    }
    assertThat(contar("diaria")).isZero();
    Number paga = criar("PAGA", "1", "90");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into despesa(produtor_id,categoria_despesa_id,diaria_id,descricao,valor_total,data_fato,data_coleta,criado_por) select produtor_id,categoria_despesa_id,diaria_id,descricao,valor_total,data_fato,data_coleta,criado_por from despesa where diaria_id=?",
                    paga))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("despesa_diaria_id_key");
    assertThat(contar("despesa")).isEqualTo(1);
  }

  @Test
  void deveReverterDiariaDespesaERateioQuandoAlteracaoOuExclusaoFalhar() throws Exception {
    Number paga = criar("PAGA", "1", "90");
    var diariaAntes = jdbc.queryForMap("select * from diaria where id=?", paga);
    var despesaAntes = jdbc.queryForMap("select * from despesa where diaria_id=?", paga);
    var rateioAntes =
        jdbc.queryForList(
            "select * from rateio_despesa where despesa_id=?", despesaAntes.get("id"));
    doThrow(new DataIntegrityViolationException("Falha de despesa de teste"))
        .when(despesas)
        .saveAndFlush(any());
    enviar(put("/api/v1/diarias/" + paga), corpo("PAGA", "3", "85.50", "2024-03-12"))
        .andExpect(status().isUnprocessableEntity());
    reset(despesas);
    doThrow(new DataIntegrityViolationException("Falha de exclusão de teste"))
        .when(diarias)
        .delete(any(Diaria.class));
    mvc.perform(delete("/api/v1/diarias/" + paga)).andExpect(status().isUnprocessableEntity());
    assertThat(jdbc.queryForMap("select * from diaria where id=?", paga)).isEqualTo(diariaAntes);
    assertThat(jdbc.queryForMap("select * from despesa where diaria_id=?", paga))
        .isEqualTo(despesaAntes);
    assertThat(
            jdbc.queryForList(
                "select * from rateio_despesa where despesa_id=?", despesaAntes.get("id")))
        .isEqualTo(rateioAntes);
  }

  @Test
  void deveRejeitarEncerramentoAnteriorQuandoDiariaConcorrenteEstiverEmGravacao() throws Exception {
    CountDownLatch lendo = new CountDownLatch(1), liberar = new CountDownLatch(1);
    doAnswer(
            inv -> {
              lendo.countDown();
              assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
              return inv.callRealMethod();
            })
        .when(lancamentos)
        .validarDataFato(any(LocalDate.class), any(Plantio.class));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<?> diaria =
          executor.submit(
              () ->
                  service.criar(
                      plantio,
                      new DiariaRequisicao(
                          com.agrogestao.api.model.NaturezaDiaria.PAGA,
                          BigDecimal.ONE,
                          new BigDecimal("90"),
                          LocalDate.of(2024, 7, 1))));
      assertThat(lendo.await(10, TimeUnit.SECONDS)).isTrue();
      Future<?> encerramento =
          executor.submit(
              () ->
                  plantios.encerrar(
                      plantio, new EncerramentoPlantioRequisicao(LocalDate.of(2024, 6, 30))));
      try {
        encerramento.get(1, TimeUnit.SECONDS);
      } catch (TimeoutException esperado) {
        /* Aguarda o lock da diária. */
      }
      liberar.countDown();
      diaria.get(10, TimeUnit.SECONDS);
      assertThatThrownBy(() -> encerramento.get(10, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RegraNegocioException.class);
    } finally {
      liberar.countDown();
    }
    assertThat(
            jdbc.queryForObject(
                "select data_encerramento from plantio where id=?", LocalDate.class, plantio))
        .isNull();
  }

  @Test
  void deveRecusarExclusaoQuandoDespesaDaDiariaEstiverEmPeriodoEmitido() throws Exception {
    Number paga = criar("PAGA", "1", "90");
    Long emissao =
        jdbc.queryForObject(
            "insert into emissao_relatorio(produtor_id,usuario_id,periodo_inicio,periodo_fim,conteudo,criado_por) values (?,?,'2024-03-01','2024-03-31','{\"teste\":\"snapshot preservado\"}'::jsonb,?) returning id",
            Long.class,
            produtor,
            autor,
            autor);
    var antes = jdbc.queryForMap("select * from despesa where diaria_id=?", paga);
    String snapshot =
        jdbc.queryForObject(
            "select conteudo::text from emissao_relatorio where id=?", String.class, emissao);
    mvc.perform(delete("/api/v1/diarias/" + paga))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem", containsString("estorno")));
    assertThat(jdbc.queryForMap("select * from despesa where diaria_id=?", paga)).isEqualTo(antes);
    assertThat(
            jdbc.queryForObject(
                "select conteudo::text from emissao_relatorio where id=?", String.class, emissao))
        .isEqualTo(snapshot);
    confereRateio(antes.get("id"), "90");
    enviar(put("/api/v1/diarias/" + paga), corpo("PAGA", "1", "90", "2024-04-01"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(delete("/api/v1/diarias/" + paga)).andExpect(status().isUnprocessableEntity());
    assertThat(jdbc.queryForMap("select * from despesa where diaria_id=?", paga)).isEqualTo(antes);
    enviar(put("/api/v1/diarias/" + paga), corpo("PAGA", "3", "85.50", "2024-03-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorTotal").value(256.5));
    assertThat(
            jdbc.queryForObject(
                "select conteudo::text from emissao_relatorio where id=?", String.class, emissao))
        .isEqualTo(snapshot);
    confereRateio(antes.get("id"), "256.50");
    Number fora =
        JsonPath.read(
            enviar(
                    post("/api/v1/plantios/" + plantio + "/diarias"),
                    corpo("PAGA", "1", "90", "2024-04-01"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    mvc.perform(delete("/api/v1/diarias/" + fora)).andExpect(status().isNoContent());
  }

  @Test
  void deveResponder404QuandoPlantioOuDiariaNaoExistir() throws Exception {
    mvc.perform(get("/api/v1/plantios/999999999/diarias")).andExpect(status().isNotFound());
    enviar(post("/api/v1/plantios/999999999/diarias"), corpo("PAGA", "1", "90", "2024-03-10"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/diarias/999999999")).andExpect(status().isNotFound());
    enviar(put("/api/v1/diarias/999999999"), corpo("PAGA", "1", "90", "2024-03-10"))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/diarias/999999999")).andExpect(status().isNotFound());
  }
}
