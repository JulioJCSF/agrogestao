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
@Table(name = "rateio_despesa")
public class RateioDespesa extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "despesa_id", nullable = false)
  private Despesa despesa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantio_id", nullable = false)
  private Plantio plantio;

  @Column(name = "percentual", precision = 9, scale = 6, nullable = false)
  private BigDecimal percentual;

  @Column(name = "valor_rateado", precision = 12, scale = 2, nullable = false)
  private BigDecimal valorRateado;
}
