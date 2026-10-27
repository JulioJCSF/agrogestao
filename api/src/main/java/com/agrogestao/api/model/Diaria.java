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
@Table(name = "diaria")
@EntityListeners(DataColetaListener.class)
public class Diaria extends EntidadeAuditada implements Lancamento {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantio_id", nullable = false)
  private Plantio plantio;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "valor_referencia_diaria_id", nullable = true)
  private ValorReferenciaDiaria valorReferenciaDiaria;

  @Enumerated(EnumType.STRING)
  @Column(name = "natureza", length = 10, nullable = false)
  private NaturezaDiaria natureza;

  @Column(name = "quantidade_dias", precision = 6, scale = 2, nullable = false)
  private BigDecimal quantidadeDias;

  @Column(name = "valor_unitario", precision = 12, scale = 2, nullable = false)
  private BigDecimal valorUnitario;

  @Column(name = "data_fato", nullable = false)
  private LocalDate dataFato;

  @Column(name = "data_coleta", nullable = false, updatable = false)
  private LocalDate dataColeta;

  @Transient private boolean coletaAtribuida;
}
