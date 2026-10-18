package com.agrogestao.api.repository;

import com.agrogestao.api.model.Cultura;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CulturaRepository extends JpaRepository<Cultura, Long> {
  boolean existsByNomeIgnoreCase(String nome);

  boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

  Page<Cultura> findByNomeContainingIgnoreCase(String busca, Pageable pageable);
}
