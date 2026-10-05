package com.agrogestao.api.exception;

import com.agrogestao.api.dto.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde 401 {@code NAO_AUTENTICADO} no formato único. O erro acontece nos filtros do Spring
 * Security, fora do alcance do {@link ApiExceptionHandler}.
 */
@Component
public class JsonPontoDeEntradaDeAutenticacao implements AuthenticationEntryPoint {

  static final String MENSAGEM = "Você não está autenticado. Faça login para continuar.";

  private final JsonMapper jsonMapper;

  public JsonPontoDeEntradaDeAutenticacao(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    ErroResposta erro = ErroResposta.de(CodigoErro.NAO_AUTENTICADO, MENSAGEM);
    response.setStatus(erro.status());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(response.getOutputStream(), erro);
  }
}
