package com.agrogestao.api.repository;

import com.agrogestao.api.model.Diaria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiariaRepository extends JpaRepository<Diaria, Long> {
  boolean existsByPlantioPropriedadeProdutorId(Long produtorId);
}
