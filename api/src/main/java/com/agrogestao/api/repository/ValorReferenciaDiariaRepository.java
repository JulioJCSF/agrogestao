package com.agrogestao.api.repository;

import com.agrogestao.api.model.ValorReferenciaDiaria;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValorReferenciaDiariaRepository
    extends JpaRepository<ValorReferenciaDiaria, Long> {
  @Query(
      """
      select count(r) > 0 from ValorReferenciaDiaria r
      where r.id <> :ignorarId
        and r.vigenciaInicio <= coalesce(:fim, r.vigenciaInicio)
        and (r.vigenciaFim is null or r.vigenciaFim >= :inicio)
      """)
  boolean existeSobreposicao(
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim,
      @Param("ignorarId") Long ignorarId);

  @Query(
      """
      select r from ValorReferenciaDiaria r where r.vigenciaInicio <= :data
        and (r.vigenciaFim is null or r.vigenciaFim >= :data)
      """)
  Optional<ValorReferenciaDiaria> buscarVigente(@Param("data") LocalDate data);
}
