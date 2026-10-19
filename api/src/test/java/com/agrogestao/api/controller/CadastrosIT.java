package com.agrogestao.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser
class CadastrosIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  Long autor;

  @BeforeEach
  void criarAutorSintetico() {
    autor =
        jdbc.queryForObject(
            "insert into usuario(nome,login,senha_hash,perfil) values (?,?,?,?) returning id",
            Long.class,
            "Autor Fictício CadastrosIT",
            "autor-ficticio-cadastros-it",
            "hash-sintetico-nao-utilizado",
            "ADMIN");
  }

  private Long criarProdutor() {
    return jdbc.queryForObject(
        "insert into produtor(nome,documento,criado_por) values (?,?,?) returning id",
        Long.class,
        "Produtor Fictício CadastrosIT",
        "99900000188",
        autor);
  }

  private Number criarCultura(String nome) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/culturas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\""
                            + nome
                            + "\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private Number criarCategoria(String nome) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/categorias-despesa")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"" + nome + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  @Test
  void deveCriarAlterarEBuscarCulturaQuandoCadastroValido() throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/culturas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\"Cultura Fictícia Cadastro\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/v1/culturas/")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Number id = JsonPath.read(body, "$.id");
    mvc.perform(
            put("/api/v1/culturas/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Cultura Fictícia Atualizada\",\"tipoCicloPadrao\":\"LONGO\",\"unidadeMedida\":\"un\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipoCicloPadrao").value("LONGO"));
    mvc.perform(get("/api/v1/culturas").param("busca", "Fictícia Atualizada").param("tamanho", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamanho").value(1))
        .andExpect(jsonPath("$.itens[0].id").value(id));
    mvc.perform(get("/api/v1/culturas/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unidadeMedida").value("un"));
  }

  @Test
  void deveRejeitarQuandoNomeDeCulturaDuplicado() throws Exception {
    criarCultura("Cultura Fictícia Duplicada");
    mvc.perform(
            post("/api/v1/culturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Cultura Fictícia Duplicada\",\"tipoCicloPadrao\":\"LONGO\",\"unidadeMedida\":\"kg\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
  }

  @Test
  void deveRetornarValidacaoQuandoEntradaOuPaginaInvalida() throws Exception {
    mvc.perform(
            post("/api/v1/culturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[0].campo").value("nome"));
    mvc.perform(get("/api/v1/culturas").param("tamanho", "0")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/culturas/999999")).andExpect(status().isNotFound());
  }

  @Test
  void deveCriarAlterarELerPropriedadeQuandoOpcionaisAusentes() throws Exception {
    Long produtor = criarProdutor();
    String body =
        mvc.perform(
                post("/api/v1/produtores/" + produtor + "/propriedades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Sítio Fictício Cadastro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.areaTotal").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Number id = JsonPath.read(body, "$.id");
    mvc.perform(
            put("/api/v1/propriedades/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Sítio Fictício Alterado\",\"areaTotal\":5.125}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.produtorId").value(produtor));
    mvc.perform(get("/api/v1/propriedades/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaTotal").value(5.125));
    mvc.perform(get("/api/v1/produtores/" + produtor + "/propriedades"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[*].produtorId", everyItem(equalTo(produtor.intValue()))));
    mvc.perform(
            post("/api/v1/produtores/999999/propriedades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Sítio Fictício\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveCriarAlterarExcluirCategoriaQuandoManual() throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/categorias-despesa")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Categoria Fictícia Cadastro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sistema").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Number id = JsonPath.read(body, "$.id");
    mvc.perform(
            put("/api/v1/categorias-despesa/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Categoria Fictícia Atualizada\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/categorias-despesa").param("busca", "Fictícia Atualizada"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(delete("/api/v1/categorias-despesa/" + id)).andExpect(status().isNoContent());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from categoria_despesa where id=?", Long.class, id))
        .isZero();
  }

  @Test
  void deveRejeitarAlteracaoEExclusaoQuandoCategoriaDeSistema() throws Exception {
    Long id =
        jdbc.queryForObject(
            "insert into categoria_despesa(nome,sistema,criado_por) values (?,true,?) returning id",
            Long.class,
            "Categoria Fictícia Sistema IT",
            autor);
    mvc.perform(
            put("/api/v1/categorias-despesa/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Alterada\"}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(delete("/api/v1/categorias-despesa/" + id))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRejeitarCriacaoEAlteracaoQuandoCulturaDuplicarNomeSemDiferenciarCaixa()
      throws Exception {
    criarCultura("Cultura Fictícia Original");
    Number outra = criarCultura("Outra Cultura Fictícia");
    for (String nome :
        java.util.List.of("Cultura Fictícia Original", "cultura fictícia original")) {
      String body =
          "{\"nome\":\"" + nome + "\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}";
      mvc.perform(
              put("/api/v1/culturas/" + outra)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
      mvc.perform(post("/api/v1/culturas").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.mensagem").value("Já existe uma cultura com este nome."));
    }
  }

  @Test
  void devePermitirAlterarApenasACaixaQuandoNomeForDaPropriaCultura() throws Exception {
    Number id = criarCultura("Cultura Fictícia Própria");
    mvc.perform(
            put("/api/v1/culturas/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"cultura fictícia própria\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("cultura fictícia própria"));
  }

  @Test
  void deveRejeitarCriacaoEAlteracaoQuandoCategoriaDuplicarNomeSemDiferenciarCaixa()
      throws Exception {
    criarCategoria("Categoria Fictícia Original");
    Number outra = criarCategoria("Outra Categoria Fictícia");
    for (String nome :
        java.util.List.of("Categoria Fictícia Original", "categoria fictícia original")) {
      String body = "{\"nome\":\"" + nome + "\"}";
      mvc.perform(
              post("/api/v1/categorias-despesa")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
      mvc.perform(
              put("/api/v1/categorias-despesa/" + outra)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.mensagem").value("Já existe uma categoria com este nome."));
    }
  }

  @Test
  void devePermitirAlterarApenasACaixaQuandoNomeForDaPropriaCategoria() throws Exception {
    Number id = criarCategoria("Categoria Fictícia Própria");
    mvc.perform(
            put("/api/v1/categorias-despesa/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"categoria fictícia própria\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("categoria fictícia própria"));
  }

  @Test
  void deveRejeitarExclusaoComMensagemClaraQuandoCategoriaEstiverEmUso() throws Exception {
    Long produtor = criarProdutor();
    Number categoria = criarCategoria("Categoria Fictícia Em Uso");
    jdbc.update(
        "insert into despesa(produtor_id,categoria_despesa_id,descricao,valor_total,data_fato,data_coleta,criado_por) values (?,?,?,1,current_date,current_date,?)",
        produtor,
        categoria,
        "Despesa Fictícia CadastrosIT",
        autor);
    mvc.perform(delete("/api/v1/categorias-despesa/" + categoria))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value("Esta categoria já foi usada em despesas e não pode ser excluída."));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from categoria_despesa where id=?", Long.class, categoria))
        .isEqualTo(1);
  }

  @Test
  void deveRetornar404QuandoPropriedadeOuProdutorNaoExistir() throws Exception {
    mvc.perform(get("/api/v1/propriedades/" + Long.MAX_VALUE)).andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/propriedades/" + Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Propriedade Fictícia Ausente\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/produtores/" + Long.MAX_VALUE + "/propriedades"))
        .andExpect(status().isNotFound());
  }
}
