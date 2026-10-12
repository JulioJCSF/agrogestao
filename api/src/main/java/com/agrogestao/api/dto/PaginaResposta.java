package com.agrogestao.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResposta<T>(List<T> itens, int pagina, int tamanho, long total) {
  public static <T> PaginaResposta<T> de(Page<T> pagina) {
    return new PaginaResposta<>(
        pagina.getContent(), pagina.getNumber(), pagina.getSize(), pagina.getTotalElements());
  }
}
