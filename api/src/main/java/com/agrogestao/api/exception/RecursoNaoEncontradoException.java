package com.agrogestao.api.exception;

/**
 * Recurso inexistente. Responde 404 {@code NAO_ENCONTRADO}; a mensagem é exibida ao operador, por
 * exemplo "Plantio não encontrado.".
 */
public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
