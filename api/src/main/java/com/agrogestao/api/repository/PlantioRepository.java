package com.agrogestao.api.repository;

import com.agrogestao.api.model.Plantio;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantioRepository extends JpaRepository<Plantio, Long> {}
