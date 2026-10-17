package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cultura")
public class Cultura extends EntidadeAuditada {
  @Column(name = "nome", length = 100, nullable = false)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_ciclo_padrao", length = 10, nullable = false)
  private TipoCiclo tipoCicloPadrao;

  @Column(name = "unidade_medida", length = 10, nullable = false)
  private String unidadeMedida;
}
