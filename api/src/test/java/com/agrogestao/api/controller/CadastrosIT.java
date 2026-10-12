package com.agrogestao.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.jayway.jsonpath.JsonPath;
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
    mvc.perform(
            post("/api/v1/culturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Milho\",\"tipoCicloPadrao\":\"LONGO\",\"unidadeMedida\":\"kg\"}"))
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
    Long produtor = jdbc.queryForObject("select id from produtor order by id limit 1", Long.class);
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
        jdbc.queryForObject("select id from categoria_despesa where sistema=true", Long.class);
    mvc.perform(
            put("/api/v1/categorias-despesa/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Alterada\"}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(delete("/api/v1/categorias-despesa/" + id))
        .andExpect(status().isUnprocessableEntity());
  }
}
