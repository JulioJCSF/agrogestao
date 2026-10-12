package com.agrogestao.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

class ContratoIT extends IntegracaoTest {
  @Autowired MockMvc mvc;

  @Test
  void devePublicarTodosOsModulosQuandoSwaggerForConsultado() throws Exception {
    String body =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Map<String, Object> paths = JsonPath.read(body, "$.paths");
    assertThat(paths)
        .containsKeys(
            "/api/v1/produtores",
            "/api/v1/produtores/{id}",
            "/api/v1/produtores/{produtorId}/propriedades",
            "/api/v1/propriedades/{id}",
            "/api/v1/culturas",
            "/api/v1/plantios",
            "/api/v1/plantios/{id}",
            "/api/v1/plantios/{id}/encerramento",
            "/api/v1/plantios/{plantioId}/producoes",
            "/api/v1/producoes/{id}",
            "/api/v1/categorias-despesa",
            "/api/v1/despesas",
            "/api/v1/despesas/{id}",
            "/api/v1/despesas/{id}/estorno",
            "/api/v1/plantios/{plantioId}/despesas",
            "/api/v1/valores-referencia-diaria",
            "/api/v1/valores-referencia-diaria/vigente",
            "/api/v1/plantios/{plantioId}/diarias",
            "/api/v1/diarias/{id}",
            "/api/v1/clientes",
            "/api/v1/vendas",
            "/api/v1/vendas/{id}",
            "/api/v1/plantios/{id}/resultado",
            "/api/v1/relatorios",
            "/api/v1/relatorios/{id}",
            "/api/v1/produtores/{produtorId}/relatorios",
            "/api/v1/usuarios",
            "/api/v1/usuarios/{id}",
            "/api/v1/plantios/{id}/saldo",
            "/api/v1/painel");
    Map<String, Object> schemas = JsonPath.read(body, "$.components.schemas");
    assertThat(schemas)
        .containsKeys(
            "ProdutorRequisicao",
            "ProdutorResposta",
            "PropriedadeRequisicao",
            "PropriedadeResposta",
            "CulturaRequisicao",
            "CulturaResposta",
            "PlantioRequisicao",
            "PlantioResposta",
            "EncerramentoPlantioRequisicao",
            "ProducaoRequisicao",
            "ProducaoResposta",
            "CategoriaDespesaRequisicao",
            "CategoriaDespesaResposta",
            "DespesaRequisicao",
            "DespesaResposta",
            "RateioDespesaResposta",
            "DespesaPlantioResposta",
            "ValorReferenciaDiariaRequisicao",
            "ValorReferenciaDiariaResposta",
            "DiariaRequisicao",
            "DiariaResposta",
            "ClienteRequisicao",
            "ClienteResposta",
            "ItemVendaRequisicao",
            "ItemVendaResposta",
            "VendaRequisicao",
            "VendaResposta",
            "ResultadoPlantioResposta",
            "EmissaoRelatorioRequisicao",
            "EmissaoRelatorioResposta",
            "ConteudoRelatorioResposta",
            "UsuarioRequisicao",
            "UsuarioAlteracaoRequisicao",
            "UsuarioResposta",
            "SaldoPlantioResposta",
            "PainelResposta");
    for (String name :
        java.util.List.of(
            "CulturaRequisicao",
            "DespesaRequisicao",
            "VendaRequisicao",
            "EmissaoRelatorioResposta")) {
      assertThat(((Map<?, ?>) schemas.get(name)).containsKey("example")).isTrue();
    }
    assertThat(body).doesNotContain("senhaHash", "senha_hash");
  }

  @Test
  @WithMockUser
  void deveRetornarErroPadraoQuandoModuloAindaForEsqueleto() throws Exception {
    mvc.perform(get("/api/v1/plantios"))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.status").value(501))
        .andExpect(jsonPath("$.erro").value("NAO_IMPLEMENTADO"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.campos", hasSize(0)))
        .andExpect(jsonPath("$.mensagem").isNotEmpty());
    mvc.perform(
            post("/api/v1/despesas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"produtorId\":1,\"categoriaDespesaId\":2,\"descricao\":\"Fixture\",\"valorTotal\":100.25,\"dataFato\":\"2026-02-01\",\"plantioIds\":[1,2]}"))
        .andExpect(status().isNotImplemented());
  }

  @Test
  @WithMockUser
  void deveValidarCamposAninhadosQuandoContratoDeVendaForInvalido() throws Exception {
    mvc.perform(
            post("/api/v1/vendas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"produtorId\":1,\"clienteId\":1,\"dataFato\":\"2026-02-01\",\"itens\":[{\"quantidade\":1,\"unidadeMedida\":\"kg\",\"valorUnitario\":80}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[0].campo").value("itens[0].plantioId"));
  }

  @Test
  void deveExigirTokenQuandoContratoNaoForPublico() throws Exception {
    mvc.perform(get("/api/v1/plantios"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
  }
}
