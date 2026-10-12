package com.agrogestao.api.repository;

import com.agrogestao.api.model.Produtor;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutorRepository extends JpaRepository<Produtor, Long> {
  Optional<Produtor> findByDocumento(String documento);
}
