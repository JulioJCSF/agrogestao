package com.agrogestao.api.exception;

import org.springframework.http.HttpStatus;

/** Códigos do campo {@code erro} da resposta de erro (padrões técnicos, seção 2.4). */
public enum CodigoErro {
  VALIDACAO(HttpStatus.BAD_REQUEST),
  NAO_AUTENTICADO(HttpStatus.UNAUTHORIZED),
  ACESSO_NEGADO(HttpStatus.FORBIDDEN),
  NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
  REGRA_NEGOCIO(HttpStatus.UNPROCESSABLE_CONTENT),
  ERRO_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

  CodigoErro(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
