package com.agrogestao.api.repository;

import com.agrogestao.api.model.EmissaoRelatorio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmissaoRelatorioRepository extends JpaRepository<EmissaoRelatorio, Long> {
  boolean existsByProdutorId(Long produtorId);
}
