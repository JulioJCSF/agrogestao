package com.agrogestao.api.controller.relatorio;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A03a contra o banco, pela API com token real (F04b): CT050 (números), CT051 e CT052 (parte de
 * integração, Q07), CT053 e CT054 da Q00f, mais as decisões de 03/11. Produtor, propriedade e
 * plantio entram por SQL, como nos outros *IT; despesa, diária, produção e venda pelas APIs.
 *
 * <p>O relógio fica fixo em 03/11/2026: todas as datas do fato são passadas e a data de emissão é
 * conhecida. Sem {@code @Transactional}: limpeza no {@code @AfterEach}.
 */
class EmissaoRelatorioIT extends IntegracaoTest {
  static final String VALOR_DECLARADO =
      "Valores declarados pelo produtor, não comprovados por documento fiscal.";
  static final String FINALIDADE =
      "Este documento não constitui garantia de aceitação para qualquer finalidade externa.";
  static final Instant AGORA = Instant.parse("2026-11-03T15:00:00Z");
  static final String SEMESTRE = "\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\"";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, produtor, milho, hortalica, sementes, adubo, cliente, referencia;
  List<Long> produtores = new ArrayList<>();
  Map<Long, Long> propriedades = new HashMap<>();

  @BeforeEach
  void preparar() throws Exception {
    relogio.fixar(AGORA);
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    usuarios.criar(Perfil.CONSULTA);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    produtor = produtor("Produtor Teste Relatório", "99900001311", "2026-01-15");
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    hortalica = jdbc.queryForObject("select id from cultura where nome = 'Hortaliça'", Long.class);
    sementes =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Sementes e mudas'", Long.class);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Relatório','FEIRA',?) returning id",
            Long.class,
            autor);
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    for (Long id : produtores) {
      jdbc.update("delete from emissao_relatorio where produtor_id = ?", id);
      jdbc.update("delete from venda where produtor_id = ?", id);
      jdbc.update("delete from despesa where produtor_id = ?", id);
      String doProdutor =
          "plantio_id in (select p.id from plantio p join propriedade r on r.id = p.propriedade_id where r.produtor_id = ?)";
      jdbc.update("delete from producao where " + doProdutor, id);
      jdbc.update("delete from diaria where " + doProdutor, id);
      jdbc.update(
          "delete from plantio where propriedade_id in (select id from propriedade where produtor_id = ?)",
          id);
      jdbc.update("delete from propriedade where produtor_id = ?", id);
      jdbc.update("delete from produtor where id = ?", id);
    }
    if (referencia != null)
      jdbc.update("delete from valor_referencia_diaria where id = ?", referencia);
    jdbc.update("delete from cliente where id = ?", cliente);
    for (Perfil perfil : new Perfil[] {Perfil.LANCAMENTO, Perfil.CONSULTA})
      jdbc.update("delete from usuario where login = ?", UsuariosTeste.login(perfil));
  }

  // --- massa ------------------------------------------------------------------------------------

  /** Produtor da Q00f com a propriedade "Sítio Teste", 10,000 ha. */
  Long produtor(String nome, String cpf, String consentimento) {
    Long id =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,endereco,profissao,contato,consentimento_em,criado_por) values (?,?,'Endereço Fictício Teste, zona rural','Agricultor','Contato Fictício Teste',?::date,?) returning id",
            Long.class,
            nome,
            cpf,
            consentimento,
            autor);
    produtores.add(id);
    propriedades.put(
        id,
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            id,
            autor));
    return id;
  }

  Long plantio(
      Long doProdutor, Long cultura, String ciclo, String area, String inicio, String fim) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,data_encerramento,criado_por) values (?,?,?::numeric,?,?::date,?::date,?) returning id",
        Long.class,
        propriedades.get(doProdutor),
        cultura,
        area,
        ciclo,
        inicio,
        fim,
        autor);
  }

  Long plantio(Long cultura, String ciclo, String area) {
    return plantio(produtor, cultura, ciclo, area, "2026-02-01", null);
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
    return mvc.perform(
        requisicao
            .header(HttpHeaders.AUTHORIZATION, lancamento)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  static long id(String corpo) {
    return ((Number) JsonPath.read(corpo, "$.id")).longValue();
  }

  static String corpo(ResultActions resultado) throws Exception {
    return resultado.andReturn().getResponse().getContentAsString();
  }

  Long despesa(Long categoria, String valor, String data, Long... plantios) throws Exception {
    return id(
        corpo(
            enviar(post("/api/v1/despesas"), despesaJson(categoria, valor, data, plantios))
                .andExpect(status().isCreated())));
  }

  String despesaJson(Long categoria, String valor, String data, Long... plantios) {
    return "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Relatório\",\"valorTotal\":%s,\"dataFato\":\"%s\",\"plantioIds\":[%s]}"
        .formatted(
            produtor,
            categoria,
            valor,
            data,
            Arrays.stream(plantios).map(String::valueOf).collect(Collectors.joining(",")));
  }

  void diaria(Long plantio, String natureza, String dias, String valor, String data)
      throws Exception {
    enviar(
            post("/api/v1/plantios/" + plantio + "/diarias"),
            "{\"natureza\":\"%s\",\"quantidadeDias\":%s,\"valorUnitario\":%s,\"dataFato\":\"%s\"}"
                .formatted(natureza, dias, valor, data))
        .andExpect(status().isCreated());
  }

  void producao(Long plantio, String quantidade, String data) throws Exception {
    enviar(
            post("/api/v1/plantios/" + plantio + "/producoes"),
            "{\"quantidade\":%s,\"unidadeMedida\":\"kg\",\"dataFato\":\"%s\"}"
                .formatted(quantidade, data))
        .andExpect(status().isCreated());
  }

  void venda(Long plantio, String quantidade, String valor, String data) throws Exception {
    enviar(
            post("/api/v1/vendas"),
            "{\"produtorId\":%d,\"clienteId\":%d,\"dataFato\":\"%s\",\"itens\":[{\"plantioId\":%d,\"quantidade\":%s,\"unidadeMedida\":\"kg\",\"valorUnitario\":%s}]}"
                .formatted(produtor, cliente, data, plantio, quantidade, valor))
        .andExpect(status().isCreated());
  }

  /** Massa do CT053: P1 (Milho, aberto), D1 de R$ 300,00 e venda de R$ 500,00. Devolve D1. */
  Long massaCt053() throws Exception {
    Long p1 = plantio(milho, "LONGO", "1.000");
    Long d1 = despesa(adubo, "300.00", "2026-03-10", p1);
    venda(p1, "100.000", "5.00", "2026-03-20");
    return d1;
  }

  ResultActions emitir(Long doProdutor, String periodo) throws Exception {
    return enviar(
        post("/api/v1/relatorios"), "{\"produtorId\":%d,%s}".formatted(doProdutor, periodo));
  }

  String emitirComSucesso(Long doProdutor, String inicio, String fim) throws Exception {
    return corpo(
        emitir(doProdutor, "\"periodoInicio\":\"%s\",\"periodoFim\":\"%s\"".formatted(inicio, fim))
            .andExpect(status().isCreated()));
  }

  String semestre() throws Exception {
    return emitirComSucesso(produtor, "2026-01-01", "2026-06-30");
  }

  String detalhe(long emissao) throws Exception {
    return corpo(
        mvc.perform(
                get("/api/v1/relatorios/" + emissao).header(HttpHeaders.AUTHORIZATION, lancamento))
            .andExpect(status().isOk()));
  }

  static BigDecimal dinheiro(String json, String caminho) {
    return new BigDecimal(JsonPath.read(json, caminho).toString());
  }

  /** As colunas que o CT053 compara byte a byte, como texto. */
  Map<String, Object> linha(long emissao) {
    return jdbc.queryForMap(
        "select conteudo::text as conteudo, data_emissao::text as data_emissao, periodo_inicio::text as periodo_inicio, periodo_fim::text as periodo_fim, usuario_id, atualizado_em::text as atualizado_em from emissao_relatorio where id = ?",
        emissao);
  }

  int emissoes(Long doProdutor) {
    return jdbc.queryForObject(
        "select count(*) from emissao_relatorio where produtor_id = ?", Integer.class, doProdutor);
  }

  // --- CT050 ------------------------------------------------------------------------------------

  @Test
  void deveTrazerOsNumerosDoCt050QuandoEmitirOSemestre() throws Exception {
    // A diária familiar usa a referência de 80,00 vigente em 15/03/2026 (migration); se outro *IT
    // a apagou, cria uma só para março.
    if (jdbc.queryForObject(
            "select count(*) from valor_referencia_diaria where vigencia_inicio <= '2026-03-15' and (vigencia_fim is null or vigencia_fim >= '2026-03-15')",
            Integer.class)
        == 0)
      referencia =
          jdbc.queryForObject(
              "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) values (80.00,'2026-03-01','2026-03-31',?) returning id",
              Long.class,
              autor);
    Long pm = plantio(milho, "LONGO", "1.000");
    Long ph = plantio(hortalica, "CURTO", "0.500");
    Long sementes450 = despesa(sementes, "450.00", "2026-02-08", pm);
    Long adubo100 = despesa(adubo, "100.00", "2026-03-10", pm, ph);
    diaria(pm, "PAGA", "2.00", "90.00", "2026-03-12");
    diaria(pm, "FAMILIAR", "1.00", "null", "2026-03-15");
    producao(pm, "500.000", "2026-06-20");
    producao(ph, "30.000", "2026-04-10");
    venda(pm, "200.000", "4.50", "2026-06-25");
    venda(ph, "30.000", "3.00", "2026-04-12");
    despesa(sementes, "40.00", "2026-07-05", pm);

    ResultActions resposta =
        emitir(produtor, SEMESTRE)
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", matchesPattern(".*/api/v1/relatorios/\\d+$")))
            .andExpect(jsonPath("$.produtorId").value(produtor))
            .andExpect(jsonPath("$.usuarioId").value(autor))
            .andExpect(jsonPath("$.dataEmissao").value("2026-11-03T15:00:00Z"))
            .andExpect(jsonPath("$.periodoInicio").value("2026-01-01"))
            .andExpect(jsonPath("$.periodoFim").value("2026-06-30"))
            .andExpect(jsonPath("$.conteudo.produtor.nome").value("Produtor Teste Relatório"))
            .andExpect(jsonPath("$.conteudo.produtor.documento").value("99900001311"))
            .andExpect(
                jsonPath("$.conteudo.produtor.endereco")
                    .value("Endereço Fictício Teste, zona rural"))
            .andExpect(jsonPath("$.conteudo.produtor.profissao").value("Agricultor"))
            .andExpect(jsonPath("$.conteudo.propriedades[*].nome", contains("Sítio Teste")))
            .andExpect(
                jsonPath(
                    "$.conteudo.plantios[*].plantioId", contains(pm.intValue(), ph.intValue())))
            .andExpect(
                jsonPath("$.conteudo.plantios[*].culturaNome", contains("Milho", "Hortaliça")))
            .andExpect(jsonPath("$.conteudo.plantios[1].periodoInicio").value("2026-01-01"))
            .andExpect(jsonPath("$.conteudo.plantios[1].periodoFim").value("2026-06-30"))
            .andExpect(jsonPath("$.conteudo.despesas", hasSize(3)))
            .andExpect(jsonPath("$.conteudo.diarias", hasSize(2)))
            .andExpect(jsonPath("$.conteudo.diarias[*].natureza", contains("PAGA", "FAMILIAR")));
    String json = corpo(resposta);
    String c = "$.conteudo.";
    // Defeitos: 770.00 (entrou a despesa de 05/07) e 910.00 (diária paga somada por fora, RN20).
    assertThat(dinheiro(json, c + "custoDesembolso")).isEqualByComparingTo("730.00");
    assertThat(dinheiro(json, c + "receita")).isEqualByComparingTo("990.00");
    assertThat(dinheiro(json, c + "valorMaoObraFamiliar")).isEqualByComparingTo("80.00");
    assertThat(dinheiro(json, c + "resultado")).isEqualByComparingTo("260.00");
    assertThat(dinheiro(json, c + "plantios[0].resultado")).isEqualByComparingTo("203.33");
    assertThat(dinheiro(json, c + "plantios[1].resultado")).isEqualByComparingTo("56.67");
    // RN25: produzido e vendido separados.
    assertThat(dinheiro(json, c + "plantios[0].quantidadeProduzida")).isEqualByComparingTo("500");
    assertThat(dinheiro(json, c + "plantios[0].quantidadeVendida")).isEqualByComparingTo("200");
    assertThat(dinheiro(json, c + "plantios[1].quantidadeProduzida")).isEqualByComparingTo("30");
    assertThat(dinheiro(json, c + "plantios[1].quantidadeVendida")).isEqualByComparingTo("30");
    assertThat(dinheiro(json, c + "despesas[0].valorTotal")).isEqualByComparingTo("450.00");
    assertThat(dinheiro(json, c + "despesas[1].valorTotal")).isEqualByComparingTo("100.00");
    assertThat(dinheiro(json, c + "despesas[2].valorTotal")).isEqualByComparingTo("180.00");
    assertThat(((Number) JsonPath.read(json, c + "despesas[0].id")).longValue())
        .isEqualTo(sementes450);
    assertThat(((Number) JsonPath.read(json, c + "despesas[1].id")).longValue())
        .isEqualTo(adubo100);
    assertThat(dinheiro(json, c + "diarias[0].valorTotal")).isEqualByComparingTo("180.00");
    assertThat(dinheiro(json, c + "diarias[1].valorTotal")).isEqualByComparingTo("80.00");

    // Consistência interna: o rateio das despesas listadas aos plantios listados é o custo.
    List<Number> plantiosListados = JsonPath.read(json, c + "plantios[*].plantioId");
    Set<Long> listados =
        plantiosListados.stream().map(Number::longValue).collect(Collectors.toSet());
    List<Map<String, Object>> rateios = JsonPath.read(json, c + "despesas[*].rateios[*]");
    BigDecimal rateado =
        rateios.stream()
            .filter(r -> listados.contains(((Number) r.get("plantioId")).longValue()))
            .map(r -> new BigDecimal(r.get("valorRateado").toString()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(rateado).isEqualByComparingTo(dinheiro(json, c + "custoDesembolso"));

    // Gravado com a escala de dinheiro.
    assertThat(
            jdbc.queryForObject(
                "select conteudo->>'custoDesembolso' from emissao_relatorio where id = ?",
                String.class,
                id(json)))
        .isEqualTo("730.00");
  }

  // --- CT051 e CT052 ----------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveGravarAsDuasRessalvasQuandoEmitirComESemLancamentosCt051Ct052() throws Exception {
    Long pl = plantio(milho, "LONGO", "1.000");
    despesa(adubo, "300.00", "2026-03-10", pl);
    for (String[] periodo :
        new String[][] {{"2026-01-01", "2026-06-30"}, {"2026-09-01", "2026-09-30"}}) {
      String emitida = emitirComSucesso(produtor, periodo[0], periodo[1]);
      long emissao = id(emitida);
      String lida = detalhe(emissao);
      String quando = " (" + periodo[0] + " a " + periodo[1] + ")";
      for (String json : List.of(emitida, lida)) {
        assertThat((String) JsonPath.read(json, "$.conteudo.ressalvaValorDeclarado"))
            .as("CT051, ressalva de valor declarado na resposta" + quando)
            .isEqualTo(VALOR_DECLARADO);
        assertThat((String) JsonPath.read(json, "$.conteudo.ressalvaFinalidade"))
            .as("CT052, ressalva de finalidade na resposta" + quando)
            .isEqualTo(FINALIDADE);
      }
      assertThat(
              jdbc.queryForObject(
                  "select conteudo->>'ressalvaValorDeclarado' from emissao_relatorio where id = ?",
                  String.class,
                  emissao))
          .as("CT051, ressalva de valor declarado gravada" + quando)
          .isEqualTo(VALOR_DECLARADO);
      assertThat(
              jdbc.queryForObject(
                  "select conteudo->>'ressalvaFinalidade' from emissao_relatorio where id = ?",
                  String.class,
                  emissao))
          .as("CT052, ressalva de finalidade gravada" + quando)
          .isEqualTo(FINALIDADE);
    }
  }

  // --- CT053 ------------------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void devePreservarEmissoesAnterioresQuandoReemitirCt053() throws Exception {
    Long d1 = massaCt053();
    Long p1 =
        jdbc.queryForObject(
            "select plantio_id from rateio_despesa where despesa_id = ?", Long.class, d1);

    String e1 = semestre();
    assertThat(dinheiro(e1, "$.conteudo.custoDesembolso")).isEqualByComparingTo("300.00");
    assertThat(dinheiro(e1, "$.conteudo.receita")).isEqualByComparingTo("500.00");
    assertThat(dinheiro(e1, "$.conteudo.resultado")).isEqualByComparingTo("200.00");
    Map<String, Object> linhaE1 = linha(id(e1));

    enviar(put("/api/v1/despesas/" + d1), despesaJson(adubo, "350.00", "2026-03-10", p1))
        .andExpect(status().isOk());

    String e2 = semestre();
    assertThat(id(e2)).isNotEqualTo(id(e1));
    assertThat(dinheiro(e2, "$.conteudo.custoDesembolso")).isEqualByComparingTo("350.00");
    assertThat(dinheiro(e2, "$.conteudo.resultado")).isEqualByComparingTo("150.00");
    Map<String, Object> linhaE2 = linha(id(e2));

    assertThat(linha(id(e1))).isEqualTo(linhaE1);
    mvc.perform(
            get("/api/v1/produtores/" + produtor + "/relatorios")
                .header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", contains((int) id(e2), (int) id(e1))));

    enviar(post("/api/v1/despesas/" + d1 + "/estorno"), "")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estornada").value(true));
    String e3 = semestre();
    assertThat(dinheiro(e3, "$.conteudo.custoDesembolso")).isEqualByComparingTo("0.00");
    assertThat(dinheiro(e3, "$.conteudo.resultado")).isEqualByComparingTo("500.00");
    assertThat((List<?>) JsonPath.read(e3, "$.conteudo.despesas")).isEmpty();

    assertThat(linha(id(e1))).isEqualTo(linhaE1);
    assertThat(linha(id(e2))).isEqualTo(linhaE2);
    assertThat(dinheiro(detalhe(id(e1)), "$.conteudo.custoDesembolso"))
        .isEqualByComparingTo("300.00");
    assertThat(dinheiro(detalhe(id(e2)), "$.conteudo.custoDesembolso"))
        .isEqualByComparingTo("350.00");
  }

  // --- CT054 ------------------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveRecusarAlterarApagarOuMontarEmissaoQuandoClienteTentarCt054() throws Exception {
    massaCt053();
    long e1 = id(semestre());
    Map<String, Object> linhaE1 = linha(e1);

    enviar(put("/api/v1/relatorios/" + e1), "{\"conteudo\":{\"resultado\":99999.99}}")
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
    mvc.perform(delete("/api/v1/relatorios/" + e1).header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));

    String montada =
        corpo(
            enviar(
                    post("/api/v1/relatorios"),
                    ("{\"produtorId\":%d,%s,\"dataEmissao\":\"2020-01-01T00:00:00Z\","
                            + "\"conteudo\":{\"resultado\":99999.99,\"ressalvaValorDeclarado\":\"\","
                            + "\"ressalvaFinalidade\":\"\"}}")
                        .formatted(produtor, SEMESTRE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataEmissao").value("2026-11-03T15:00:00Z"))
                .andExpect(jsonPath("$.conteudo.ressalvaValorDeclarado").value(VALOR_DECLARADO))
                .andExpect(jsonPath("$.conteudo.ressalvaFinalidade").value(FINALIDADE)));
    assertThat(dinheiro(montada, "$.conteudo.resultado")).isEqualByComparingTo("200.00");

    mvc.perform(
            delete("/api/v1/produtores/" + produtor).header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));

    assertThat(linha(e1)).isEqualTo(linhaE1);
    assertThat(emissoes(produtor)).isEqualTo(2);
  }

  // --- decisões de 03/11, consentimento e validação -------------------------------------------

  @Test
  void deveDeixarForaPlantiosInativosNoPeriodoQuandoEmitir() throws Exception {
    Long aberto = plantio(milho, "LONGO", "1.000");
    plantio(produtor, milho, "LONGO", "1.000", "2026-07-01", null);
    plantio(produtor, milho, "LONGO", "1.000", "2025-08-01", "2025-12-31");
    emitir(produtor, SEMESTRE)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.conteudo.plantios[*].plantioId", contains(aberto.intValue())));
  }

  @Test
  void deveEmitirComZerosEListasVaziasQuandoPeriodoNaoTiverLancamentos() throws Exception {
    String json =
        corpo(
            emitir(produtor, SEMESTRE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conteudo.plantios", hasSize(0)))
                .andExpect(jsonPath("$.conteudo.despesas", hasSize(0)))
                .andExpect(jsonPath("$.conteudo.diarias", hasSize(0)))
                .andExpect(jsonPath("$.conteudo.ressalvaValorDeclarado").value(VALOR_DECLARADO))
                .andExpect(jsonPath("$.conteudo.ressalvaFinalidade").value(FINALIDADE)));
    for (String campo : List.of("receita", "custoDesembolso", "resultado", "valorMaoObraFamiliar"))
      assertThat(
              jdbc.queryForObject(
                  "select conteudo->>? from emissao_relatorio where id = ?",
                  String.class,
                  campo,
                  id(json)))
          .as(campo)
          .isEqualTo("0.00");
  }

  @Test
  void deveBloquearSemGravarQuandoProdutorNaoTiverConsentimento() throws Exception {
    Long semConsentimento = produtor("Produtor Teste Sem Consentimento", "99900001400", null);
    emitir(semConsentimento, SEMESTRE)
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "O produtor ainda não tem consentimento registrado. Registre o consentimento"
                        + " antes de emitir o relatório."));
    assertThat(emissoes(semConsentimento)).isZero();

    jdbc.update(
        "update produtor set consentimento_em = '2026-10-01' where id = ?", semConsentimento);
    emitir(semConsentimento, SEMESTRE).andExpect(status().isCreated());
    assertThat(emissoes(semConsentimento)).isEqualTo(1);
  }

  @Test
  void deveValidarNaOrdemDoContratoQuandoEmissaoForInvalida() throws Exception {
    emitir(produtor, "\"periodoInicio\":\"2026-01-01\"")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[*].campo", hasItem("periodoFim")));
    emitir(0L, SEMESTRE)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", hasItem("produtorId")));
    // Período invertido vem antes do produtor inexistente.
    emitir(999_999_999L, "\"periodoInicio\":\"2026-06-30\",\"periodoFim\":\"2026-01-01\"")
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem").value("O fim do período não pode ser anterior ao início."));
    emitir(999_999_999L, SEMESTRE)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Produtor não encontrado."));
    assertThat(emissoes(produtor)).isZero();
  }

  // --- leitura ----------------------------------------------------------------------------------

  @Test
  void deveDevolverOConteudoGravadoSemReapurarQuandoLancamentoMudarPorSql() throws Exception {
    Long d1 = massaCt053();
    long e1 = id(semestre());
    // Sentinela: muda o lançamento por fora da API; uma leitura que reapurasse veria 999.99.
    jdbc.update("update rateio_despesa set valor_rateado = 999.99 where despesa_id = ?", d1);
    jdbc.update("update despesa set valor_total = 999.99 where id = ?", d1);
    String lida = detalhe(e1);
    assertThat(dinheiro(lida, "$.conteudo.custoDesembolso")).isEqualByComparingTo("300.00");
    assertThat(dinheiro(lida, "$.conteudo.despesas[0].valorTotal")).isEqualByComparingTo("300.00");
    assertThat(dinheiro(lida, "$.conteudo.despesas[0].rateios[0].valorRateado"))
        .isEqualByComparingTo("300.00");
    String lista =
        corpo(
            mvc.perform(
                    get("/api/v1/produtores/" + produtor + "/relatorios")
                        .header(HttpHeaders.AUTHORIZATION, lancamento))
                .andExpect(status().isOk()));
    assertThat(dinheiro(lista, "$.itens[0].conteudo.custoDesembolso"))
        .isEqualByComparingTo("300.00");
  }

  @Test
  void deveListarDaMaisRecenteParaAMaisAntigaQuandoConsultarOHistorico() throws Exception {
    Long outro = produtor("Produtor Teste Emissão", "99900001583", "2026-01-15");
    long e1 = id(semestre());
    long e2 = id(emitirComSucesso(produtor, "2026-09-01", "2026-09-30"));
    emitirComSucesso(outro, "2026-01-01", "2026-06-30");

    mvc.perform(
            get("/api/v1/produtores/" + produtor + "/relatorios")
                .header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", contains((int) e2, (int) e1)))
        .andExpect(jsonPath("$.itens[0].periodoInicio").value("2026-09-01"))
        .andExpect(jsonPath("$.itens[0].conteudo.ressalvaFinalidade").value(FINALIDADE));
    mvc.perform(
            get("/api/v1/produtores/" + produtor + "/relatorios")
                .param("pagina", "1")
                .param("tamanho", "1")
                .header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagina").value(1))
        .andExpect(jsonPath("$.tamanho").value(1))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", contains((int) e1)));
    mvc.perform(
            get("/api/v1/produtores/999999999/relatorios")
                .header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Produtor não encontrado."));
    mvc.perform(get("/api/v1/relatorios/999999999").header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.mensagem").value("Emissão de relatório não encontrada."));
  }

  @Test
  void deveNegarEmissaoEPermitirLeituraQuandoPerfilForConsulta() throws Exception {
    long e1 = id(semestre());
    String consulta = usuarios.bearer(Perfil.CONSULTA);
    mvc.perform(
            post("/api/v1/relatorios")
                .header(HttpHeaders.AUTHORIZATION, consulta)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtorId\":%d,%s}".formatted(produtor, SEMESTRE)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/relatorios/" + e1).header(HttpHeaders.AUTHORIZATION, consulta))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(e1));
    mvc.perform(
            get("/api/v1/produtores/" + produtor + "/relatorios")
                .header(HttpHeaders.AUTHORIZATION, consulta))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    assertThat(emissoes(produtor)).isEqualTo(1);
  }
}
