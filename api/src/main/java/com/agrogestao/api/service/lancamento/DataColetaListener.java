package com.agrogestao.api.service.lancamento;

import com.agrogestao.api.model.Lancamento;
import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;

/**
 * Listener JPA das quatro entidades de lançamento. O Hibernate o cria pelo contêiner do Spring
 * ({@code SpringBeanContainer}), então recebe o mesmo {@code Clock} da aplicação (LancamentoIT).
 */
@RequiredArgsConstructor
public class DataColetaListener {
  private final LancamentoService lancamentos;

  @PrePersist
  void antesDeGravar(Object entidade) {
    lancamentos.atribuirDataColeta((Lancamento) entidade);
  }
}
