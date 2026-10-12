package com.agrogestao.api.repository;

import com.agrogestao.api.model.Despesa;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {}
