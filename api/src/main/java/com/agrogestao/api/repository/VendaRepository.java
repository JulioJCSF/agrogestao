package com.agrogestao.api.repository;

import com.agrogestao.api.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaRepository extends JpaRepository<Venda, Long> {
  boolean existsByProdutorId(Long produtorId);
}
