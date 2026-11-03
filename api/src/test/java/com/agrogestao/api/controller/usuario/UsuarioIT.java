package com.agrogestao.api.controller.usuario;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * M01 pela API, com token real do login (F04b), para provar junto que desativar e rebaixar cortam o
 * acesso na hora. Usuários criados pela API têm login {@code operador.teste.m01*} e são apagados no
 * {@code @AfterEach}; os ADMIN desativados por algum caso voltam a ficar ativos.
 */
@ExtendWith(OutputCaptureExtension.class)
class UsuarioIT extends IntegracaoTest {
  static final String SENHA = "senha-ficticia-m01";
  static final String LOGIN_REPETIDO = "Já existe um usuário com este login.";
  static final String ULTIMO_ADMIN = "O sistema precisa de pelo menos um administrador ativo.";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  UsuariosTeste usuarios;
  String admin;
  List<Long> adminsAtivos;

  @BeforeEach
  void preparar() throws Exception {
    usuarios = new UsuariosTeste(jdbc, mvc);
    for (Perfil perfil : Perfil.values()) usuarios.criar(perfil);
    admin = usuarios.bearer(Perfil.ADMIN);
    adminsAtivos =
        jdbc.queryForList("select id from usuario where perfil = 'ADMIN' and ativo", Long.class);
  }

  @AfterEach
  void limpar() {
    for (Long id : adminsAtivos)
      jdbc.update("update usuario set ativo = true, perfil = 'ADMIN' where id = ?", id);
    jdbc.update("delete from usuario where login like 'operador.teste.m01%'");
    for (Perfil perfil : Perfil.values())
      jdbc.update("delete from usuario where login = ?", UsuariosTeste.login(perfil));
  }

