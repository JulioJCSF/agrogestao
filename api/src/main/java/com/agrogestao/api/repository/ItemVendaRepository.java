package com.agrogestao.api.repository;

import com.agrogestao.api.model.ItemVenda;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {
  /** Todos os itens de venda do plantio com a venda; o recorte por período é da apuração (A01). */
  @Query("select i from ItemVenda i join fetch i.venda where i.plantio.id = :plantioId")
  List<ItemVenda> buscarDoPlantioComVenda(@Param("plantioId") Long plantioId);

  /** Itens das vendas de uma página da lista, em ordem de id, numa consulta só (L09b). */
  @Query("select i from ItemVenda i where i.venda.id in :vendaIds order by i.id")
  List<ItemVenda> buscarDasVendas(@Param("vendaIds") Collection<Long> vendaIds);

  /** Quantidade vendida acumulada de cada plantio, de todas as vendas (RN29). */
  @Query(
      "select i.plantio.id as plantioId, sum(i.quantidade) as total from ItemVenda i"
          + " where i.plantio.id in :plantioIds group by i.plantio.id")
  List<TotalPorPlantio> somarQuantidadePorPlantio(@Param("plantioIds") Collection<Long> plantioIds);

  boolean existsByPlantioId(Long plantioId);

  boolean existsByPlantioCulturaId(Long culturaId);
}
