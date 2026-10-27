package com.agrogestao.api.repository;

import com.agrogestao.api.model.Venda;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendaRepository extends JpaRepository<Venda, Long> {
  boolean existsByProdutorId(Long produtorId);

  /** Maior data do fato entre as vendas com item no plantio; nula se não houver (C06b). */
  @Query("select max(v.dataFato) from Venda v join v.itens i where i.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);
}
