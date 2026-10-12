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
@Table(name = "plantio")
public class Plantio extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "propriedade_id", nullable = false)
  private Propriedade propriedade;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cultura_id", nullable = false)
  private Cultura cultura;

  @Column(name = "area", precision = 12, scale = 3, nullable = false)
  private BigDecimal area;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_ciclo", length = 10, nullable = false)
  private TipoCiclo tipoCiclo;

  @Column(name = "data_inicio", nullable = false)
  private LocalDate dataInicio;

  @Column(name = "previsao_colheita", nullable = true)
  private LocalDate previsaoColheita;

  @Column(name = "data_encerramento", nullable = true)
  private LocalDate dataEncerramento;
}
