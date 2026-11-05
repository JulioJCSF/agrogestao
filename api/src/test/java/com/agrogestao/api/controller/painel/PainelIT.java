package com.agrogestao.api.controller.painel;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * M07 contra o banco, pela API com token real: os números do CT050 iguais aos do relatório, o
 * painel do produtor e o geral, o número de consultas que não cresce com os plantios e os erros.
 * Produtor, propriedade e plantio entram por SQL, como no {@code EmissaoRelatorioIT}; despesa,
 * diária, produção e venda pelas APIs.
 *
 * <p>O painel geral soma todos os plantios do banco, inclusive os de outros testes e os dados
 * iniciais; por isso, sem produtor, os casos conferem só a presença dos plantios daqui e que o
 * total é a soma da lista. Sem {@code @Transactional}: limpeza no {@code @AfterEach}.
 */
class PainelIT extends IntegracaoTest {
  static final Instant AGORA = Instant.parse("2026-11-03T15:00:00Z");
  static final String SEMESTRE = "periodoInicio=2026-01-01&periodoFim=2026-06-30";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  @Autowired EntityManagerFactory emf;
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
    produtor = produtor("Produtor Teste Painel", "99900001311", "2026-01-15");
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
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Painel','FEIRA',?) returning id",
            Long.class,
            autor);
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    for (Long id : produtores) {
      LimpezaTeste.apagarEmissoes(jdbc, id);
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

  Long plantio(Long doProdutor, Long cultura, String ciclo, String area) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,data_encerramento,criado_por) values (?,?,?::numeric,?,'2026-02-01'::date,null,?) returning id",
        Long.class,
        propriedades.get(doProdutor),
        cultura,
        area,
        ciclo,
        autor);
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
    return mvc.perform(
        requisicao
            .header(HttpHeaders.AUTHORIZATION, lancamento)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  static String corpo(ResultActions resultado) throws Exception {
    return resultado.andReturn().getResponse().getContentAsString();
  }

  static String ids(Collection<Long> plantios) {
    return plantios.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  void despesa(Long doProdutor, Long categoria, String valor, String data, List<Long> plantios)
      throws Exception {
    enviar(
            post("/api/v1/despesas"),
            "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Painel\",\"valorTotal\":%s,\"dataFato\":\"%s\",\"plantioIds\":[%s]}"
                .formatted(doProdutor, categoria, valor, data, ids(plantios)))
        .andExpect(status().isCreated());
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

  /** Uma venda com um item de cada plantio, todos com a mesma quantidade e o mesmo preço. */
  void venda(Long doProdutor, List<Long> plantios, String quantidade, String valor, String data)
      throws Exception {
    String itens =
        plantios.stream()
            .map(
                plantio ->
                    "{\"plantioId\":%d,\"quantidade\":%s,\"unidadeMedida\":\"kg\",\"valorUnitario\":%s}"
                        .formatted(plantio, quantidade, valor))
            .collect(Collectors.joining(","));
    enviar(
            post("/api/v1/vendas"),
            "{\"produtorId\":%d,\"clienteId\":%d,\"dataFato\":\"%s\",\"itens\":[%s]}"
                .formatted(doProdutor, cliente, data, itens))
        .andExpect(status().isCreated());
  }

  ResultActions painel(String token, String consulta) throws Exception {
    return mvc.perform(get("/api/v1/painel?" + consulta).header(HttpHeaders.AUTHORIZATION, token));
  }

  String painelComSucesso(String consulta) throws Exception {
    return corpo(painel(lancamento, consulta).andExpect(status().isOk()));
  }

  static BigDecimal dinheiro(String json, String caminho) {
    return new BigDecimal(JsonPath.read(json, caminho).toString());
  }

  static BigDecimal somaDaLista(String json, String campo) {
    List<Object> valores = JsonPath.read(json, "$.plantios[*]." + campo);
    return valores.stream()
        .map(valor -> new BigDecimal(valor.toString()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // --- CT050 ------------------------------------------------------------------------------------

  @Test
  void deveTrazerOsNumerosDoRelatorioQuandoConsultarOSemestreDoCt050() throws Exception {
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
    Long pm = plantio(produtor, milho, "LONGO", "1.000");
    Long ph = plantio(produtor, hortalica, "CURTO", "0.500");
    despesa(produtor, sementes, "450.00", "2026-02-08", List.of(pm));
    despesa(produtor, adubo, "100.00", "2026-03-10", List.of(pm, ph));
    diaria(pm, "PAGA", "2.00", "90.00", "2026-03-12");
    diaria(pm, "FAMILIAR", "1.00", "null", "2026-03-15");
    producao(pm, "500.000", "2026-06-20");
    producao(ph, "30.000", "2026-04-10");
    venda(produtor, List.of(pm), "200.000", "4.50", "2026-06-25");
    venda(produtor, List.of(ph), "30.000", "3.00", "2026-04-12");
    despesa(produtor, sementes, "40.00", "2026-07-05", List.of(pm));

    String painel =
        corpo(
            painel(lancamento, SEMESTRE + "&produtorId=" + produtor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoInicio").value("2026-01-01"))
                .andExpect(jsonPath("$.periodoFim").value("2026-06-30"))
                .andExpect(
                    jsonPath("$.plantios[*].plantioId", contains(pm.intValue(), ph.intValue())))
                .andExpect(jsonPath("$.plantios[*].culturaNome", contains("Milho", "Hortaliça"))));
    assertThat(dinheiro(painel, "$.custoDesembolso")).isEqualByComparingTo("730.00");
    assertThat(dinheiro(painel, "$.receita")).isEqualByComparingTo("990.00");
    assertThat(dinheiro(painel, "$.resultado")).isEqualByComparingTo("260.00");
    assertThat(dinheiro(painel, "$.plantios[0].resultado")).isEqualByComparingTo("203.33");
    assertThat(dinheiro(painel, "$.plantios[1].resultado")).isEqualByComparingTo("56.67");

    // Os mesmos números do relatório do mesmo período, campo a campo.
    String relatorio =
        corpo(
            enviar(
                    post("/api/v1/relatorios"),
                    "{\"produtorId\":%d,\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\"}"
                        .formatted(produtor))
                .andExpect(status().isCreated()));
    for (String campo : List.of("receita", "custoDesembolso", "resultado"))
      assertThat(dinheiro(painel, "$." + campo))
          .as(campo)
          .isEqualByComparingTo(dinheiro(relatorio, "$.conteudo." + campo));
    List<Map<String, Object>> doPainel = JsonPath.read(painel, "$.plantios");
    List<Map<String, Object>> doRelatorio = JsonPath.read(relatorio, "$.conteudo.plantios");
    assertThat(doPainel).hasSameSizeAs(doRelatorio);
    for (int i = 0; i < doPainel.size(); i++)
      for (String campo : doRelatorio.get(i).keySet())
        assertThat(doPainel.get(i).get(campo).toString())
            .as("plantios[%d].%s", i, campo)
            .isEqualTo(doRelatorio.get(i).get(campo).toString());
  }

  // --- produtor e geral -------------------------------------------------------------------------

  @Test
  void deveSepararOProdutorEJuntarTodosQuandoHouverDoisProdutores() throws Exception {
    Long outro = produtor("Produtor Teste Painel B", "99900001400", "2026-01-15");
    Long deA = plantio(produtor, milho, "LONGO", "1.000");
    Long deB = plantio(outro, hortalica, "CURTO", "1.000");
    despesa(produtor, adubo, "100.00", "2026-03-10", List.of(deA));
    producao(deA, "50.000", "2026-03-15");
    venda(produtor, List.of(deA), "50.000", "3.00", "2026-03-20");
    despesa(outro, adubo, "80.00", "2026-04-10", List.of(deB));

    String soA = painelComSucesso(SEMESTRE + "&produtorId=" + produtor);
    assertThat(JsonPath.<List<Integer>>read(soA, "$.plantios[*].plantioId"))
        .containsExactly(deA.intValue());
    assertThat(dinheiro(soA, "$.resultado")).isEqualByComparingTo("50.00");
    String soB = painelComSucesso(SEMESTRE + "&produtorId=" + outro);
    assertThat(JsonPath.<List<Integer>>read(soB, "$.plantios[*].plantioId"))
        .containsExactly(deB.intValue());
    assertThat(dinheiro(soB, "$.resultado")).isEqualByComparingTo("-80.00");

    String geral = painelComSucesso(SEMESTRE);
    List<Integer> todos = JsonPath.read(geral, "$.plantios[*].plantioId");
    assertThat(todos).contains(deA.intValue(), deB.intValue()).isSorted();
    for (String campo : List.of("receita", "custoDesembolso", "resultado"))
      assertThat(dinheiro(geral, "$." + campo))
          .as(campo)
          .isEqualByComparingTo(somaDaLista(geral, campo));
  }

  // --- N+1 --------------------------------------------------------------------------------------

  /** Plantios do produtor com despesa rateada, diária paga, produção e venda em cada um. */
  List<Long> plantiosComLancamentos(int quantos) throws Exception {
    List<Long> plantios = new ArrayList<>();
    for (int i = 0; i < quantos; i++) plantios.add(plantio(produtor, milho, "LONGO", "0.100"));
    despesa(produtor, adubo, "%d.00".formatted(quantos * 10), "2026-03-10", plantios);
    for (Long plantio : plantios) {
      diaria(plantio, "PAGA", "1.00", "50.00", "2026-03-12");
      producao(plantio, "10.000", "2026-04-10");
    }
    venda(produtor, plantios, "5.000", "2.00", "2026-04-12");
    return plantios;
  }

  /** Comandos SQL preparados durante um GET do painel do produtor. */
  long consultasDoPainel() throws Exception {
    Statistics estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
    boolean ligadas = estatisticas.isStatisticsEnabled();
    estatisticas.setStatisticsEnabled(true);
    estatisticas.clear();
    try {
      painelComSucesso(SEMESTRE + "&produtorId=" + produtor);
      return estatisticas.getPrepareStatementCount();
    } finally {
      estatisticas.setStatisticsEnabled(ligadas);
    }
  }

  @Test
  void deveManterONumeroDeConsultasQuandoDobrarOsPlantios() throws Exception {
    List<Long> plantios = plantiosComLancamentos(30);
    long com30 = consultasDoPainel();
    String painel = painelComSucesso(SEMESTRE + "&produtorId=" + produtor);
    assertThat(JsonPath.<List<Object>>read(painel, "$.plantios")).hasSize(30);
    assertThat(dinheiro(painel, "$.plantios[0].custoDesembolso")).isPositive();

    plantios.addAll(plantiosComLancamentos(30));
    long com60 = consultasDoPainel();
    System.out.printf("Consultas do painel: 30 plantios = %d; 60 plantios = %d%n", com30, com60);
    assertThat(com30).isLessThanOrEqualTo(8);
    assertThat(com60).isEqualTo(com30);
    assertThat(
            JsonPath.<List<Object>>read(
                painelComSucesso(SEMESTRE + "&produtorId=" + produtor), "$.plantios"))
        .hasSize(60);
  }

  // --- erros e perfil ---------------------------------------------------------------------------

  @Test
  void deveRecusarQuandoPeriodoForInvertido() throws Exception {
    painel(lancamento, "periodoInicio=2026-06-30&periodoFim=2026-01-01")
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem").value("O fim do período não pode ser anterior ao início."));
  }

  @Test
  void deveRecusarComNaoEncontradoQuandoProdutorNaoExistir() throws Exception {
    painel(lancamento, SEMESTRE + "&produtorId=999999999")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Produtor não encontrado."));
  }

  @Test
  void deveRecusarComFormatoInvalidoQuandoProdutorForZero() throws Exception {
    painel(lancamento, SEMESTRE + "&produtorId=0")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[0].campo").value("produtorId"))
        .andExpect(jsonPath("$.campos[0].mensagem").value("deve ser maior que 0"));
  }

  @Test
  void deveResponderParaConsultaQuandoPerfilSoLer() throws Exception {
    Long plantio = plantio(produtor, milho, "LONGO", "1.000");
    painel(usuarios.bearer(Perfil.CONSULTA), SEMESTRE + "&produtorId=" + produtor)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plantios[*].plantioId", contains(plantio.intValue())));
  }

  @Test
  void deveUsarDoComecoDoAnoAteHojeQuandoNaoHouverPeriodo() throws Exception {
    painel(lancamento, "produtorId=" + produtor)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodoInicio").value("2026-01-01"))
        .andExpect(jsonPath("$.periodoFim").value("2026-11-03"));
  }
}
