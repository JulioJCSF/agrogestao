package com.agrogestao.api.repository;

import com.agrogestao.api.model.Producao;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
  boolean existsByPlantioId(Long plantioId);

  boolean existsByPlantioCulturaId(Long culturaId);

  Page<Producao> findByPlantioId(Long plantioId, Pageable pageable);

  /** Todos os registros dos plantios, sem paginação; o recorte por período é da apuração (A01). */
  @Query("select p from Producao p where p.plantio.id in :plantioIds")
  List<Producao> buscarDosPlantios(@Param("plantioIds") Collection<Long> plantioIds);

  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);

  /**
   * Só o produtor do plantio da produção, sem carregar a entidade: o produtor é travado antes da
   * produção, do plantio e da cultura (A03d).
   */
  @Query("select p.plantio.propriedade.produtor.id from Producao p where p.id = :id")
  Optional<Long> buscarProdutorId(@Param("id") Long id);

  /** Primeira produção do plantio: a unidade de referência do item de venda (RN30, L09b). */
  Optional<Producao> findFirstByPlantioIdOrderByIdAsc(Long plantioId);

  /** Quantidade produzida acumulada de cada plantio, sem recorte de período (RN29). */
  @Query(
      "select p.plantio.id as plantioId, sum(p.quantidade) as total from Producao p"
          + " where p.plantio.id in :plantioIds group by p.plantio.id")
  List<TotalPorPlantio> somarQuantidadePorPlantio(@Param("plantioIds") Collection<Long> plantioIds);

  /** Maior data do fato entre as produções do plantio; nula se não houver (C06b). */
  @Query("select max(p.dataFato) from Producao p where p.plantio.id = :plantioId")
  LocalDate ultimaDataFatoDoPlantio(@Param("plantioId") Long plantioId);
}
