package com.agrogestao.api.repository;

import com.agrogestao.api.model.ItemVenda;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {
  /** Todos os itens de venda do plantio com a venda; o recorte por período é da apuração (A01). */
  @Query("select i from ItemVenda i join fetch i.venda where i.plantio.id = :plantioId")
  List<ItemVenda> buscarDoPlantioComVenda(@Param("plantioId") Long plantioId);
}