  ResultActions como(String bearer, MockHttpServletRequestBuilder requisicao, String json)
      throws Exception {
    if (json != null) requisicao.contentType(MediaType.APPLICATION_JSON).content(json);
    return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, bearer));
  }

  static String novo(String login, String senha, String perfil) {
    return "{\"nome\":\"Operador Teste M01\",\"login\":\"%s\",\"senha\":\"%s\",\"perfil\":\"%s\"}"
        .formatted(login, senha, perfil);
  }

  static String alteracao(String nome, String perfil, boolean ativo) {
    return "{\"nome\":\"%s\",\"perfil\":\"%s\",\"ativo\":%s}".formatted(nome, perfil, ativo);
  }

  Long cadastrar(String login, String perfil) throws Exception {
    String resposta =
        como(admin, post("/api/v1/usuarios"), novo(login, SENHA, perfil))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(resposta, "$.id")).longValue();
  }

  ResultActions entrar(String login, String senha) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)));
  }

  String bearer(String login, String senha) throws Exception {
    String resposta =
        entrar(login, senha)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(resposta, "$.token");
  }

  @Test
  void deveCriarComLoginNormalizadoSemDevolverSenhaQuandoAdminCadastrar() throws Exception {
    String corpo =
        como(admin, post("/api/v1/usuarios"), novo("Operador.Teste.M01", SENHA, "LANCAMENTO"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", matchesPattern(".*/api/v1/usuarios/\\d+$")))
            .andExpect(jsonPath("$.login").value("operador.teste.m01"))
            .andExpect(jsonPath("$.perfil").value("LANCAMENTO"))
            .andExpect(jsonPath("$.ativo").value(true))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(corpo).doesNotContain(SENHA, "$2");
    assertThat(
            jdbc.queryForObject(
                "select senha_hash from usuario where login = 'operador.teste.m01'", String.class))
        .startsWith("$2");
    // Decisão 3: o login compara sem diferenciar caixa.
    entrar("OPERADOR.TESTE.M01", SENHA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuario.perfil").value("LANCAMENTO"));
    como(admin, post("/api/v1/usuarios"), novo("operador.TESTE.m01", SENHA, "CONSULTA"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.mensagem").value(LOGIN_REPETIDO))
        .andExpect(jsonPath("$.campos[*].campo", contains("login")));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where lower(login) = 'operador.teste.m01'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void deveResponder400ComOCampoQuandoFormatoForInvalido() throws Exception {
    como(
            admin,
            post("/api/v1/usuarios"),
            "{\"nome\":\"\",\"login\":\"operador.teste.m01\",\"senha\":\"%s\",\"perfil\":\"LANCAMENTO\"}"
                .formatted(SENHA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", contains("nome")));
    como(admin, post("/api/v1/usuarios"), novo("operador.teste.m01", "1234567", "LANCAMENTO"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", contains("senha")));
    como(admin, post("/api/v1/usuarios"), novo("operador.teste.m01", SENHA, "CHEFE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[*].campo", contains("perfil")));
    // 40 "á": 40 caracteres passam no @Size, mas são 80 bytes, e o BCrypt truncaria.
    String acentos = "á".repeat(40);
    String corpo =
        como(admin, post("/api/v1/usuarios"), novo("operador.teste.m01", acentos, "LANCAMENTO"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("VALIDACAO"))
            .andExpect(jsonPath("$.campos[*].campo", contains("senha")))
            .andExpect(jsonPath("$.mensagem").value("A senha deve ter no máximo 72 bytes."))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(corpo).doesNotContain(acentos);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where login like 'operador.teste.m01%'",
                Integer.class))
        .isZero();
  }

  @Test
  void deveCortarTokenAntigoQuandoAdminDesativarUsuario() throws Exception {
    Long id = cadastrar("operador.teste.m01", "LANCAMENTO");
    String token = bearer("operador.teste.m01", SENHA);
    como(token, get("/api/v1/culturas"), null).andExpect(status().isOk());
    como(admin, put("/api/v1/usuarios/" + id), alteracao("Operador Teste M01", "LANCAMENTO", false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativo").value(false))
        .andExpect(jsonPath("$.login").value("operador.teste.m01"));
    como(token, get("/api/v1/culturas"), null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
    entrar("operador.teste.m01", SENHA).andExpect(status().isUnauthorized());
  }

  @Test
  void deveNegarGravacaoComTokenAntigoQuandoAdminRebaixarParaConsulta() throws Exception {
    Long id = cadastrar("operador.teste.m01", "LANCAMENTO");
    String token = bearer("operador.teste.m01", SENHA);
    como(admin, put("/api/v1/usuarios/" + id), alteracao("Operador Teste M01", "CONSULTA", true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("CONSULTA"));
    como(
            token,
            post("/api/v1/culturas"),
            "{\"nome\":\"Cultura Teste M01\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}")
        .andExpect(status().isForbidden());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cultura where nome = 'Cultura Teste M01'", Integer.class))
        .isZero();
  }

  @Test
  void deveManterUmAdminAtivoQuandoTentarDesativarOUltimo() throws Exception {
    Long doTeste = usuarios.id(Perfil.ADMIN);
    Long daMigration =
        jdbc.queryForObject(
            "select id from usuario where login = 'admin-teste-sintetico'", Long.class);
    String nomeDaMigration =
        jdbc.queryForObject("select nome from usuario where id = ?", String.class, daMigration);
    // Só os dois ficam ativos; o @AfterEach reativa os demais.
    for (Long id : adminsAtivos)
      if (!id.equals(doTeste) && !id.equals(daMigration))
        jdbc.update("update usuario set ativo = false where id = ?", id);

    como(admin, put("/api/v1/usuarios/" + daMigration), alteracao(nomeDaMigration, "ADMIN", false))
        .andExpect(status().isOk());
    como(admin, put("/api/v1/usuarios/" + doTeste), alteracao("Usuário Teste", "ADMIN", false))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.mensagem").value(ULTIMO_ADMIN));
    como(admin, put("/api/v1/usuarios/" + doTeste), alteracao("Usuário Teste", "LANCAMENTO", true))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.mensagem").value(ULTIMO_ADMIN));
    assertThat(
            jdbc.queryForList(
                "select id from usuario where perfil = 'ADMIN' and ativo", Long.class))
        .containsExactly(doTeste);
  }

  @Test
  void deveTrocarASenhaQuandoAdminRedefinir() throws Exception {
    Long id = cadastrar("operador.teste.m01", "LANCAMENTO");
    como(admin, put("/api/v1/usuarios/" + id + "/senha"), "{\"senha\":\"senha-nova-ficticia\"}")
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    entrar("operador.teste.m01", SENHA).andExpect(status().isUnauthorized());
    entrar("operador.teste.m01", "senha-nova-ficticia").andExpect(status().isOk());
    como(admin, put("/api/v1/usuarios/" + id + "/senha"), "{\"senha\":\"1234567\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", contains("senha")));
    como(admin, put("/api/v1/usuarios/999999999/senha"), "{\"senha\":\"senha-nova-ficticia\"}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Usuário não encontrado."));
  }

  @Test
  void deveNegarCadastroESenhaQuandoPerfilNaoForAdmin() throws Exception {
    Long id = usuarios.id(Perfil.CONSULTA);
    for (Perfil perfil : List.of(Perfil.LANCAMENTO, Perfil.CONSULTA)) {
      String token = usuarios.bearer(perfil);
      como(token, post("/api/v1/usuarios"), novo("operador.teste.m01", SENHA, "ADMIN"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));
      como(token, put("/api/v1/usuarios/" + id + "/senha"), "{\"senha\":\"senha-nova-ficticia\"}")
          .andExpect(status().isForbidden());
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where login like 'operador.teste.m01%'",
                Integer.class))
        .isZero();
    entrar(UsuariosTeste.CONSULTA, "senha-nova-ficticia").andExpect(status().isUnauthorized());
  }

  @Test
  void deveResponder405NoFormatoPadraoQuandoTentarExcluirUsuario() throws Exception {
    Long id = usuarios.id(Perfil.CONSULTA);
    como(admin, delete("/api/v1/usuarios/" + id), null)
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.campos", hasSize(0)));
    assertThat(jdbc.queryForObject("select count(*) from usuario where id = ?", Integer.class, id))
        .isEqualTo(1);
  }

  @Test
  void deveListarPorIdSemSenhaQuandoAdminConsultar() throws Exception {
    cadastrar("operador.teste.m01a", "CONSULTA");
    String corpo =
        como(admin, get("/api/v1/usuarios").param("tamanho", "100"), null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens[*].login", hasItem("operador.teste.m01a")))
            .andExpect(jsonPath("$.itens[0].senhaHash").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Integer> ids = JsonPath.read(corpo, "$.itens[*].id");
    assertThat(ids).isSorted();
    assertThat(corpo).doesNotContain("senha", "$2");
    como(admin, get("/api/v1/usuarios").param("tamanho", "1").param("pagina", "1"), null)
        .andExpect(jsonPath("$.itens[0].id").value(ids.get(1)))
        .andExpect(jsonPath("$.tamanho").value(1));
  }

  @Test
  void deveManterSenhaForaDoLogQuandoCadastrarERedefinir(CapturedOutput saida) throws Exception {
    String senha = "senha-fixture-nao-logar";
    Long id =
        JsonPath.<Number>read(
                como(admin, post("/api/v1/usuarios"), novo("operador.teste.m01", senha, "CONSULTA"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.id")
            .longValue();
    como(admin, put("/api/v1/usuarios/" + id + "/senha"), "{\"senha\":\"" + senha + "-2\"}")
        .andExpect(status().isNoContent());
    // Também no caminho de erro: login repetido e login com a senha antiga.
    como(admin, post("/api/v1/usuarios"), novo("OPERADOR.teste.m01", senha, "CONSULTA"))
        .andExpect(status().isUnprocessableContent());
    entrar("operador.teste.m01", senha).andExpect(status().isUnauthorized());
    assertThat(saida.getAll()).doesNotContain(senha);
  }
}
