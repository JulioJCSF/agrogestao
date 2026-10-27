package com.agrogestao.api.repository;

import com.agrogestao.api.model.Producao;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);

  /** Maior data do fato entre as produções do plantio; nula se não houver (C06b). */
  @Query("select max(p.dataFato) from Producao p where p.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);
}
