package com.agrogestao.api.repository;

import com.agrogestao.api.model.CategoriaDespesa;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesa, Long> {
  boolean existsByNome(String nome);

  boolean existsByNomeAndIdNot(String nome, Long id);

  Page<CategoriaDespesa> findByNomeContainingIgnoreCase(String busca, Pageable pageable);
}
