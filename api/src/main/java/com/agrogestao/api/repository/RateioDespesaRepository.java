package com.agrogestao.api.repository;

import com.agrogestao.api.model.RateioDespesa;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RateioDespesaRepository
    extends JpaRepository<RateioDespesa, Long>, JpaSpecificationExecutor<RateioDespesa> {
  /**
   * Todos os rateios do plantio com a despesa, estornadas e fora de qualquer período inclusive: o
   * recorte é da apuração (A01), não da consulta.
   */
  @Query("select r from RateioDespesa r join fetch r.despesa where r.plantio.id = :plantioId")
  List<RateioDespesa> buscarDoPlantioComDespesa(@Param("plantioId") Long plantioId);

  /** Rateios gravados das despesas de uma página da lista, numa consulta só (L04b). */
  @Query("select r from RateioDespesa r where r.despesa.id in :despesaIds")
  List<RateioDespesa> buscarDasDespesas(@Param("despesaIds") Collection<Long> despesaIds);
}
