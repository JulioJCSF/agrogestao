package com.agrogestao.api.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "produtor")
public class Produtor extends EntidadeAuditada {
  @Column(name = "nome", length = 150, nullable = false)
  private String nome;

  @Column(name = "documento", columnDefinition = "char(11)", nullable = false, unique = true)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  private String documento;

  @Column(name = "endereco", length = 255, nullable = true)
  private String endereco;

  @Column(name = "profissao", length = 100, nullable = true)
  private String profissao;

  @Column(name = "contato", length = 100, nullable = true)
  private String contato;

  @Column(name = "consentimento_em", nullable = true)
  private LocalDate consentimentoEm;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;
}
