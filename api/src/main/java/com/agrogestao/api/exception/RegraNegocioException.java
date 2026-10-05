package com.agrogestao.api.exception;

import com.agrogestao.api.dto.ErroResposta.CampoErro;
import java.util.List;

/**
 * Violação de regra de negócio. Responde 422 {@code REGRA_NEGOCIO}; a mensagem é exibida ao
 * operador.
 */
public class RegraNegocioException extends RuntimeException {

  private final List<CampoErro> campos;

  public RegraNegocioException(String mensagem) {
    this(mensagem, List.of());
  }

  public RegraNegocioException(String mensagem, List<CampoErro> campos) {
    super(mensagem);
    this.campos = campos == null ? List.of() : List.copyOf(campos);
  }

  public List<CampoErro> getCampos() {
    return campos;
  }
}
