package com.agrogestao.api.repository;

import com.agrogestao.api.model.Cliente;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
  Page<Cliente> findByNomeContainingIgnoreCase(String busca, Pageable pageable);
}
