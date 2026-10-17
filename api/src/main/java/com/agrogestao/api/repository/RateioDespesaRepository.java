package com.agrogestao.api.repository;

import com.agrogestao.api.model.RateioDespesa;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateioDespesaRepository extends JpaRepository<RateioDespesa, Long> {}
