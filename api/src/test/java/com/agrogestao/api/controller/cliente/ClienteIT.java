package com.agrogestao.api.controller.cliente;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.TipoCanal;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clientes do teste usam o prefixo "Cliente Teste L09a"; a migration já grava um cliente por canal,
 * então nenhuma asserção depende da contagem total.
 */
@Transactional
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class ClienteIT extends IntegracaoTest {
  static final String PREFIXO = "Cliente Teste L09a";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void criarLancador() {
    new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
  }

  static String corpo(String nome, String tipoCanal) {
    return "{\"nome\":\"" + nome + "\",\"tipoCanal\":\"" + tipoCanal + "\"}";
  }

  ResultActions enviar(String nome, String tipoCanal) throws Exception {
    return mvc.perform(
        post("/api/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo(nome, tipoCanal)));
  }

  Number criar(String nome, String tipoCanal) throws Exception {
    String body =
        enviar(nome, tipoCanal)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  @Test
  void deveCriarEDetalharQuandoCadastroValido() throws Exception {
    var resposta =
        enviar(PREFIXO + " Feira", "FEIRA")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value(PREFIXO + " Feira"))
            .andExpect(jsonPath("$.tipoCanal").value("FEIRA"))
            .andReturn()
            .getResponse();
    Number id = JsonPath.read(resposta.getContentAsString(), "$.id");
    assertThat(resposta.getHeader("Location")).isEqualTo("/api/v1/clientes/" + id);
    mvc.perform(get(resposta.getHeader("Location")))
        .andExpect(status().isOk())
        .andExpect(content().json(resposta.getContentAsString(), JsonCompareMode.STRICT));
    assertThat(jdbc.queryForObject("select criado_por from cliente where id=?", Long.class, id))
        .isNotNull();
  }

  @Test
  void deveGravarCadaCanalQuandoCriarUmPorValor() throws Exception {
    for (TipoCanal canal : TipoCanal.values()) {
      Number id = criar(PREFIXO + " " + canal, canal.name());
      assertThat(jdbc.queryForObject("select tipo_canal from cliente where id=?", String.class, id))
          .isEqualTo(canal.name());
    }
  }

  @Test
  void deveTrocarNomeECanalQuandoAlterar() throws Exception {
    Number id = criar(PREFIXO + " Feira", "FEIRA");
    Map<String, Object> antes =
        jdbc.queryForMap("select criado_em,atualizado_em from cliente where id=?", id);
    mvc.perform(
            put("/api/v1/clientes/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(PREFIXO + " Merenda", "MERENDA")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.nome").value(PREFIXO + " Merenda"))
        .andExpect(jsonPath("$.tipoCanal").value("MERENDA"));
    mvc.perform(get("/api/v1/clientes/" + id))
        .andExpect(jsonPath("$.nome").value(PREFIXO + " Merenda"))
        .andExpect(jsonPath("$.tipoCanal").value("MERENDA"));
    Map<String, Object> depois =
        jdbc.queryForMap("select criado_em,atualizado_em from cliente where id=?", id);
    assertThat(depois.get("criado_em")).isEqualTo(antes.get("criado_em"));
    assertThat(depois.get("atualizado_em")).isNotEqualTo(antes.get("atualizado_em"));
  }

  List<Number> criarTresParaBusca() throws Exception {
    return List.of(
        criar(PREFIXO + " Comércio Alfa", "COMERCIO"),
        criar(PREFIXO + " Feira Beta", "FEIRA"),
        criar("cliente teste l09a feira gama", "FEIRA"));
  }

  @Test
  void deveFiltrarSemDiferenciarMaiusculasQuandoBuscarPorTrecho() throws Exception {
    List<Number> ids = criarTresParaBusca();
    mvc.perform(get("/api/v1/clientes").param("busca", "  L09A FEIRA "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.itens[*].id", contains(ids.get(1), ids.get(2))))
        .andExpect(
            jsonPath(
                "$.itens[*].nome",
                contains(PREFIXO + " Feira Beta", "cliente teste l09a feira gama")));
  }

  @Test
  void devePaginarPorIdQuandoBuscaTiverMaisItensQueOTamanho() throws Exception {
    List<Number> ids = criarTresParaBusca();
    mvc.perform(
            get("/api/v1/clientes")
                .param("busca", PREFIXO)
                .param("pagina", "0")
                .param("tamanho", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.itens[*].id", contains(ids.get(0), ids.get(1))));
    mvc.perform(
            get("/api/v1/clientes")
                .param("busca", PREFIXO)
                .param("pagina", "1")
                .param("tamanho", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagina").value(1))
        .andExpect(jsonPath("$.itens[*].id", contains(ids.get(2))));
    for (String tamanho : List.of("0", "101"))
      mvc.perform(get("/api/v1/clientes").param("tamanho", tamanho))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"));
  }

  @Test
  void deveAceitarQuandoNomeSeRepetir() throws Exception {
    Number primeiro = criar(PREFIXO + " Repetido", "CEASA");
    Number segundo = criar(PREFIXO + " Repetido", "CEASA");
    assertThat(segundo).isNotEqualTo(primeiro);
  }

  @Test
  void deveResponder400QuandoEntradaForInvalida() throws Exception {
    for (String corpo :
        List.of(
            corpo("   ", "FEIRA"),
            corpo("x".repeat(151), "FEIRA"),
            "{\"nome\":\"" + PREFIXO + " Sem Canal\"}"))
      mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"))
          .andExpect(
              jsonPath(
                  "$.campos[*].campo",
                  hasItem(corpo.contains("tipoCanal") ? "nome" : "tipoCanal")));
    enviar(PREFIXO + " Atacado", "ATACADO")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[*].campo", hasItem("tipoCanal")));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cliente where nome like ?", Long.class, PREFIXO + "%"))
        .isZero();
  }

  @Test
  void deveResponder404QuandoClienteNaoExistir() throws Exception {
    mvc.perform(get("/api/v1/clientes/" + Long.MAX_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado."));
    mvc.perform(
            put("/api/v1/clientes/" + Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(PREFIXO + " Inexistente", "OUTRO")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
  }

  @Test
  @WithAnonymousUser
  void deveExigirTokenQuandoNaoAutenticado() throws Exception {
    mvc.perform(get("/api/v1/clientes"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
  }
}
