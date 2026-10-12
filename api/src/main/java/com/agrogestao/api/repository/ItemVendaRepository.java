package com.agrogestao.api.repository;

import com.agrogestao.api.model.ItemVenda;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {}
