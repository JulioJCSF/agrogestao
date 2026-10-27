package com.agrogestao.api.model;

import java.time.LocalDate;

/**
 * Evento com data do fato e data de coleta (RF17): produção, despesa, diária e venda.
 *
 * <p>{@code coletaAtribuida} é transitória e só deve ser mexida pelo {@code LancamentoService}: diz
 * que a data de coleta veio da base (atribuída ou herdada), e não de um {@code setDataColeta}
 * solto.
 */
public interface Lancamento {
  LocalDate getDataFato();

  LocalDate getDataColeta();

  void setDataColeta(LocalDate dataColeta);

  boolean isColetaAtribuida();

  void setColetaAtribuida(boolean coletaAtribuida);
}
