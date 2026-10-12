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
@Table(name = "producao")
public class Producao extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantio_id", nullable = false)
  private Plantio plantio;

  @Column(name = "quantidade", precision = 12, scale = 3, nullable = false)
  private BigDecimal quantidade;

  @Column(name = "unidade_medida", length = 10, nullable = false)
  private String unidadeMedida;

  @Column(name = "data_fato", nullable = false)
  private LocalDate dataFato;

  @Column(name = "data_coleta", nullable = false)
  private LocalDate dataColeta;
}
