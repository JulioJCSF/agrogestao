package com.agrogestao.api.exception;

import com.agrogestao.api.dto.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde 403 {@code ACESSO_NEGADO} no formato único. O erro acontece nos filtros do Spring
 * Security, fora do alcance do {@link ApiExceptionHandler}.
 */
@Component
public class JsonAcessoNegadoHandler implements AccessDeniedHandler {

  static final String MENSAGEM = "Você não tem permissão para realizar esta operação.";

  private final JsonMapper jsonMapper;

  public JsonAcessoNegadoHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    ErroResposta erro = ErroResposta.de(CodigoErro.ACESSO_NEGADO, MENSAGEM);
    response.setStatus(erro.status());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(response.getOutputStream(), erro);
  }
}
