package com.agrogestao.api.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrogestao.api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ErroTesteController.class)
@Import({
  SecurityConfig.class, 
  JsonPontoDeEntradaDeAutenticacao.class, 
  JsonAcessoNegadoHandler.class
})
class ApiExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  private static void assertFormatoErro(
      ResultActions resultado, int status, String erro, String mensagem) throws Exception {
    resultado
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.timestamp").value(notNullValue()))
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.erro").value(erro))
        .andExpect(jsonPath("$.mensagem").value(mensagem))
        .andExpect(jsonPath("$.campos").isArray());
  }

  @Test
  @WithMockUser
  void deveRetornar422RegraNegocioQuandoRegraNegocioException() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/regra-negocio"));

    assertFormatoErro(
        resultado, 422, "REGRA_NEGOCIO", "Não é possível lançar despesa em plantio encerrado.");
    resultado.andExpect(jsonPath("$.campos", hasSize(0)));
  }

  @Test
  @WithMockUser
  void deveRetornar404NaoEncontradoQuandoRecursoNaoEncontradoException() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/nao-encontrado"));

    assertFormatoErro(resultado, 404, "NAO_ENCONTRADO", "Plantio não encontrado.");
  }

  @Test
  @WithMockUser
  void deveRetornar400ComCamposQuandoBeanValidationFalhar() throws Exception {
    ResultActions resultado =
        mockMvc.perform(
            post("/teste-erro/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\": \"\", \"dataDespesa\": null}"));

    assertFormatoErro(resultado, 400, "VALIDACAO", ApiExceptionHandler.MENSAGEM_VALIDACAO);
    resultado
        .andExpect(jsonPath("$.campos", hasSize(2)))
        .andExpect(
            jsonPath("$.campos[?(@.campo == 'descricao')].mensagem")
                .value("não deve estar em branco"))
        .andExpect(
            jsonPath("$.campos[?(@.campo == 'dataDespesa')].mensagem").value("não deve ser nulo"));
  }

  @Test
  @WithMockUser
  void deveRetornar200QuandoCorpoValido() throws Exception {
    mockMvc
        .perform(
            post("/teste-erro/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\": \"Adubo\", \"dataDespesa\": \"2026-09-13\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void deveRetornar400QuandoJsonMalformado() throws Exception {
    ResultActions resultado =
        mockMvc.perform(
            post("/teste-erro/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\": "));

    assertFormatoErro(resultado, 400, "VALIDACAO", ApiExceptionHandler.MENSAGEM_CORPO_INVALIDO);
    resultado.andExpect(jsonPath("$.campos", hasSize(0)));
  }

  @Test
  @WithMockUser
  void deveRetornar400ComCampoQuandoTipoInvalidoNoCorpo() throws Exception {
    ResultActions resultado =
        mockMvc.perform(
            post("/teste-erro/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\": \"Adubo\", \"dataDespesa\": \"ontem\"}"));

    assertFormatoErro(resultado, 400, "VALIDACAO", ApiExceptionHandler.MENSAGEM_VALIDACAO);
    resultado
        .andExpect(jsonPath("$.campos", hasSize(1)))
        .andExpect(jsonPath("$.campos[0].campo").value("dataDespesa"))
        .andExpect(
            jsonPath("$.campos[0].mensagem").value(ApiExceptionHandler.MENSAGEM_VALOR_INVALIDO));
  }

  @Test
  @WithMockUser
  void deveRetornar400ComCampoQuandoTipoInvalidoNoPath() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/plantios/abc"));

    assertFormatoErro(resultado, 400, "VALIDACAO", ApiExceptionHandler.MENSAGEM_VALIDACAO);
    resultado
        .andExpect(jsonPath("$.campos", hasSize(1)))
        .andExpect(jsonPath("$.campos[0].campo").value("id"));
  }

  @Test
  @WithMockUser
  void deveRetornar422SemDetalheTecnicoQuandoViolarConstraintDoBanco() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/constraint-banco"));

    assertFormatoErro(
        resultado, 422, "REGRA_NEGOCIO", ApiExceptionHandler.MENSAGEM_CONSTRAINT_BANCO);
    resultado
        .andExpect(content().string(not(containsString("uk_plantio_codigo"))))
        .andExpect(content().string(not(containsString("duplicate key"))));
  }

  @Test
  @WithMockUser
  void deveRetornar500SemStackTraceQuandoErroNaoTratado() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/erro-interno"));

    assertFormatoErro(resultado, 500, "ERRO_INTERNO", ApiExceptionHandler.MENSAGEM_ERRO_INTERNO);
    resultado
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(content().string(not(containsString("IllegalStateException"))))
        .andExpect(content().string(not(containsString("com.agrogestao"))));
  }

  @Test
  void deveRetornar401NaoAutenticadoQuandoSemUsuario() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/regra-negocio"));

    assertFormatoErro(resultado, 401, "NAO_AUTENTICADO", JsonPontoDeEntradaDeAutenticacao.MENSAGEM);
  }

  @Test
  @WithMockUser
  void deveRetornar403AcessoNegadoQuandoAccessDeniedException() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/teste-erro/acesso-negado"));

    assertFormatoErro(resultado, 403, "ACESSO_NEGADO", JsonAcessoNegadoHandler.MENSAGEM);
  }

  @Test
  @WithMockUser
  void deveRetornar404NaoEncontradoQuandoRotaInexistente() throws Exception {
    ResultActions resultado = mockMvc.perform(get("/rota-que-nao-existe"));

    assertFormatoErro(
        resultado, 404, "NAO_ENCONTRADO", ApiExceptionHandler.MENSAGEM_NAO_ENCONTRADO);
  }
}
