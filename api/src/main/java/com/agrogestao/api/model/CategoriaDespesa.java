package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categoria_despesa")
public class CategoriaDespesa extends EntidadeAuditada {
  @Column(name = "nome", length = 80, nullable = false)
  private String nome;

  @Column(name = "sistema", nullable = false)
  private boolean sistema;
}
