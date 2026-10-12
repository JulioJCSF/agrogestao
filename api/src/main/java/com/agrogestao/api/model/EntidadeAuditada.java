package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Auditoria compartilhada; Usuario é a raiz e não herda esta classe (RNF07). */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadeAuditada {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreatedBy
  @Column(name = "criado_por", nullable = false, updatable = false)
  private Long criadoPor;

  @CreatedDate
  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @LastModifiedDate
  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;
}
