package com.agrogestao.api.repository;

import com.agrogestao.api.model.CategoriaDespesa;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesa, Long> {
  Optional<CategoriaDespesa> findByNomeIgnoreCaseAndSistemaTrue(String nome);

  boolean existsByNomeIgnoreCase(String nome);

  boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

  Page<CategoriaDespesa> findByNomeContainingIgnoreCase(String busca, Pageable pageable);
}
