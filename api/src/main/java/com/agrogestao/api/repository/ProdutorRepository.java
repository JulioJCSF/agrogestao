package com.agrogestao.api.repository;

import com.agrogestao.api.model.Produtor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutorRepository extends JpaRepository<Produtor, Long> {
  Optional<Produtor> findByDocumento(String documento);

  /**
   * Trava o produtor (A03b): emissão e operações sobre lançamento de período emitido passam por
   * aqui antes de qualquer plantio.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Produtor p where p.id = :id")
  Optional<Produtor> buscarParaAtualizar(@Param("id") Long id);

  boolean existsByDocumento(String documento);

  boolean existsByDocumentoAndIdNot(String documento, Long id);

  /** Busca por parte do nome ou, quando há dígitos na busca, por parte do CPF (RF08). */
  @Query(
      """
      select p from Produtor p
      where lower(p.nome) like lower(concat('%', :busca, '%'))
         or (:digitos <> '' and p.documento like concat('%', :digitos, '%'))
      """)
  Page<Produtor> buscar(
      @Param("busca") String busca, @Param("digitos") String digitos, Pageable pageable);
}
