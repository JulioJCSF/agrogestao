package com.agrogestao.api.repository;

import com.agrogestao.api.model.Plantio;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlantioRepository
    extends JpaRepository<Plantio, Long>, JpaSpecificationExecutor<Plantio> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Plantio p where p.id = :id")
  Optional<Plantio> buscarParaAtualizar(@Param("id") Long id);

  /**
   * Todos os plantios do produtor, em ordem de id, com cultura e propriedade na mesma consulta. O
   * recorte pelo período do relatório é da emissão (A03a).
   */
  @Query(
      """
      select p from Plantio p join fetch p.cultura join fetch p.propriedade pr
      where pr.produtor.id = :produtorId
      order by p.id
      """)
  List<Plantio> buscarDoProdutorComCultura(@Param("produtorId") Long produtorId);
}
