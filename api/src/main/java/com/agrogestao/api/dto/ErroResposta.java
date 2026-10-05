package com.agrogestao.api.dto;

import com.agrogestao.api.exception.CodigoErro;
import java.time.Instant;
import java.util.List;

/**
 * Formato único de toda resposta de erro da API (padrões técnicos, seção 2.4). A {@code mensagem} é
 * exibida ao operador e deve estar em português claro (RNF03).
 */
public record ErroResposta(
    Instant timestamp, int status, String erro, String mensagem, List<CampoErro> campos) {

  public record CampoErro(String campo, String mensagem) {}

  public static ErroResposta de(CodigoErro codigo, String mensagem) {
    return de(codigo, mensagem, List.of());
  }

  public static ErroResposta de(CodigoErro codigo, String mensagem, List<CampoErro> campos) {
    return new ErroResposta(
        Instant.now(),
        codigo.getStatus().value(),
        codigo.name(),
        mensagem,
        campos == null ? List.of() : List.copyOf(campos));
  }
}
