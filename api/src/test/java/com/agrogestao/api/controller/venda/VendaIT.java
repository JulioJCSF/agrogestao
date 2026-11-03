package com.agrogestao.api.controller.venda;

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
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * L09b contra o banco, com a massa da Q00e por SQL. Automatiza CT042, CT043, CT044 e CT047 (sem o
 * {@code /saldo}, da M05), que a Q02 herda. Sem {@code @Transactional}: o GET precisa ler do banco
 * e a falha precisa provar que nada foi gravado. Limpeza no {@code @AfterEach}.
 */
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class VendaIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  Long autor, produtor, propriedade, vizinho, propriedadeVizinho, milho, feijao, cliente, cultura;

  @BeforeEach
  void preparar() {
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
    autor = new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
    produtor = produtor("Produtor Teste Venda", "99900001150");
    propriedade = propriedade(produtor);
    vizinho = produtor("Produtor Teste Vizinho", "99900001664");
    propriedadeVizinho = propriedade(vizinho);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    feijao = jdbc.queryForObject("select id from cultura where nome = 'Feijão'", Long.class);
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Venda','FEIRA',?) returning id",
            Long.class,
            autor);
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    Object[] produtores = {produtor, vizinho};
    for (String sql :
        List.of(
            "delete from venda where produtor_id in (?,?)",
            "delete from producao where plantio_id in (select p.id from plantio p join propriedade r on r.id = p.propriedade_id where r.produtor_id in (?,?))",
            "delete from plantio where propriedade_id in (select id from propriedade where produtor_id in (?,?))",
            "delete from propriedade where produtor_id in (?,?)",
            "delete from produtor where id in (?,?)")) jdbc.update(sql, produtores);
    jdbc.update("delete from cliente where id = ?", cliente);
    if (cultura != null) jdbc.update("delete from cultura where id = ?", cultura);
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

  /** Plantio de 1,000 ha, ciclo curto, início em 2026-02-01. */
  Long plantio(Long naPropriedade, Long daCultura, LocalDate encerramento) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,data_encerramento,criado_por) values (?,?,1.000,'CURTO','2026-02-01',?,?) returning id",
        Long.class,
        naPropriedade,
        daCultura,
        encerramento,
        autor);
  }

  Long plantio(Long daCultura) {
    return plantio(propriedade, daCultura, null);
  }

  void producao(Long plantio, String quantidade) {
    jdbc.update(
        "insert into producao(plantio_id,quantidade,unidade_medida,data_fato,data_coleta,criado_por) values (?,?::numeric,'kg','2026-05-20','2026-05-20',?)",
        plantio,
        quantidade,
        autor);
  }

  static String item(Object plantio, String quantidade, String unidade, String valor) {
    return "{\"plantioId\":%s,\"quantidade\":%s,\"unidadeMedida\":\"%s\",\"valorUnitario\":%s}"
        .formatted(plantio, quantidade, unidade, valor);
  }

  String corpo(Long doProdutor, String dataFato, String... itens) {
    return "{\"produtorId\":%d,\"clienteId\":%d,\"dataFato\":\"%s\",\"itens\":[%s]}"
        .formatted(doProdutor, cliente, dataFato, String.join(",", itens));
  }

  String corpo(String... itens) {
    return corpo(produtor, "2026-06-10", itens);
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String json) throws Exception {
    return mvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(json));
  }

  Long vender(String json) throws Exception {
    String resposta =
        enviar(post("/api/v1/vendas"), json)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(resposta, "$.id")).longValue();
  }

  int vendas() {
    return jdbc.queryForObject(
        "select count(*) from venda where produtor_id in (?,?)", Integer.class, produtor, vizinho);
  }

  int itens() {
    return jdbc.queryForObject(
        "select count(*) from item_venda i join venda v on v.id = i.venda_id where v.produtor_id in (?,?)",
        Integer.class,
        produtor,
        vizinho);
  }

  @Test
  void deveRegistrarVendaComItensNoPlantioDeOrigemQuandoReceberUmOuMaisItensCt042()
      throws Exception {
    Long p1 = plantio(milho), p2 = plantio(feijao);
    producao(p1, "100.000");
    producao(p2, "50.000");

    enviar(post("/api/v1/vendas"), corpo(item(p1, "10.000", "kg", "4.50")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(".*/api/v1/vendas/\\d+$")))
        .andExpect(jsonPath("$.produtorId").value(produtor))
        .andExpect(jsonPath("$.clienteId").value(cliente))
        .andExpect(jsonPath("$.dataFato").value("2026-06-10"))
        .andExpect(jsonPath("$.dataColeta").value("2026-10-11"))
        .andExpect(jsonPath("$.valorTotal").value(45.00))
        .andExpect(jsonPath("$.avisos", hasSize(0)))
        .andExpect(jsonPath("$.itens", hasSize(1)))
        .andExpect(jsonPath("$.itens[0].plantioId").value(p1))
        .andExpect(jsonPath("$.itens[0].quantidade").value(10.000))
        .andExpect(jsonPath("$.itens[0].unidadeMedida").value("kg"))
        .andExpect(jsonPath("$.itens[0].valorUnitario").value(4.50))
        .andExpect(jsonPath("$.itens[0].valorTotal").value(45.00));

    String passo2 =
        enviar(
                post("/api/v1/vendas"),
                corpo(item(p1, "20.000", "kg", "4.50"), item(p2, "5.000", "kg", "7.25")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.itens[0].plantioId").value(p1))
            .andExpect(jsonPath("$.itens[0].valorTotal").value(90.00))
            .andExpect(jsonPath("$.itens[1].plantioId").value(p2))
            .andExpect(jsonPath("$.itens[1].valorTotal").value(36.25))
            .andExpect(jsonPath("$.valorTotal").value(126.25))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Number id = JsonPath.read(passo2, "$.id");
    String detalhe =
        mvc.perform(get("/api/v1/vendas/" + id))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(JsonPath.<Object>read(detalhe, "$")).isEqualTo(JsonPath.read(passo2, "$"));
    assertThat(vendas()).isEqualTo(2);
    assertThat(itens()).isEqualTo(3);
  }

  @Test
  void deveRejeitarComCampoItensSemGravarQuandoVendaNaoTiverItensCt043() throws Exception {
    String semItens =
        "{\"produtorId\":%d,\"clienteId\":%d,\"dataFato\":\"2026-06-10\""
            .formatted(produtor, cliente);
    for (String json :
        List.of(semItens + "}", semItens + ",\"itens\":[]}", semItens + ",\"itens\":null}"))
      enviar(post("/api/v1/vendas"), json)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"))
          .andExpect(jsonPath("$.campos[*].campo", contains("itens")));
    assertThat(vendas()).isZero();
    assertThat(itens()).isZero();
  }

  @Test
  void deveRejeitarSemGravarQuandoItemNaoTiverPlantioOuPlantioNaoExistirCt044() throws Exception {
    Long p1 = plantio(milho);
    producao(p1, "100.000");
    String semPlantio = "{\"quantidade\":10.000,\"unidadeMedida\":\"kg\",\"valorUnitario\":4.50}";
    for (String invalido :
        List.of(semPlantio, item("null", "10.000", "kg", "4.50"), item(0, "10.000", "kg", "4.50")))
      enviar(post("/api/v1/vendas"), corpo(invalido))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"))
          .andExpect(jsonPath("$.campos[*].campo", contains("itens[0].plantioId")));
    enviar(post("/api/v1/vendas"), corpo(item(Long.MAX_VALUE, "10.000", "kg", "4.50")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.mensagem").value("Plantio não encontrado."))
        .andExpect(jsonPath("$.campos", hasSize(0)));
    enviar(post("/api/v1/vendas"), corpo(item(p1, "10.000", "kg", "4.50"), semPlantio))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", contains("itens[1].plantioId")));
    assertThat(vendas()).isZero();
    assertThat(itens()).isZero();
  }

  static final String ACIMA =
      "A quantidade vendida do plantio (150,000 kg) supera a produção registrada (100,000 kg)."
          + " Confira os registros.";

  @Test
  void deveAceitarESinalizarQuandoVendidoAcumuladoPassarDoProduzidoCt047() throws Exception {
    Long p1 = plantio(milho);
    producao(p1, "100.000");
    String primeira =
        enviar(post("/api/v1/vendas"), corpo(item(p1, "60.000", "kg", "5.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.itens[0].valorTotal").value(300.00))
            .andExpect(jsonPath("$.avisos", hasSize(0)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    // 90 sozinho não passa de 100; o acumulado 150 passa. Defeito: 422 ou avisos vazio.
    String segunda =
        enviar(post("/api/v1/vendas"), corpo(item(p1, "90.000", "kg", "5.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.itens[0].valorTotal").value(450.00))
            .andExpect(jsonPath("$.avisos", contains(ACIMA)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from item_venda where plantio_id = ?", Integer.class, p1))
        .isEqualTo(2);
    // Decisão 3 de 03/11: o detalhe recalcula com o acumulado atual, inclusive o da primeira.
    for (String venda : List.of(segunda, primeira))
      mvc.perform(get("/api/v1/vendas/" + JsonPath.read(venda, "$.id")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.avisos", contains(ACIMA)));
  }

  @Test
  void deveRejeitarSemGravarQuandoPlantioForDeOutroProdutor() throws Exception {
    Long meu = plantio(milho), alheio = plantio(propriedadeVizinho, milho, null);
    enviar(
            post("/api/v1/vendas"),
            corpo(item(meu, "1.000", "kg", "2.00"), item(alheio, "1.000", "kg", "2.00")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value("Todos os itens devem ser de plantios do produtor informado."))
        .andExpect(jsonPath("$.campos[*].campo", contains("itens[1].plantioId")));
    assertThat(vendas()).isZero();
    assertThat(itens()).isZero();
  }

  @Test
  void deveRejeitarComCampoDoItemQuandoQuantidadeOuValorNaoForPositivo() throws Exception {
    Long p1 = plantio(milho);
    enviar(post("/api/v1/vendas"), corpo(item(p1, "0", "kg", "2.00")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.campos[*].campo", contains("itens[0].quantidade")));
    enviar(post("/api/v1/vendas"), corpo(item(p1, "1.000", "kg", "-1")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.campos[*].campo", contains("itens[0].valorUnitario")));
    enviar(post("/api/v1/vendas"), corpo(item(p1, "1.000", "saca", "2.00")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "A unidade do item (saca) é diferente da unidade da cultura do plantio (kg)."))
        .andExpect(jsonPath("$.campos[*].campo", contains("itens[0].unidadeMedida")));
    assertThat(vendas()).isZero();
  }

  @Test
  void deveRejeitarQuandoDataDoFatoForPosteriorAoEncerramentoRn04() throws Exception {
    Long encerrado = plantio(propriedade, milho, LocalDate.of(2026, 6, 30));
    enviar(
            post("/api/v1/vendas"),
            corpo(produtor, "2026-07-01", item(encerrado, "1.000", "kg", "2.00")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "O plantio foi encerrado em 30/06/2026. A data do fato não pode ser posterior"
                        + " ao encerramento."));
    enviar(
            post("/api/v1/vendas"),
            corpo(produtor, "2026-06-30", item(encerrado, "1.000", "kg", "2.00")))
        .andExpect(status().isCreated());
    assertThat(vendas()).isEqualTo(1);
  }

  @Test
  void deveSubstituirItensEManterColetaQuandoAlterarVenda() throws Exception {
    Long p1 = plantio(milho), p2 = plantio(milho);
    Long id = vender(corpo(item(p1, "10.000", "kg", "4.50"), item(p1, "1.000", "kg", "4.50")));
    List<Long> antigos =
        jdbc.queryForList("select id from item_venda where venda_id = ?", Long.class, id);
    relogio.fixar(Instant.parse("2026-10-20T15:00:00Z"));
    enviar(
            put("/api/v1/vendas/" + id),
            corpo(
                produtor,
                "2026-06-12",
                item(p2, "2.000", "KG", "7.25"),
                item(p1, "3.333", "kg", "4.50")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataFato").value("2026-06-12"))
        .andExpect(jsonPath("$.dataColeta").value("2026-10-11"))
        .andExpect(jsonPath("$.itens[*].plantioId", contains(p2.intValue(), p1.intValue())))
        .andExpect(jsonPath("$.itens[0].unidadeMedida").value("kg"))
        .andExpect(jsonPath("$.valorTotal").value(29.50));
    List<Long> atuais =
        jdbc.queryForList("select id from item_venda where venda_id = ?", Long.class, id);
    assertThat(atuais).hasSize(2).doesNotContainAnyElementsOf(antigos);
    assertThat(
            jdbc.queryForObject("select data_coleta from venda where id = ?", LocalDate.class, id))
        .isEqualTo(LocalDate.of(2026, 10, 11));
    assertThat(jdbc.queryForObject("select valor_total from venda where id = ?", Double.class, id))
        .isEqualTo(29.50);
  }

  @Test
  void deveRemoverVendaEItensQuandoExcluir() throws Exception {
    Long p1 = plantio(milho);
    Long id = vender(corpo(item(p1, "1.000", "kg", "2.00"), item(p1, "2.000", "kg", "2.00")));
    mvc.perform(delete("/api/v1/vendas/" + id)).andExpect(status().isNoContent());
    assertThat(vendas()).isZero();
    assertThat(itens()).isZero();
    mvc.perform(get("/api/v1/vendas/" + id)).andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/vendas/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void deveFiltrarPorProdutorEPeriodoComBordasQuandoListar() throws Exception {
    Long p1 = plantio(milho), alheio = plantio(propriedadeVizinho, milho, null);
    producao(p1, "100.000");
    Long antes = vender(corpo(produtor, "2026-05-31", item(p1, "1.000", "kg", "2.00")));
    Long inicio = vender(corpo(produtor, "2026-06-01", item(p1, "1.000", "kg", "2.00")));
    Long fim = vender(corpo(produtor, "2026-06-30", item(p1, "1.000", "kg", "2.00")));
    Long depois = vender(corpo(produtor, "2026-07-01", item(p1, "1.000", "kg", "2.00")));
    vender(corpo(vizinho, "2026-06-10", item(alheio, "1.000", "kg", "2.00")));

    mvc.perform(
            get("/api/v1/vendas")
                .param("produtorId", produtor.toString())
                .param("periodoInicio", "2026-06-01")
                .param("periodoFim", "2026-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", contains(inicio.intValue(), fim.intValue())))
        .andExpect(jsonPath("$.itens[0].itens[0].plantioId").value(p1))
        .andExpect(jsonPath("$.itens[0].avisos", hasSize(0)));
    mvc.perform(
            get("/api/v1/vendas")
                .param("produtorId", produtor.toString())
                .param("tamanho", "1")
                .param("pagina", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(4))
        .andExpect(jsonPath("$.itens[*].id", contains(depois.intValue())));
    mvc.perform(
            get("/api/v1/vendas")
                .param("produtorId", produtor.toString())
                .param("periodoFim", "2026-05-31"))
        .andExpect(jsonPath("$.itens[*].id", contains(antes.intValue())));
    mvc.perform(
            get("/api/v1/vendas")
                .param("periodoInicio", "2026-06-02")
                .param("periodoFim", "2026-06-01"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem").value("O fim do período não pode ser anterior ao início."));
  }

  @Test
  void deveMostrarAvisoNaListaQuandoVendidoPassarDoProduzido() throws Exception {
    Long p1 = plantio(milho);
    producao(p1, "100.000");
    vender(corpo(item(p1, "60.000", "kg", "5.00")));
    vender(corpo(item(p1, "90.000", "kg", "5.00")));
    mvc.perform(get("/api/v1/vendas").param("produtorId", produtor.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens", hasSize(2)))
        .andExpect(jsonPath("$.itens[0].avisos", contains(ACIMA)))
        .andExpect(jsonPath("$.itens[1].avisos", contains(ACIMA)));
  }

  @Test
  void deveApurarReceitaEQuantidadeVendidaQuandoVendaVierDaApi() throws Exception {
    Long p1 = plantio(milho), p2 = plantio(milho);
    vender(
        corpo(
            item(p1, "3.333", "kg", "4.50"),
            item(p1, "2.335", "kg", "3.00"),
            item(p2, "1.000", "kg", "9.00")));
    vender(corpo(produtor, "2026-07-01", item(p1, "5.000", "kg", "1.00")));
    // Junho: 15,00 + 7,01 = 22,01; vendido 5,668. Defeito: 31,01 (total da venda) ou 22,00.
    mvc.perform(get("/api/v1/plantios/" + p1 + "/resultado").param("mes", "2026-06"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.receita").value(22.01))
        .andExpect(jsonPath("$.quantidadeVendida").value(5.668))
        .andExpect(jsonPath("$.resultado").value(22.01));
  }

  @Test
  void deveRecusarTrocaDeUnidadeDaCulturaQuandoPlantioTiverSoItemDeVendaRn38() throws Exception {
    cultura =
        jdbc.queryForObject(
            "insert into cultura(nome,tipo_ciclo_padrao,unidade_medida,criado_por) values ('Cultura Teste Venda','CURTO','kg',?) returning id",
            Long.class,
            autor);
    Long plantio = plantio(cultura);
    vender(corpo(item(plantio, "1.000", "kg", "2.00")));
    enviar(
            put("/api/v1/culturas/" + cultura),
            "{\"nome\":\"Cultura Teste Venda\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"saca\"}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem")
                .value("A unidade da cultura não pode mudar porque já há produção registrada."));
    assertThat(
            jdbc.queryForObject(
                "select unidade_medida from cultura where id = ?", String.class, cultura))
        .isEqualTo("kg");
  }

  @Test
  @WithMockUser(username = UsuariosTeste.CONSULTA, roles = "CONSULTA")
  void deveNegarCadastroEPermitirConsultaQuandoPerfilForConsulta() throws Exception {
    Long p1 = plantio(milho);
    enviar(post("/api/v1/vendas"), corpo(item(p1, "1.000", "kg", "2.00")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/vendas").param("produtorId", produtor.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    assertThat(vendas()).isZero();
  }
}
