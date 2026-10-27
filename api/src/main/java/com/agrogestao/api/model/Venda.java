package com.agrogestao.api.model;

import com.agrogestao.api.service.lancamento.DataColetaListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venda")
@EntityListeners(DataColetaListener.class)
public class Venda extends EntidadeAuditada implements Lancamento {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produtor_id", nullable = false)
  private Produtor produtor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @Column(name = "valor_total", precision = 12, scale = 2, nullable = false)
  private BigDecimal valorTotal;

  @Column(name = "data_fato", nullable = false)
  private LocalDate dataFato;

  @Column(name = "data_coleta", nullable = false, updatable = false)
  private LocalDate dataColeta;

  @Transient private boolean coletaAtribuida;

  @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemVenda> itens = new ArrayList<>();
}
