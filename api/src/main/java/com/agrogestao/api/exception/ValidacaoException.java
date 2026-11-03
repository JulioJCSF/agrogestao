package com.agrogestao.api.exception;

import com.agrogestao.api.dto.ErroResposta.CampoErro;
import java.util.List;

/**
 * Entrada inválida que o Bean Validation não alcança, como o limite de bytes da senha. Responde 400
 * {@code VALIDACAO}, no mesmo formato da validação do DTO.
 */
public class ValidacaoException extends RuntimeException {

  private final List<CampoErro> campos;

  public ValidacaoException(String mensagem, List<CampoErro> campos) {
    super(mensagem);
    this.campos = List.copyOf(campos);
  }

  public List<CampoErro> getCampos() {
    return campos;
  }
}
