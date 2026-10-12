package com.agrogestao.api.repository;

import com.agrogestao.api.model.Cultura;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CulturaRepository extends JpaRepository<Cultura, Long> {
  boolean existsByNome(String nome);

  boolean existsByNomeAndIdNot(String nome, Long id);

  Page<Cultura> findByNomeContainingIgnoreCase(String busca, Pageable pageable);
}
