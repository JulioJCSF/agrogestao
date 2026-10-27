package com.agrogestao.api.repository;

import com.agrogestao.api.model.Diaria;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiariaRepository extends JpaRepository<Diaria, Long> {
  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);

  /** Maior data do fato entre as diárias do plantio; nula se não houver (C06b). */
  @Query("select max(d.dataFato) from Diaria d where d.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);
}
