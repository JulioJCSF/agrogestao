package com.agrogestao.api.controller.despesa;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.RelogioAjustavel;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Massa da Q00b por SQL. Sem {@code @Transactional}: o teste atômico precisa de commit real e o
 * CT018 precisa que o GET leia do banco, não do contexto de persistência. Limpeza no
 * {@code @AfterEach}.
 */
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class DespesaIT extends IntegracaoTest {
  static final String CATEGORIA_SISTEMA =
      "A categoria Diária é lançada automaticamente pela diária paga e não pode ser usada em"
          + " despesa manual.";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  Long autor, produtor, propriedade, vizinho, propriedadeVizinho, milho, adubo, diaria;

  @BeforeEach
  void criarLancador() {
    new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
  }

  @BeforeEach
  void preparar() {
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
    autor =
        jdbc.queryForObject(
            "insert into usuario(nome,login,senha_hash,perfil) values ('Autor Teste Despesa','autor-despesa-it','hash-teste','LANCAMENTO') returning id",
            Long.class);
    produtor = produtor("Produtor Teste Despesa", "99900001745");
    propriedade = propriedade(produtor);
    vizinho = produtor("Produtor Teste Vizinho Despesa", "99900002040");
    propriedadeVizinho = propriedade(vizinho);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    diaria =
        jdbc.queryForObject("select id from categoria_despesa where sistema limit 1", Long.class);
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    List<Long> produtores = List.of(produtor, vizinho);
    for (String sql :
        List.of(
            "delete from despesa where produtor_id in (?,?)",
            "delete from plantio where propriedade_id in (select id from propriedade where produtor_id in (?,?))",
            "delete from propriedade where produtor_id in (?,?)",
            "delete from produtor where id in (?,?)")) jdbc.update(sql, produtores.toArray());
    jdbc.update("delete from usuario where id = ?", autor);
    jdbc.update("delete from usuario where login = ?", UsuariosTeste.LANCAMENTO);
  }

  Long produtor(String nome, String cpf) {
    return jdbc.queryForObject(
        "insert into produtor(nome,documento,criado_por) values (?,?,?) returning id",
        Long.class,
        nome,
        cpf,
        autor);
  }

  Long propriedade(Long dono) {
    return jdbc.queryForObject(
        "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
        Long.class,
        dono,
        autor);
  }

  Long plantio(Long naPropriedade, String area, LocalDate encerramento) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,data_encerramento,criado_por) values (?,?,?,'CURTO','2026-02-01',?,?) returning id",
        Long.class,
        naPropriedade,
        milho,
        new BigDecimal(area),
        encerramento,
        autor);
  }

  Long plantio(String area) {
    return plantio(propriedade, area, null);
  }

  static String corpo(
      Long produtorId,
      Long categoria,
      String descricao,
      String valor,
      String dataFato,
      Object... plantioIds) {
    return "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"%s\",\"valorTotal\":%s,\"dataFato\":\"%s\",\"plantioIds\":[%s]}"
        .formatted(
            produtorId,
            categoria,
            descricao,
            valor,
            dataFato,
            Arrays.stream(plantioIds).map(String::valueOf).collect(Collectors.joining(",")));
  }

  String corpo(String valor, Object... plantioIds) {
    return corpo(produtor, adubo, "Adubo de teste", valor, "2026-03-10", plantioIds);
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String json) throws Exception {
    return mvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(json));
  }

  Long lancar(String valor, Object... plantioIds) throws Exception {
    String resposta =
        enviar(post("/api/v1/despesas"), corpo(valor, plantioIds))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(resposta, "$.id")).longValue();
  }

  /** plantio_id, percentual, valor_rateado, atualizado_em e id de cada rateio, por plantio. */
  List<Map<String, Object>> rateios(Long despesa) {
    return jdbc.queryForList(
        "select id,plantio_id,percentual,valor_rateado,atualizado_em from rateio_despesa where despesa_id=? order by plantio_id",
        despesa);
  }

  static void confere(Map<String, Object> linha, Long plantio, String percentual, String valor) {
    assertThat(((Number) linha.get("plantio_id")).longValue()).isEqualTo(plantio);
    assertThat((BigDecimal) linha.get("percentual")).isEqualByComparingTo(percentual);
    assertThat((BigDecimal) linha.get("valor_rateado")).isEqualByComparingTo(valor);
  }

  static List<Object> atualizacoes(List<Map<String, Object>> linhas) {
    return linhas.stream().map(l -> l.get("atualizado_em")).toList();
  }

  void alterarArea(Long plantio, String area) throws Exception {
    enviar(
            put("/api/v1/plantios/" + plantio),
            "{\"propriedadeId\":%d,\"culturaId\":%d,\"area\":%s,\"dataInicio\":\"2026-02-01\"}"
                .formatted(propriedade, milho, area))
        .andExpect(status().isOk());
  }

  int despesasDoProdutor() {
    return jdbc.queryForObject(
        "select count(*) from despesa where produtor_id in (?,?)",
        Integer.class,
        produtor,
        vizinho);
  }

  int rateiosDoProdutor() {
    return jdbc.queryForObject(
        "select count(*) from rateio_despesa r join despesa d on d.id = r.despesa_id where d.produtor_id in (?,?)",
        Integer.class,
        produtor,
        vizinho);
  }

  // CT009 (RN05, P1)
  @Test
  void deveManterRateioGravadoQuandoAreaDoPlantioMudar() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("2.000");
    Long d1 = lancar("90.00", p1, p2);
    List<Map<String, Object>> antes = rateios(d1);

    alterarArea(p2, "0.500");

    List<Map<String, Object>> depois = rateios(d1);
    confere(depois.get(0), p1, "0.333333", "30.00");
    confere(depois.get(1), p2, "0.666667", "60.00");
    assertThat(atualizacoes(depois)).isEqualTo(atualizacoes(antes));

    Long d2 = lancar("90.00", p1, p2);
    List<Map<String, Object>> novos = rateios(d2);
    confere(novos.get(0), p1, "0.666667", "60.00");
    confere(novos.get(1), p2, "0.333333", "30.00");
  }

  // CT018 (RN10, P1). A L04b repete o passo 4 com GET /api/v1/plantios/{P2}/despesas.
  @Test
  void deveDevolverValorGravadoQuandoRateioForAlteradoPorSql() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("1.000"), p3 = plantio("1.000");
    Long d1 = lancar("100.00", p1, p2, p3);
    List<Map<String, Object>> linhas = rateios(d1);
    assertThat(linhas).hasSize(3);
    confere(linhas.get(0), p1, "0.333333", "33.34");
    confere(linhas.get(1), p2, "0.333333", "33.33");
    confere(linhas.get(2), p3, "0.333333", "33.33");

    jdbc.update(
        "update rateio_despesa set valor_rateado = 40.00 where despesa_id = ? and plantio_id = ?",
        d1,
        p2);

    mvc.perform(get("/api/v1/despesas/" + d1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rateios[1].plantioId").value(p2))
        .andExpect(jsonPath("$.rateios[1].valorRateado").value(40.0));
  }

  @Test
  void deveResponder201ComLocationQuandoDespesaForValida() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("1.000"), p3 = plantio("1.000");
    MvcResult resultado =
        enviar(post("/api/v1/despesas"), corpo("100.00", p3, p1, p2))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dataColeta").value("2026-10-11"))
            .andExpect(jsonPath("$.estornada").value(false))
            .andExpect(jsonPath("$.diariaId").value(nullValue()))
            .andExpect(jsonPath("$.produtorId").value(produtor))
            .andExpect(
                jsonPath(
                    "$.rateios[*].plantioId",
                    contains(p1.intValue(), p2.intValue(), p3.intValue())))
            .andExpect(jsonPath("$.rateios[0].valorRateado").value(33.34))
            .andReturn();
    Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
    assertThat(resultado.getResponse().getHeader("Location")).endsWith("/api/v1/despesas/" + id);
    assertThat(
            jdbc.queryForObject(
                "select data_coleta from despesa where id = ?", LocalDate.class, id))
        .isEqualTo(LocalDate.of(2026, 10, 11));
  }

  /**
   * A área 0,001 contra 9.999 ha dá percentual 0,000000 e viola ck_rateio_percentual no INSERT do
   * rateio, depois do INSERT da despesa. Nada pode sobrar.
   */
  @Test
  void deveDesfazerTudoQuandoFalharDepoisDeGravarDespesa() throws Exception {
    Long grande = plantio("9999.000"), minusculo = plantio("0.001");
    enviar(post("/api/v1/despesas"), corpo("10.00", grande, minusculo))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "Não foi possível salvar: os dados conflitam com registros já cadastrados."));
    assertThat(despesasDoProdutor()).isZero();
    assertThat(rateiosDoProdutor()).isZero();
  }

  @Test
  void deveResponderStatusDaTabelaQuandoValidacaoFalhar() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("1.000"), p3 = plantio("1.000");
    Long deOutro = plantio(propriedadeVizinho, "1.000", null);
    enviar(
            post("/api/v1/despesas"),
            corpo(produtor, adubo, "Adubo de teste", "null", "2026-03-10", p1))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[*].campo", hasItem("valorTotal")));
    enviar(
            post("/api/v1/despesas"),
            corpo(produtor, adubo, "Adubo de teste", "10.001", "2026-03-10", p1))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", hasItem("valorTotal")));

    Map<String, String> regras = new LinkedHashMap<>();
    regras.put(corpo("0.00", p1), "O valor da despesa deve ser maior que zero.");
    regras.put(corpo("90.00"), "A despesa precisa estar vinculada a pelo menos um plantio.");
    regras.put(corpo("90.00", p1, p1), "O mesmo plantio foi informado mais de uma vez.");
    regras.put(
        corpo(produtor, diaria, "Diária manual", "90.00", "2026-03-10", p1), CATEGORIA_SISTEMA);
    regras.put(
        corpo("90.00", p1, deOutro),
        "Todos os plantios da despesa devem ser do produtor informado.");
    regras.put(
        corpo("0.02", p1, p2, p3),
        "O valor da despesa não cobre um centavo por plantio. Vincule menos plantios.");
    regras.put(
        corpo(produtor, adubo, "Adubo de teste", "90.00", "2026-10-12", p1),
        "A data do fato não pode ser posterior a hoje.");
    for (var regra : regras.entrySet())
      enviar(post("/api/v1/despesas"), regra.getKey())
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
          .andExpect(jsonPath("$.mensagem").value(regra.getValue()));

    Map<String, String> inexistentes = new LinkedHashMap<>();
    inexistentes.put(
        corpo(999_999_999L, adubo, "Adubo de teste", "90.00", "2026-03-10", p1),
        "Produtor não encontrado.");
    inexistentes.put(
        corpo(produtor, 999_999_999L, "Adubo de teste", "90.00", "2026-03-10", p1),
        "Categoria de despesa não encontrada.");
    inexistentes.put(corpo("90.00", p1, 999_999_999L), "Plantio não encontrado.");
    for (var caso : inexistentes.entrySet())
      enviar(post("/api/v1/despesas"), caso.getKey())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
          .andExpect(jsonPath("$.mensagem").value(caso.getValue()));

    assertThat(despesasDoProdutor()).isZero();
  }

  @Test
  void deveRejeitarSemGravarQuandoDataDoFatoForPosteriorAoEncerramento() throws Exception {
    Long encerrado = plantio(propriedade, "1.000", LocalDate.of(2026, 6, 30));
    Long aberto = plantio("1.000");
    enviar(
            post("/api/v1/despesas"),
            corpo(produtor, adubo, "Adubo de teste", "90.00", "2026-07-01", aberto, encerrado))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "O plantio foi encerrado em 30/06/2026. A data do fato não pode ser posterior ao encerramento."));
    assertThat(despesasDoProdutor()).isZero();
    assertThat(rateiosDoProdutor()).isZero();
  }

  // PUT 1: protege a RN05 na alteração.
  @Test
  void deveManterRateioGravadoQuandoAlteracaoMudarSoADescricao() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("2.000");
    Long d1 = lancar("90.00", p1, p2);
    List<Map<String, Object>> antes = rateios(d1);
    alterarArea(p2, "0.500");

    enviar(
            put("/api/v1/despesas/" + d1),
            corpo(produtor, adubo, "Descrição corrigida", "90.00", "2026-03-10", p1, p2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.descricao").value("Descrição corrigida"))
        .andExpect(jsonPath("$.rateios[0].valorRateado").value(30.0))
        .andExpect(jsonPath("$.rateios[1].valorRateado").value(60.0));

    List<Map<String, Object>> depois = rateios(d1);
    confere(depois.get(0), p1, "0.333333", "30.00");
    confere(depois.get(1), p2, "0.666667", "60.00");
    assertThat(depois).isEqualTo(antes);
  }

  /**
   * PUT 2. R$ 120,00 em P1 1,000 + P2 0,500 (Python decimal): 80,00 / 40,00, 0,666667 / 0,333333.
   */
  @Test
  void deveRecalcularComAreasAtuaisQuandoAlteracaoMudarOValor() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("2.000");
    Long d1 = lancar("90.00", p1, p2);
    List<Map<String, Object>> antes = rateios(d1);
    alterarArea(p2, "0.500");
    relogio.fixar(OffsetDateTime.parse("2026-10-20T12:00:00-03:00").toInstant());

    enviar(put("/api/v1/despesas/" + d1), corpo("120.00", p1, p2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorTotal").value(120.0))
        .andExpect(jsonPath("$.dataColeta").value("2026-10-11"));

    List<Map<String, Object>> depois = rateios(d1);
    confere(depois.get(0), p1, "0.666667", "80.00");
    confere(depois.get(1), p2, "0.333333", "40.00");
    assertThat(depois)
        .extracting(l -> l.get("id"))
        .isEqualTo(antes.stream().map(l -> l.get("id")).toList());
    assertThat(
            jdbc.queryForObject(
                "select data_coleta from despesa where id = ?", LocalDate.class, d1))
        .isEqualTo(LocalDate.of(2026, 10, 11));
  }

  /**
   * PUT 3. R$ 90,00 em P2 2,000 + P3 1,000 (Python decimal): 60,00 / 30,00, 0,666667 / 0,333333.
   */
  @Test
  void deveTrocarLinhasQuandoAlteracaoMudarOsPlantios() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("2.000"), p3 = plantio("1.000");
    Long d1 = lancar("90.00", p1, p2);
    Object linhaP2 = rateios(d1).get(1).get("id");

    enviar(put("/api/v1/despesas/" + d1), corpo("90.00", p2, p3)).andExpect(status().isOk());

    List<Map<String, Object>> depois = rateios(d1);
    assertThat(depois).hasSize(2);
    confere(depois.get(0), p2, "0.666667", "60.00");
    confere(depois.get(1), p3, "0.333333", "30.00");
    assertThat(depois.get(0).get("id")).isEqualTo(linhaP2);
    assertThat(
            depois.stream()
                .map(l -> (BigDecimal) l.get("valor_rateado"))
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("90.00");
  }

  // PUT 4: a ordem de plantioIds não é mudança.
  @Test
  void deveManterRateioGravadoQuandoAlteracaoSoInverterAOrdemDosPlantios() throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("2.000");
    Long d1 = lancar("90.00", p1, p2);
    List<Map<String, Object>> antes = rateios(d1);
    alterarArea(p2, "0.500");

    enviar(put("/api/v1/despesas/" + d1), corpo("90.00", p2, p1)).andExpect(status().isOk());

    assertThat(rateios(d1)).isEqualTo(antes);
  }

  @Test
  void deveResponder404QuandoDespesaNaoExistir() throws Exception {
    Long p1 = plantio("1.000");
    enviar(put("/api/v1/despesas/999999999"), corpo("90.00", p1))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
    mvc.perform(get("/api/v1/despesas/999999999")).andExpect(status().isNotFound());
  }
}
