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
@Table(name = "propriedade")
public class Propriedade extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produtor_id", nullable = false)
  private Produtor produtor;

  @Column(name = "nome", length = 150, nullable = false)
  private String nome;

  @Column(name = "area_total", precision = 12, scale = 3, nullable = true)
  private BigDecimal areaTotal;

  @Column(name = "localizacao", length = 255, nullable = true)
  private String localizacao;
}
