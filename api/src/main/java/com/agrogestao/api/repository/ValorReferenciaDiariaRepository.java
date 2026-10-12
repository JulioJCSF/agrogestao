package com.agrogestao.api.repository;

import com.agrogestao.api.model.ValorReferenciaDiaria;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValorReferenciaDiariaRepository
    extends JpaRepository<ValorReferenciaDiaria, Long> {}
