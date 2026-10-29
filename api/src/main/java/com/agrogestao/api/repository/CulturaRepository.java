package com.agrogestao.api.repository;

import com.agrogestao.api.model.Cultura;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CulturaRepository extends JpaRepository<Cultura, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cultura c where c.id = :id")
  Optional<Cultura> buscarParaAtualizar(@Param("id") Long id);

  boolean existsByNomeIgnoreCase(String nome);

  boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

  Page<Cultura> findByNomeContainingIgnoreCase(String busca, Pageable pageable);
}
