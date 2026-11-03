package com.agrogestao.api.repository;

import com.agrogestao.api.model.Diaria;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiariaRepository extends JpaRepository<Diaria, Long> {
  Page<Diaria> findByPlantioId(Long plantioId, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Diaria d where d.id = :id")
  Optional<Diaria> buscarParaAtualizar(@Param("id") Long id);

  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);

  /** Maior data do fato entre as diárias do plantio; nula se não houver (C06b). */
  @Query("select max(d.dataFato) from Diaria d where d.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);
}
