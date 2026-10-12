package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "emissao_relatorio")
public class EmissaoRelatorio extends EntidadeAuditada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produtor_id", nullable = false)
  private Produtor produtor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "data_emissao", nullable = false)
  private Instant dataEmissao;

  @Column(name = "periodo_inicio", nullable = false)
  private LocalDate periodoInicio;

  @Column(name = "periodo_fim", nullable = false)
  private LocalDate periodoFim;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String conteudo;
}
