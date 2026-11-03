package com.agrogestao.api.repository;

import com.agrogestao.api.model.Diaria;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiariaRepository extends JpaRepository<Diaria, Long> {
  Page<Diaria> findByPlantioId(Long plantioId, Pageable pageable);

  /** Todos os registros do plantio, sem paginação; o recorte por período é da apuração (A01). */
  List<Diaria> findAllByPlantioId(Long plantioId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Diaria d where d.id = :id")
  Optional<Diaria> buscarParaAtualizar(@Param("id") Long id);

  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);

  /** Maior data do fato entre as diárias do plantio; nula se não houver (C06b). */
  @Query("select max(d.dataFato) from Diaria d where d.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);

  /** Diárias pagas e familiares dos plantios do produtor com data do fato no período (A03a). */
  @Query(
      """
      select d from Diaria d
      where d.plantio.propriedade.produtor.id = :produtorId
        and d.dataFato between :inicio and :fim
      order by d.id
      """)
  List<Diaria> buscarDoRelatorio(
      @Param("produtorId") Long produtorId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);
}
