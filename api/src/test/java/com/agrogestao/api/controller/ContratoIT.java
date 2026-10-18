package com.agrogestao.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.dto.*;
import com.jayway.jsonpath.JsonPath;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class ContratoIT extends IntegracaoTest {
  @Autowired MockMvc mvc;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  @Test
  void deveDeclararCriacaoComLocationEExclusaoSemCorpoQuandoSwaggerForConsultado()
      throws Exception {
    String body =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(body, "$.paths");
    paths.forEach(
        (path, operations) -> {
          if (!path.startsWith("/api/v1/") || path.startsWith("/api/v1/auth/")) return;
          for (String method : List.of("post", "delete")) {
            if (!operations.containsKey(method)) continue;
            Map<String, Object> responses =
                (Map<String, Object>) operations.get(method).get("responses");
            String expected = method.equals("post") ? "201" : "204";
            assertThat(responses)
                .as(method + " " + path)
                .containsKey(expected)
                .doesNotContainKey("200");
            Map<String, Object> success = (Map<String, Object>) responses.get(expected);
            if (method.equals("post")) {
              assertThat((Map<String, Object>) success.get("headers")).containsKey("Location");
            } else {
              assertThat(success).doesNotContainKey("content");
            }
          }
        });
    mappings
        .getHandlerMethods()
        .forEach(
            (mapping, handler) -> {
              if (mapping.getPatternValues().stream()
                  .noneMatch(
                      path -> path.startsWith("/api/v1/") && !path.startsWith("/api/v1/auth/")))
                return;
              var methods = mapping.getMethodsCondition().getMethods();
              if (methods.contains(RequestMethod.POST)) {
                assertThat(handler.getMethod().getReturnType()).isEqualTo(ResponseEntity.class);
                assertThat(handler.getMethodAnnotation(ResponseStatus.class).value().value())
                    .isEqualTo(201);
              }
              if (methods.contains(RequestMethod.DELETE)) {
                assertThat(handler.getMethodAnnotation(ResponseStatus.class).value().value())
                    .isEqualTo(204);
              }
            });
  }

  @Test
  void deveIncluirNomesNoSnapshotQuandoContratoDeRelatorioForPublicado() throws Exception {
    String body =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Map<String, Object> properties =
        JsonPath.read(body, "$.components.schemas.ResultadoPlantioResposta.properties");
    assertThat(properties).containsKeys("culturaNome", "propriedadeNome");
    for (Class<?> dto :
        List.of(
            ResultadoPlantioResposta.class,
            ConteudoRelatorioResposta.class,
            EmissaoRelatorioResposta.class)) {
      String example = dto.getAnnotation(Schema.class).example();
      String prefix =
          dto == ResultadoPlantioResposta.class
              ? "$"
              : dto == ConteudoRelatorioResposta.class ? "$.plantios[0]" : "$.conteudo.plantios[0]";
      assertThat((String) JsonPath.read(example, prefix + ".culturaNome"))
          .isEqualTo("Cultura Fictícia");
      assertThat((String) JsonPath.read(example, prefix + ".propriedadeNome"))
          .isEqualTo("Propriedade Fictícia");
    }
  }

  @Test
  void devePublicarExemplosValidosQuandoSwaggerForGerado() {
    for (Class<?> dto :
        List.of(
            DespesaResposta.class,
            DespesaPlantioResposta.class,
            ConteudoRelatorioResposta.class,
            EmissaoRelatorioResposta.class)) {
      String example = dto.getAnnotation(Schema.class).example();
      String prefix =
          dto == EmissaoRelatorioResposta.class
              ? "$.conteudo.despesas[0]"
              : dto == ConteudoRelatorioResposta.class ? "$.despesas[0]" : "$";
      assertThat((Boolean) JsonPath.read(example, prefix + ".estornada")).isFalse();
    }
    String example = ProdutorRequisicao.class.getAnnotation(Schema.class).example();
    assertThat((String) JsonPath.read(example, "$.documento")).isEqualTo("99900000188");
  }

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
