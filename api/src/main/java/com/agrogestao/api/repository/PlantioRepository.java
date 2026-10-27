package com.agrogestao.api.repository;

import com.agrogestao.api.model.Plantio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlantioRepository
    extends JpaRepository<Plantio, Long>, JpaSpecificationExecutor<Plantio> {}
