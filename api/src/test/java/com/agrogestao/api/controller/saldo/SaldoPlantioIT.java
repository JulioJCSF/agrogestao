package com.agrogestao.api.controller.saldo;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.model.Perfil;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * M05 contra o banco, com token real: o CT047 inteiro pela primeira vez (Q00e), o saldo sem recorte
 * de período e a sugestão de origem (RN28) pelo filtro de plantios da C06a, com a massa do CT045 e
 * do CT046. Plantios por SQL; produção e venda pelas APIs.
 */
class SaldoPlantioIT extends IntegracaoTest {
  static final String AVISO_CT047 =
      "A quantidade vendida do plantio (150,000 kg) supera a produção registrada (100,000 kg)."
          + " Confira os registros.";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, produtor, vizinho, milho, feijao, cliente;
  Map<Long, Long> propriedades = new HashMap<>();

  @BeforeEach
  void preparar() throws Exception {
    relogio.fixar(Instant.parse("2026-10-12T15:00:00Z"));
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    usuarios.criar(Perfil.CONSULTA);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    feijao = jdbc.queryForObject("select id from cultura where nome = 'Feijão'", Long.class);
    produtor = produtor("Produtor Teste Venda", "99900002121");
    vizinho = produtor("Produtor Teste Vizinho", "99900002202");
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Venda','FEIRA',?) returning id",
            Long.class,
            autor);
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    for (Long id : propriedades.keySet()) {
      jdbc.update("delete from venda where produtor_id = ?", id);
      jdbc.update(
          "delete from producao where plantio_id in (select id from plantio where propriedade_id = ?)",
          propriedades.get(id));
      jdbc.update("delete from plantio where propriedade_id = ?", propriedades.get(id));
      jdbc.update("delete from propriedade where id = ?", propriedades.get(id));
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
    propriedades.put(
        id,
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            id,
            autor));
    return id;
  }

  /** Plantio de 1,000 ha; aberto quando {@code encerramento} é nulo. */
  Long plantio(Long doProdutor, Long cultura, String inicio, String encerramento) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,data_encerramento,criado_por) values (?,?,1.000,'LONGO',?::date,?::date,?) returning id",
        Long.class,
        propriedades.get(doProdutor),
        cultura,
        inicio,
        encerramento,
        autor);
  }

  Long plantio(Long doProdutor, Long cultura) {
    return plantio(doProdutor, cultura, "2026-02-01", null);
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
    return mvc.perform(
        requisicao
            .header(HttpHeaders.AUTHORIZATION, lancamento)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  ResultActions consultar(String caminho) throws Exception {
    return mvc.perform(get(caminho).header(HttpHeaders.AUTHORIZATION, lancamento));
  }

  void producao(Long plantio, String quantidade, String data) throws Exception {
    enviar(
            post("/api/v1/plantios/" + plantio + "/producoes"),
            "{\"quantidade\":%s,\"unidadeMedida\":\"kg\",\"dataFato\":\"%s\"}"
                .formatted(quantidade, data))
        .andExpect(status().isCreated());
  }

  ResultActions vender(Long plantio, String quantidade, String data) throws Exception {
    return enviar(
        post("/api/v1/vendas"),
        "{\"produtorId\":%d,\"clienteId\":%d,\"dataFato\":\"%s\",\"itens\":[{\"plantioId\":%d,\"quantidade\":%s,\"unidadeMedida\":\"kg\",\"valorUnitario\":5.00}]}"
            .formatted(produtor, cliente, data, plantio, quantidade));
  }

  /** Compara o texto do JSON, para conferir também as 3 casas (100.000, não 100 nem 100.0). */
  ResultActions saldo(Long plantio, String produzido, String vendido, String saldo)
      throws Exception {
    return consultar("/api/v1/plantios/" + plantio + "/saldo")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plantioId").value(plantio))
        .andExpect(jsonPath("$.unidadeMedida").value("kg"))
        .andExpect(content().string(containsString("\"quantidadeProduzida\":" + produzido)))
        .andExpect(content().string(containsString("\"quantidadeVendida\":" + vendido)))
        .andExpect(content().string(containsString("\"saldo\":" + saldo + ",")));
  }

  int itensDoPlantio(Long plantio) {
    return jdbc.queryForObject(
        "select count(*) from item_venda where plantio_id = ?", Integer.class, plantio);
  }

  // --- CT047 ------------------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveAceitarESinalizarComOSaldoDaMesmaContaQuandoVendaPassarDoProduzidoCt047()
      throws Exception {
    Long p1 = plantio(produtor, milho);
    producao(p1, "100.000", "2026-03-10");

    // Passo 1.
    vender(p1, "60.000", "2026-06-10")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.itens[0].valorTotal").value(300.00))
        .andExpect(jsonPath("$.avisos", hasSize(0)));
    // Passo 2.
    saldo(p1, "100.000", "60.000", "40.000");
    // Passo 3: aceita; só o acumulado (150,000) passa do produzido.
    vender(p1, "90.000", "2026-06-10")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.itens[0].valorTotal").value(450.00))
        .andExpect(jsonPath("$.avisos", contains(AVISO_CT047)));
    // Passo 4: o mesmo 150,000 / 100,000 do aviso, com sinal.
    saldo(p1, "100.000", "150.000", "-50.000");
    // Passo 5.
    assertThat(itensDoPlantio(p1)).isEqualTo(2);
  }

  @Test
  void deveSomarTudoSemRecorteDePeriodoQuandoProducaoEVendaEstiveremEmMesesDiferentes()
      throws Exception {
    Long p1 = plantio(produtor, milho);
    producao(p1, "33.333", "2026-03-10");
    producao(p1, "66.667", "2026-08-15");
    vender(p1, "10.001", "2026-04-01").andExpect(status().isCreated());
    vender(p1, "20.002", "2026-06-10").andExpect(status().isCreated());
    vender(p1, "30.003", "2026-09-20").andExpect(status().isCreated());
    saldo(p1, "100.000", "60.006", "39.994");
  }

  @Test
  void deveResponderZeroNoPlantioSemRegistroEEncerradoNormalmenteQuandoConsultar()
      throws Exception {
    Long vazio = plantio(produtor, milho);
    saldo(vazio, "0.000", "0.000", "0.000");
    Long encerrado = plantio(produtor, milho, "2026-01-10", "2026-05-31");
    producao(encerrado, "12.500", "2026-03-10");
    saldo(encerrado, "12.500", "0.000", "12.500");
  }

  @Test
  void deveResponder404QuandoPlantioNaoExistirEPermitirConsultaAoPerfilConsulta() throws Exception {
    consultar("/api/v1/plantios/999999999/saldo")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Plantio não encontrado."));
    Long p1 = plantio(produtor, milho);
    mvc.perform(
            get("/api/v1/plantios/" + p1 + "/saldo")
                .header(HttpHeaders.AUTHORIZATION, usuarios.bearer(Perfil.CONSULTA)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"saldo\":0.000,")));
  }

  // --- sugestão de origem (RN28)
  // ------------------------------------------------------------------

  ResultActions abertos(Long doProdutor, Long cultura) throws Exception {
    return consultar(
            "/api/v1/plantios?produtorId=%d&culturaId=%d&situacao=ABERTO"
                .formatted(doProdutor, cultura))
        .andExpect(status().isOk());
  }

  @Test
  void deveTrazerSoOPlantioAbertoDaCulturaDoProdutorQuandoHouverUmCandidatoCt045()
      throws Exception {
    Long pm1 = plantio(produtor, milho);
    plantio(produtor, milho, "2026-01-10", "2026-05-31");
    Long pf = plantio(produtor, feijao);
    plantio(vizinho, milho);

    abertos(produtor, milho)
        .andExpect(jsonPath("$.itens[*].id", contains(pm1.intValue())))
        .andExpect(jsonPath("$.total").value(1));
    abertos(produtor, feijao).andExpect(jsonPath("$.itens[*].id", contains(pf.intValue())));
  }

  @Test
  void deveTrazerOsDoisEmOrdemDeIdQuandoHouverDoisCandidatosCt046() throws Exception {
    Long pm1 = plantio(produtor, milho);
    Long pm2 = plantio(produtor, milho);
    plantio(produtor, milho, "2026-01-10", "2026-05-31");

    abertos(produtor, milho)
        .andExpect(jsonPath("$.itens[*].id", contains(pm1.intValue(), pm2.intValue())));
  }
}
