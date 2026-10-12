package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "item_venda")
public class ItemVenda extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "venda_id", nullable = false)
  private Venda venda;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantio_id", nullable = false)
  private Plantio plantio;

  @Column(name = "quantidade", precision = 12, scale = 3, nullable = false)
  private BigDecimal quantidade;

  @Column(name = "unidade_medida", length = 10, nullable = false)
  private String unidadeMedida;

  @Column(name = "valor_unitario", precision = 12, scale = 2, nullable = false)
  private BigDecimal valorUnitario;

  @Column(name = "valor_total", precision = 12, scale = 2, nullable = false)
  private BigDecimal valorTotal;
}
