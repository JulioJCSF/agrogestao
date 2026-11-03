package com.agrogestao.api.repository;

import com.agrogestao.api.model.Propriedade;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {
  Page<Propriedade> findByProdutorId(Long produtorId, Pageable pageable);

  boolean existsByProdutorId(Long produtorId);

  List<Propriedade> findAllByProdutorIdOrderById(Long produtorId);
}
