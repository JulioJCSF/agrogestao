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
@Table(name = "despesa")
public class Despesa extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produtor_id", nullable = false)
  private Produtor produtor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categoria_despesa_id", nullable = false)
  private CategoriaDespesa categoriaDespesa;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "diaria_id", unique = true)
  private Diaria diaria;

  @Column(name = "descricao", length = 255, nullable = false)
  private String descricao;

  @Column(name = "valor_total", precision = 12, scale = 2, nullable = false)
  private BigDecimal valorTotal;

  @Column(name = "data_fato", nullable = false)
  private LocalDate dataFato;

  @Column(name = "data_coleta", nullable = false)
  private LocalDate dataColeta;

  @Column(name = "estornada", nullable = false)
  private boolean estornada;

  @OneToMany(mappedBy = "despesa", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RateioDespesa> rateios = new ArrayList<>();
}
