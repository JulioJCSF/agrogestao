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
 * A03b contra o banco, pela API com token real: CT020 da Q00b, a estornada congelada, a diária paga
 * com despesa estornada e a venda em período emitido (decisões de 03/11). As emissões saem da API
 * da A03a, e todo caso que mexe em lançamento de período emitido compara {@code conteudo::text}
 * antes e depois (RN34).
 */
class ProtecaoEmissaoIT extends IntegracaoTest {
  static final String SEMESTRE = "\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\"";
  static final String EXCLUSAO_EMITIDA =
      "Esta despesa já consta em relatório emitido e não pode ser excluída. Use o estorno.";
  static final String ESTORNADA =
      "Esta despesa foi estornada e não pode mais ser alterada nem excluída.";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, produtor, milho, adubo, cliente, p1, p2;
  List<Long> produtores = new ArrayList<>();
  Map<Long, Long> propriedades = new HashMap<>();

  @BeforeEach
  void preparar() throws Exception {
    relogio.fixar(Instant.parse("2026-11-03T15:00:00Z"));
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    usuarios.criar(Perfil.CONSULTA);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Estorno','FEIRA',?) returning id",
            Long.class,
            autor);
    produtor = produtor("Produtor Teste Relatório", "99900001311");
    p1 = plantio(produtor);
    p2 = plantio(produtor);
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
    jdbc.update("delete from cliente where id = ?", cliente);
    for (Perfil perfil : new Perfil[] {Perfil.LANCAMENTO, Perfil.CONSULTA})
      jdbc.update("delete from usuario where login = ?", UsuariosTeste.login(perfil));
  }

  // --- massa ------------------------------------------------------------------------------------

  Long produtor(String nome, String cpf) {
    Long id =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,consentimento_em,criado_por) values (?,?,'2026-01-15',?) returning id",
            Long.class,
            nome,
            cpf,
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

  /** Milho, 1,000 ha, aberto desde 2026-02-01. */
  Long plantio(Long doProdutor) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1.000,'LONGO','2026-02-01',?) returning id",
        Long.class,
        propriedades.get(doProdutor),
        milho,
        autor);
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
    return mvc.perform(
        requisicao
            .header(HttpHeaders.AUTHORIZATION, lancamento)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  ResultActions excluir(String caminho) throws Exception {
    return mvc.perform(delete(caminho).header(HttpHeaders.AUTHORIZATION, lancamento));
  }

  ResultActions estornar(Long despesa) throws Exception {
    return enviar(post("/api/v1/despesas/" + despesa + "/estorno"), "");
  }

  static long id(ResultActions resultado) throws Exception {
    return ((Number)
            JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id"))
        .longValue();
  }

  String despesaJson(Long doProdutor, String valor, String data, Long... plantios) {
    return "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Estorno\",\"valorTotal\":%s,\"dataFato\":\"%s\",\"plantioIds\":[%s]}"
        .formatted(
            doProdutor,
            adubo,
            valor,
            data,
            Arrays.stream(plantios).map(String::valueOf).collect(Collectors.joining(",")));
  }

  Long despesa(Long doProdutor, String valor, String data, Long... plantios) throws Exception {
    return id(
        enviar(post("/api/v1/despesas"), despesaJson(doProdutor, valor, data, plantios))
            .andExpect(status().isCreated()));
  }

  String vendaJson(String quantidade, String data) {
    return "{\"produtorId\":%d,\"clienteId\":%d,\"dataFato\":\"%s\",\"itens\":[{\"plantioId\":%d,\"quantidade\":%s,\"unidadeMedida\":\"kg\",\"valorUnitario\":5.00}]}"
        .formatted(produtor, cliente, data, p1, quantidade);
  }

  Long venda(String quantidade, String data) throws Exception {
    return id(
        enviar(post("/api/v1/vendas"), vendaJson(quantidade, data))
            .andExpect(status().isCreated()));
  }

  /** Emite 2026-01-01 a 2026-06-30 pela API da A03a e devolve o id. */
  long emitir(Long doProdutor) throws Exception {
    return id(
        enviar(post("/api/v1/relatorios"), "{\"produtorId\":%d,%s}".formatted(doProdutor, SEMESTRE))
            .andExpect(status().isCreated()));
  }

  String conteudo(long emissao) {
    return jdbc.queryForObject(
        "select conteudo::text from emissao_relatorio where id = ?", String.class, emissao);
  }

  List<BigDecimal> rateios(Long despesa) {
    return jdbc.queryForList(
        "select valor_rateado from rateio_despesa where despesa_id = ? order by plantio_id",
        BigDecimal.class,
        despesa);
  }

  boolean estornada(Long despesa) {
    return jdbc.queryForObject(
        "select estornada from despesa where id = ?", Boolean.class, despesa);
  }

  int despesasDoProdutor() {
    return jdbc.queryForObject(
        "select count(*) from despesa where produtor_id = ?", Integer.class, produtor);
  }

  ResultActions regra(ResultActions resultado, String mensagem) throws Exception {
    return resultado
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(jsonPath("$.mensagem").value(mensagem));
  }

  // --- CT020 ------------------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveRecusarExclusaoEAceitarEstornoQuandoDespesaEstiverEmPeriodoEmitidoCt020()
      throws Exception {
    Long d1 = despesa(produtor, "80.00", "2026-03-10", p1, p2);
    long e1 = emitir(produtor);
    String conteudoE1 = conteudo(e1);

    // Passo 1.
    regra(excluir("/api/v1/despesas/" + d1), EXCLUSAO_EMITIDA);
    // Passo 2.
    assertThat(estornada(d1)).isFalse();
    assertThat(rateios(d1))
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("40.00"), new BigDecimal("40.00"));
    int despesasAntes = despesasDoProdutor();

    // Passo 3.
    estornar(d1)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/v1/despesas/" + d1)))
        .andExpect(jsonPath("$.id").value(d1))
        .andExpect(jsonPath("$.estornada").value(true))
        .andExpect(jsonPath("$.rateios", hasSize(2)));

    // Passo 4: sem lançamento de ajuste, rateios preservados, E1 intacta (RN34).
    assertThat(estornada(d1)).isTrue();
    assertThat(despesasDoProdutor()).isEqualTo(despesasAntes);
    assertThat(rateios(d1))
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("40.00"), new BigDecimal("40.00"));
    assertThat(conteudo(e1)).isEqualTo(conteudoE1);

    // Borda: fora do período emitido, exclui normalmente.
    Long d3 = despesa(produtor, "30.00", "2026-07-01", p1);
    excluir("/api/v1/despesas/" + d3).andExpect(status().isNoContent());
    assertThat(jdbc.queryForObject("select count(*) from despesa where id = ?", Integer.class, d3))
        .isZero();
  }

  // --- estornada congelada ----------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveCongelarAEstornadaQuandoTentarAlterarExcluirOuEstornarDeNovo() throws Exception {
    Long d1 = despesa(produtor, "80.00", "2026-03-10", p1, p2);
    estornar(d1).andExpect(status().isCreated());

    regra(
        enviar(put("/api/v1/despesas/" + d1), despesaJson(produtor, "90.00", "2026-03-10", p1, p2)),
        ESTORNADA);
    regra(excluir("/api/v1/despesas/" + d1), ESTORNADA);
    regra(estornar(d1), "Esta despesa já foi estornada.");
    assertThat(
            jdbc.queryForObject(
                "select valor_total from despesa where id = ?", BigDecimal.class, d1))
        .isEqualByComparingTo("80.00");
    estornar(999_999_999L)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Despesa não encontrada."));
  }

  @Test
  void deveEstornarADespesaDaDiariaPagaECongelarADiariaQuandoDespesaForGerada() throws Exception {
    long diaria =
        id(
            enviar(
                    post("/api/v1/plantios/" + p1 + "/diarias"),
                    "{\"natureza\":\"PAGA\",\"quantidadeDias\":1.00,\"valorUnitario\":90.00,\"dataFato\":\"2026-03-12\"}")
                .andExpect(status().isCreated()));
    Long gerada =
        jdbc.queryForObject("select id from despesa where diaria_id = ?", Long.class, diaria);

    estornar(gerada)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.diariaId").value(diaria));

    String mensagem =
        "A despesa desta diária foi estornada. A diária não pode mais ser alterada nem excluída.";
    regra(
        enviar(
            put("/api/v1/diarias/" + diaria),
            "{\"natureza\":\"PAGA\",\"quantidadeDias\":2.00,\"valorUnitario\":90.00,\"dataFato\":\"2026-03-12\"}"),
        mensagem);
    regra(excluir("/api/v1/diarias/" + diaria), mensagem);
    // RN19: a estornada sai do custo.
    mvc.perform(
            get("/api/v1/plantios/" + p1 + "/resultado")
                .header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.custoDesembolso").value(0.00));
    assertThat(
            jdbc.queryForObject(
                "select quantidade_dias from diaria where id = ?", BigDecimal.class, diaria))
        .isEqualByComparingTo("1.00");
  }

  // --- despesa emitida: PUT
  // -----------------------------------------------------------------------

  @Test
  void deveAceitarCorrecaoDoValorSemMudarAEmissaoQuandoDespesaEstiverEmitida() throws Exception {
    Long d1 = despesa(produtor, "80.00", "2026-03-10", p1, p2);
    long e1 = emitir(produtor);
    String conteudoE1 = conteudo(e1);

    enviar(put("/api/v1/despesas/" + d1), despesaJson(produtor, "100.00", "2026-03-10", p1, p2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorTotal").value(100.00))
        .andExpect(jsonPath("$.rateios[*].valorRateado", contains(50.00, 50.00)));

    assertThat(conteudo(e1)).isEqualTo(conteudoE1);
    long e2 = emitir(produtor);
    assertThat(
            jdbc.queryForObject(
                "select conteudo->>'custoDesembolso' from emissao_relatorio where id = ?",
                String.class,
                e2))
        .isEqualTo("100.00");
  }

  @Test
  @Tag("P1")
  void deveRecusarTrocaDaDataSemMudarNadaQuandoDespesaEstiverEmitida() throws Exception {
    Long d1 = despesa(produtor, "80.00", "2026-03-10", p1, p2);
    long e1 = emitir(produtor);
    String conteudoE1 = conteudo(e1);

    regra(
        enviar(put("/api/v1/despesas/" + d1), despesaJson(produtor, "90.00", "2026-07-02", p1, p2)),
        "Esta despesa já consta em relatório emitido. A data do fato não pode ser alterada.");

    Map<String, Object> linha =
        jdbc.queryForMap(
            "select data_fato::text as data, valor_total from despesa where id = ?", d1);
    assertThat(linha.get("data")).isEqualTo("2026-03-10");
    assertThat((BigDecimal) linha.get("valor_total")).isEqualByComparingTo("80.00");
    assertThat(conteudo(e1)).isEqualTo(conteudoE1);
  }

  // --- venda ------------------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveProtegerAVendaEmitidaEPermitirCorrigirAQuantidadeQuandoPeriodoEstiverEmitido()
      throws Exception {
    Long v1 = venda("10.000", "2026-03-20");
    long e1 = emitir(produtor);
    String conteudoE1 = conteudo(e1);

    regra(
        excluir("/api/v1/vendas/" + v1),
        "Esta venda já consta em relatório emitido e não pode ser excluída. Corrija pela"
            + " alteração.");
    regra(
        enviar(put("/api/v1/vendas/" + v1), vendaJson("10.000", "2026-07-02")),
        "Esta venda já consta em relatório emitido. A data do fato não pode ser alterada.");
    enviar(put("/api/v1/vendas/" + v1), vendaJson("20.000", "2026-03-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorTotal").value(100.00));

    assertThat(
            jdbc.queryForObject("select data_fato::text from venda where id = ?", String.class, v1))
        .isEqualTo("2026-03-20");
    assertThat(conteudo(e1)).isEqualTo(conteudoE1);

    Long foraDoPeriodo = venda("5.000", "2026-07-10");
    excluir("/api/v1/vendas/" + foraDoPeriodo).andExpect(status().isNoContent());
  }

  // --- fronteiras -------------------------------------------------------------------------------

  @Test
  void deveExcluirLancamentoDeOutroProdutorQuandoSoOPrimeiroTiverEmissao() throws Exception {
    Long outro = produtor("Produtor Teste Emissão", "99900001583");
    Long doOutro = despesa(outro, "50.00", "2026-03-10", plantio(outro));
    despesa(produtor, "80.00", "2026-03-10", p1);
    emitir(produtor);
    excluir("/api/v1/despesas/" + doOutro).andExpect(status().isNoContent());
  }

  @Test
  void deveNegarEstornoQuandoPerfilForConsulta() throws Exception {
    Long d1 = despesa(produtor, "80.00", "2026-03-10", p1);
    mvc.perform(
            post("/api/v1/despesas/" + d1 + "/estorno")
                .header(HttpHeaders.AUTHORIZATION, usuarios.bearer(Perfil.CONSULTA)))
        .andExpect(status().isForbidden());
    assertThat(estornada(d1)).isFalse();
  }
}
