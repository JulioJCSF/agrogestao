package com.agrogestao.api.config;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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
import org.springframework.transaction.annotation.Transactional;

/** 2026-10-12T01:30Z é 22h30 de 11/10 em Fortaleza: em UTC, "amanhã" já seria hoje. */
@Transactional
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class RelogioIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;

  @BeforeEach
  void criarLancador() {
    new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
  }

  @BeforeEach
  void fixarNoiteEmFortaleza() {
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
  }

  @AfterEach
  void restaurar() {
    relogio.restaurar();
  }

  static String produtor(String consentimentoEm) {
    return "{\"nome\":\"Produtor Fictício Relógio\",\"documento\":\"99900001079\",\"consentimentoEm\":\""
        + consentimentoEm
        + "\",\"ativo\":true}";
  }

  ResultActions enviar(MockHttpServletRequestBuilder requisicao, String json) throws Exception {
    return mvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(json));
  }

  @Test
  void deveRejeitarConsentimentoQuandoAmanhaEmFortalezaJaForHojeEmUtc() throws Exception {
    enviar(post("/api/v1/produtores"), produtor("2026-10-12"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos[*].campo", hasItem("consentimentoEm")));
  }

  @Test
  void deveAceitarConsentimentoQuandoHojeAindaForNoiteEmFortaleza() throws Exception {
    String resposta =
        enviar(post("/api/v1/produtores"), produtor("2026-10-11"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consentimentoEm").value("2026-10-11"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Number id = JsonPath.read(resposta, "$.id");
    enviar(put("/api/v1/produtores/" + id), produtor("2026-10-12"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[*].campo", hasItem("consentimentoEm")));
  }

  @Test
  void deveUsarMesmoRelogioQuandoServiceValidarInicioDoPlantio() throws Exception {
    Long autor =
        jdbc.queryForObject(
            "insert into usuario(nome,login,senha_hash,perfil) values ('Autor Fictício Relógio','autor-relogio-it','hash-teste','LANCAMENTO') returning id",
            Long.class);
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Fictício Relógio','99900001150',?) returning id",
            Long.class,
            autor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,criado_por) values (?,'Sítio Fictício Relógio',?) returning id",
            Long.class,
            produtor,
            autor);
    Long cultura =
        jdbc.queryForObject(
            "insert into cultura(nome,tipo_ciclo_padrao,unidade_medida,criado_por) values ('Cultura Fictícia Relógio','CURTO','kg',?) returning id",
            Long.class,
            autor);
    String plantio =
        "{\"propriedadeId\":"
            + propriedade
            + ",\"culturaId\":"
            + cultura
            + ",\"area\":1,\"dataInicio\":\"%s\"}";
    enviar(post("/api/v1/plantios"), plantio.formatted("2026-10-12"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"));
    enviar(post("/api/v1/plantios"), plantio.formatted("2026-10-11"))
        .andExpect(status().isCreated());
  }
}
