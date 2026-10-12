package com.agrogestao.api.repository;

import com.agrogestao.api.model.Producao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {}
