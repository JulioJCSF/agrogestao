package com.agrogestao.api.controller.resultado;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A01 contra o banco: o que o unitário com repositórios mockados não prova. Diária, despesa e
 * rateio entram pelas APIs da L07 e da L04a; venda e item de venda por SQL até a L09b. Token real
 * do login (F04b). Sem {@code @Transactional}: limpeza no {@code @AfterEach}.
 *
 * <p>Datas já passadas e plantios longos encerrados, para não depender do dia em que roda. A diária
 * familiar usa 2024, porque a migration de dados iniciais já tem referência vigente de 2026 em
 * diante e outro *IT pode tê-la apagado; em 2024 a referência é criada aqui.
 */
class ApuracaoResultadoIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, produtor, propriedade, milho, adubo, cliente, referencia;

  @BeforeEach
  void preparar() throws Exception {
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    usuarios.criar(Perfil.CONSULTA);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Apuração','99900001079',?) returning id",
            Long.class,
            autor);
    propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            autor);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Apuração','FEIRA',?) returning id",
            Long.class,
            autor);
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    jdbc.update("delete from venda where produtor_id = ?", produtor);
    jdbc.update("delete from despesa where produtor_id = ?", produtor);
    jdbc.update(
        "delete from diaria where plantio_id in (select id from plantio where propriedade_id = ?)",
        propriedade);
    if (referencia != null)
      jdbc.update("delete from valor_referencia_diaria where id = ?", referencia);
    jdbc.update("delete from plantio where propriedade_id = ?", propriedade);
    jdbc.update("delete from propriedade where id = ?", propriedade);
    jdbc.update("delete from produtor where id = ?", produtor);
    jdbc.update("delete from cliente where id = ?", cliente);
    for (Perfil perfil : new Perfil[] {Perfil.LANCAMENTO, Perfil.CONSULTA})
      jdbc.update("delete from usuario where login = ?", UsuariosTeste.login(perfil));
  }

  /** Plantio de Milho (cultura LONGO) com o ciclo efetivo informado, 1,000 ha. */
  Long plantio(String ciclo, String inicio, String encerramento) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,data_encerramento,criado_por) values (?,?,1.000,?,?::date,?::date,?) returning id",
        Long.class,
        propriedade,
        milho,
        ciclo,
        inicio,
        encerramento,
        autor);
  }

  Long curto() {
    return plantio("CURTO", "2026-02-01", null);
  }

  Long longoEncerrado() {
    return plantio("LONGO", "2026-02-01", "2026-07-31");
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
    return mvc.perform(
        requisicao
            .header(HttpHeaders.AUTHORIZATION, lancamento)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  ResultActions resultado(Long plantio, String mes) throws Exception {
    return resultado(plantio, mes, lancamento);
  }

  ResultActions resultado(Long plantio, String mes, String bearer) throws Exception {
    MockHttpServletRequestBuilder requisicao = get("/api/v1/plantios/" + plantio + "/resultado");
    if (mes != null) requisicao.param("mes", mes);
    return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer));
  }

  void diaria(Long plantio, String natureza, String dias, String valor, String data)
      throws Exception {
    enviar(
            post("/api/v1/plantios/" + plantio + "/diarias"),
            "{\"natureza\":\"%s\",\"quantidadeDias\":%s,\"valorUnitario\":%s,\"dataFato\":\"%s\"}"
                .formatted(natureza, dias, valor, data))
        .andExpect(status().isCreated());
  }

  Long despesa(String valor, String data, Long... plantios) throws Exception {
    String corpo =
        enviar(
                post("/api/v1/despesas"),
                "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Adubo Teste Apuração\",\"valorTotal\":%s,\"dataFato\":\"%s\",\"plantioIds\":[%s]}"
                    .formatted(
                        produtor,
                        adubo,
                        valor,
                        data,
                        Arrays.stream(plantios)
                            .map(String::valueOf)
                            .collect(Collectors.joining(","))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(corpo, "$.id")).longValue();
  }

  @Test
  void deveSomarDiariaPagaUmaVezQuandoGeradaPelaApiCt033() throws Exception {
    Long pl = longoEncerrado();
    diaria(pl, "PAGA", "1.00", "80.00", "2026-03-10");
    // Defeito: 160.00 — a diária paga somada por fora, além da despesa que ela gerou.
    resultado(pl, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.custoDesembolso").value(80.00))
        .andExpect(jsonPath("$.valorMaoObraFamiliar").value(0.00))
        .andExpect(jsonPath("$.custoComMaoObraFamiliar").value(80.00))
        .andExpect(jsonPath("$.resultado").value(-80.00));
  }

  @Test
  void deveDeixarDiariaFamiliarForaDoDesembolsoQuandoGravadaPelaApiCt034() throws Exception {
    referencia =
        jdbc.queryForObject(
            "insert into valor_referencia_diaria(valor,vigencia_inicio,vigencia_fim,criado_por) values (80.00,'2024-03-01','2024-03-31',?) returning id",
            Long.class,
            autor);
    Long pl = plantio("LONGO", "2024-02-01", "2024-07-31");
    diaria(pl, "FAMILIAR", "1.00", "null", "2024-03-10");
    resultado(pl, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodoInicio").value("2024-02-01"))
        .andExpect(jsonPath("$.periodoFim").value("2024-07-31"))
        .andExpect(jsonPath("$.custoDesembolso").value(0.00))
        .andExpect(jsonPath("$.valorMaoObraFamiliar").value(80.00))
        .andExpect(jsonPath("$.custoComMaoObraFamiliar").value(80.00))
        .andExpect(jsonPath("$.resultado").value(0.00));
  }

  @Test
  void deveLerValorRateadoGravadoEIgnorarEstornoQuandoDespesaVierDaApi() throws Exception {
    Long a = longoEncerrado(), b = longoEncerrado(), c = longoEncerrado();
    Long despesa = despesa("100.00", "2026-03-10", a, b, c);
    // Residual ao menor id (RN09); percentual × total daria 33.33 para os três.
    resultado(a, null).andExpect(jsonPath("$.custoDesembolso").value(33.34));
    resultado(b, null).andExpect(jsonPath("$.custoDesembolso").value(33.33));
    resultado(c, null).andExpect(jsonPath("$.custoDesembolso").value(33.33));

    jdbc.update("update despesa set estornada = TRUE where id = ?", despesa);
    resultado(a, null)
        .andExpect(jsonPath("$.custoDesembolso").value(0.00))
        .andExpect(jsonPath("$.resultado").value(0.00));
  }

  @Test
  void deveSomarSoItensDoPlantioQuandoVendaTiverOutroPlantio() throws Exception {
    Long plantio = curto(), outro = curto();
    Long venda =
        jdbc.queryForObject(
            "insert into venda(produtor_id,cliente_id,valor_total,data_fato,data_coleta,criado_por) values (?,?,190.00,'2026-03-10','2026-03-10',?) returning id",
            Long.class,
            produtor,
            cliente,
            autor);
    String item =
        "insert into item_venda(venda_id,plantio_id,quantidade,unidade_medida,valor_unitario,valor_total,criado_por) values (?,?,?,'kg',?,?,?)";
    jdbc.update(item, venda, plantio, 10, 5.00, 50.00, autor);
    jdbc.update(item, venda, outro, 20, 7.00, 140.00, autor);
    // Defeito: 190.00 — o total da venda no lugar dos itens do plantio.
    resultado(plantio, "2026-03")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.receita").value(50.00))
        .andExpect(jsonPath("$.quantidadeVendida").value(10.000))
        .andExpect(jsonPath("$.resultado").value(50.00));
  }

  @Test
  void deveIncluirAsDuasBordasQuandoApurarMesDoPlantioCurto() throws Exception {
    Long plantio = curto();
    despesa("1.00", "2026-02-28", plantio);
    despesa("10.00", "2026-03-01", plantio);
    despesa("100.00", "2026-03-31", plantio);
    despesa("1000.00", "2026-04-01", plantio);
    resultado(plantio, "2026-03")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipoCiclo").value("CURTO"))
        .andExpect(jsonPath("$.periodoInicio").value("2026-03-01"))
        .andExpect(jsonPath("$.periodoFim").value("2026-03-31"))
        .andExpect(jsonPath("$.custoDesembolso").value(110.00));
  }

  @Test
  void deveApurarMesDeFortalezaQuandoCurtoVierSemMes() throws Exception {
    Long plantio = curto();
    relogio.fixar(Instant.parse("2026-11-01T01:30:00Z"));
    resultado(plantio, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodoInicio").value("2026-10-01"))
        .andExpect(jsonPath("$.periodoFim").value("2026-10-31"));
  }

  @Test
  void deveRecusarMesInvalidoEPlantioInexistenteQuandoConsultar() throws Exception {
    Long plantio = curto();
    for (String mes : new String[] {"2026-13", "abc"})
      resultado(plantio, mes)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"))
          .andExpect(jsonPath("$.campos[0].campo").value("mes"));
    resultado(999999L, null)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.mensagem").value("Plantio não encontrado."));
  }

  @Test
  void devePermitirConsultaQuandoPerfilForConsulta() throws Exception {
    Long plantio = longoEncerrado();
    resultado(plantio, null, usuarios.bearer(Perfil.CONSULTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plantioId").value(plantio))
        .andExpect(jsonPath("$.culturaNome").value("Milho"))
        .andExpect(jsonPath("$.propriedadeNome").value("Sítio Teste"))
        .andExpect(jsonPath("$.unidadeMedida").value("kg"))
        .andExpect(jsonPath("$.quantidadeProduzida").value(0.0))
        .andExpect(jsonPath("$.receita").value(0.0));
  }
}
