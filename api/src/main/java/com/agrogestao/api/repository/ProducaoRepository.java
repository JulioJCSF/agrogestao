package com.agrogestao.api.repository;

import com.agrogestao.api.model.Producao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);
}
