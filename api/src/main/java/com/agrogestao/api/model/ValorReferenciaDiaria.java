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
@Table(name = "valor_referencia_diaria")
public class ValorReferenciaDiaria extends EntidadeAuditada {
  @Column(name = "valor", precision = 12, scale = 2, nullable = false)
  private BigDecimal valor;

  @Column(name = "vigencia_inicio", nullable = false)
  private LocalDate vigenciaInicio;

  @Column(name = "vigencia_fim", nullable = true)
  private LocalDate vigenciaFim;
}
