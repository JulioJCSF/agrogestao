package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cliente")
public class Cliente extends EntidadeAuditada {
  @Column(name = "nome", length = 150, nullable = false)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_canal", length = 30, nullable = false)
  private TipoCanal tipoCanal;
}
