package com.agrogestao.api.repository;

import com.agrogestao.api.model.EmissaoRelatorio;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmissaoRelatorioRepository extends JpaRepository<EmissaoRelatorio, Long> {
  boolean existsByProdutorId(Long produtorId);

  Page<EmissaoRelatorio> findByProdutorId(Long produtorId, Pageable pageable);

  @Query(
      "select count(e) > 0 from EmissaoRelatorio e where e.produtor.id = :produtorId and :dataFato between e.periodoInicio and e.periodoFim")
  boolean existeEmissaoNoPeriodo(
      @Param("produtorId") Long produtorId, @Param("dataFato") LocalDate dataFato);
}
