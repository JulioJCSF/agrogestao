package com.agrogestao.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matriz de permissões da F04b pela API, com token real do login: só assim o conversor de token
 * roda e o perfil vem do banco. CPF sintético com DV válido.
 */
@Transactional
class PermissoesIT extends IntegracaoTest {
  static final String NEGADO = "Você não tem permissão para realizar esta operação.";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  UsuariosTeste usuarios;
  Long produtor, plantio, outroPlantio, adubo, cliente;

  @BeforeEach
  void preparar() {
    usuarios = new UsuariosTeste(jdbc, mvc);
    for (Perfil perfil : Perfil.values()) usuarios.criar(perfil);
    Long autor = usuarios.id(Perfil.LANCAMENTO);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Permissões','99900020103',?) returning id",
            Long.class,
            autor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            autor);
    Long milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    plantio = plantio(propriedade, milho, autor);
    outroPlantio = plantio(propriedade, milho, autor);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste F04b','FEIRA',?) returning id",
            Long.class,
            autor);
  }

  Long plantio(Long propriedade, Long cultura, Long autor) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1.000,'LONGO','2026-02-01',?) returning id",
        Long.class,
        propriedade,
        cultura,
        autor);
  }

  ResultActions como(String bearer, MockHttpServletRequestBuilder requisicao) throws Exception {
    return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer));
  }

  static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String corpo) {
    return req.contentType(MediaType.APPLICATION_JSON).content(corpo);
  }

  static MockHttpServletRequestBuilder cultura(String nome) {
    return json(
        post("/api/v1/culturas"),
        "{\"nome\":\"" + nome + "\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}");
  }

  MockHttpServletRequestBuilder despesa() {
    return json(
        post("/api/v1/despesas"),
        "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste F04b\",\"valorTotal\":90.00,\"dataFato\":\"2026-03-10\",\"plantioIds\":[%d,%d]}"
            .formatted(produtor, adubo, plantio, outroPlantio));
  }

  static void assertNegado(ResultActions resultado) throws Exception {
    resultado
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"))
        .andExpect(jsonPath("$.mensagem").value(NEGADO))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.campos", hasSize(0)));
  }

  long contar(String sql, Object... args) {
    return jdbc.queryForObject(sql, Long.class, args);
  }

  @Test
  void deveLerQuandoPerfilForConsulta() throws Exception {
    String consulta = usuarios.bearer(Perfil.CONSULTA);
    como(consulta, get("/api/v1/culturas")).andExpect(status().isOk());
    como(consulta, get("/api/v1/plantios")).andExpect(status().isOk());
  }

  @Test
  void deveResponder403SemGravarQuandoConsultaCriarCultura() throws Exception {
    assertNegado(como(usuarios.bearer(Perfil.CONSULTA), cultura("Cultura Teste F04b Consulta")));
    assertThat(contar("select count(*) from cultura where nome = 'Cultura Teste F04b Consulta'"))
        .isZero();
  }

  @Test
  void deveResponder403SemAlterarQuandoConsultaGravarLancamentoOuCadastro() throws Exception {
    String consulta = usuarios.bearer(Perfil.CONSULTA);
    assertNegado(
        como(
            consulta,
            json(
                put("/api/v1/clientes/" + cliente),
                "{\"nome\":\"Cliente Teste F04b Alterado\",\"tipoCanal\":\"CEASA\"}")));
    assertNegado(como(consulta, despesa()));
    assertNegado(
        como(
            consulta,
            json(
                post("/api/v1/plantios/" + plantio + "/encerramento"),
                "{\"dataEncerramento\":\"2026-06-30\"}")));
    assertThat(jdbc.queryForMap("select nome,tipo_canal from cliente where id=?", cliente))
        .containsEntry("nome", "Cliente Teste F04b")
        .containsEntry("tipo_canal", "FEIRA");
    assertThat(contar("select count(*) from despesa where produtor_id=?", produtor)).isZero();
    assertThat(
            jdbc.queryForObject(
                "select data_encerramento from plantio where id=?", Object.class, plantio))
        .isNull();
  }

  @Test
  void deveResponder403AntesDo501QuandoConsultaEmitirRelatorio() throws Exception {
    assertNegado(como(usuarios.bearer(Perfil.CONSULTA), json(post("/api/v1/relatorios"), "{}")));
  }

  @Test
  void deveCriarQuandoLancamentoGravarConfiguracoesELancamentos() throws Exception {
    String lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    como(lancamento, cultura("Cultura Teste F04b Lançamento")).andExpect(status().isCreated());
    como(
            lancamento,
            json(
                post("/api/v1/valores-referencia-diaria"),
                "{\"valor\":75.00,\"vigenciaInicio\":\"2023-01-01\",\"vigenciaFim\":\"2023-12-31\"}"))
        .andExpect(status().isCreated());
    como(
            lancamento,
            json(post("/api/v1/categorias-despesa"), "{\"nome\":\"Categoria Teste F04b\"}"))
        .andExpect(status().isCreated());
    como(lancamento, despesa()).andExpect(status().isCreated());
    como(
            lancamento,
            json(
                post("/api/v1/plantios/" + plantio + "/encerramento"),
                "{\"dataEncerramento\":\"2026-06-30\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void deveResponder403QuandoLancamentoOuConsultaLerUsuarios() throws Exception {
    assertNegado(como(usuarios.bearer(Perfil.LANCAMENTO), get("/api/v1/usuarios")));
    assertNegado(como(usuarios.bearer(Perfil.CONSULTA), get("/api/v1/usuarios")));
  }

  @Test
  void devePassarDaAutorizacaoQuandoAdminAcessarUsuariosOuGravar() throws Exception {
    String admin = usuarios.bearer(Perfil.ADMIN);
    como(admin, get("/api/v1/usuarios"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.erro").value("NAO_IMPLEMENTADO"));
    como(admin, cultura("Cultura Teste F04b Admin")).andExpect(status().isCreated());
  }

  @Test
  void deveNegarQuandoRotaEstiverForaDaMatriz() throws Exception {
    assertNegado(como(usuarios.bearer(Perfil.ADMIN), get("/rota-fora-da-api")));
  }

  @Test
  void deveDevolverPerfilDoBancoQuandoConsultarUsuarioLogado() throws Exception {
    for (Perfil perfil : Perfil.values())
      como(usuarios.bearer(perfil), get("/api/v1/auth/me"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(usuarios.id(perfil)))
          .andExpect(jsonPath("$.perfil").value(perfil.name()));
  }

  @Test
  void deveResponder401QuandoNaoHouverToken() throws Exception {
    mvc.perform(get("/api/v1/culturas"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
  }

  @Test
  void deveNegarComMesmoTokenQuandoPerfilForRebaixadoNoBanco() throws Exception {
    String token = usuarios.bearer(Perfil.LANCAMENTO);
    jdbc.update("update usuario set perfil = 'CONSULTA' where login = ?", UsuariosTeste.LANCAMENTO);
    assertNegado(como(token, cultura("Cultura Teste F04b Rebaixado")));
    assertThat(contar("select count(*) from cultura where nome = 'Cultura Teste F04b Rebaixado'"))
        .isZero();
  }

  @Test
  void deveResponder401ComMesmoTokenQuandoUsuarioForDesativado() throws Exception {
    String token = usuarios.bearer(Perfil.LANCAMENTO);
    jdbc.update("update usuario set ativo = false where login = ?", UsuariosTeste.LANCAMENTO);
    como(token, get("/api/v1/culturas"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
  }

  @Test
  void deveResponder401QuandoUsuarioDoTokenForApagado() throws Exception {
    String token = usuarios.bearer(Perfil.CONSULTA);
    jdbc.update("delete from usuario where login = ?", UsuariosTeste.CONSULTA);
    como(token, get("/api/v1/culturas"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
  }

  @Test
  void devePermitirComMesmoTokenQuandoPerfilForPromovidoNoBanco() throws Exception {
    String token = usuarios.bearer(Perfil.CONSULTA);
    jdbc.update("update usuario set perfil = 'LANCAMENTO' where login = ?", UsuariosTeste.CONSULTA);
    como(token, cultura("Cultura Teste F04b Promovido")).andExpect(status().isCreated());
  }
}
