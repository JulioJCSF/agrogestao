package com.agrogestao.api.repository;

import com.agrogestao.api.model.Despesa;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespesaRepository
    extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {
  Optional<Despesa> findByDiariaId(Long diariaId);

  boolean existsByCategoriaDespesaId(Long categoriaDespesaId);

  boolean existsByProdutorId(Long produtorId);

  /**
   * Maior data do fato entre as despesas rateadas ao plantio, estornadas inclusive (o estorno
   * preserva o registro, RN11); nula se não houver (C06b).
   */
  @Query("select max(d.dataFato) from Despesa d join d.rateios r where r.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);

  /**
   * Despesas do produtor com data do fato no período, estornadas inclusive, com os rateios gravados
   * na mesma consulta. Tirar a estornada da lista do relatório é da emissão (A03a).
   */
  @Query(
      """
      select distinct d from Despesa d left join fetch d.rateios
      where d.produtor.id = :produtorId
        and d.dataFato between :inicio and :fim
      order by d.id
      """)
  List<Despesa> buscarDoRelatorio(
      @Param("produtorId") Long produtorId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);
}
