package com.agrogestao.api.exception;

/**
 * Falha de login. Responde 401 {@code NAO_AUTENTICADO} sempre com a mesma mensagem, para não
 * revelar se o login existe, se a senha está errada ou se o usuário está inativo.
 */
public class CredenciaisInvalidasException extends RuntimeException {

  public static final String MENSAGEM = "Login ou senha inválidos.";

  public CredenciaisInvalidasException() {
    super(MENSAGEM);
  }
}
