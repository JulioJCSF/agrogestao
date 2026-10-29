package com.agrogestao.api.repository;

import com.agrogestao.api.model.Despesa;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
  Optional<Despesa> findByDiariaId(Long diariaId);

  boolean existsByCategoriaDespesaId(Long categoriaDespesaId);

  boolean existsByProdutorId(Long produtorId);

  /**
   * Maior data do fato entre as despesas rateadas ao plantio, estornadas inclusive (o estorno
   * preserva o registro, RN11); nula se não houver (C06b).
   */
  @Query("select max(d.dataFato) from Despesa d join d.rateios r where r.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);
}
