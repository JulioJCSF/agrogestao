package com.agrogestao.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Operador do sistema. É a raiz da cadeia de auditoria: não tem {@code criado_por} e, por isso, não
 * herda a base de auditoria das demais entidades.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, unique = true, length = 100)
  private String login;

  @Column(name = "senha_hash", nullable = false)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Perfil perfil;

  @Column(nullable = false)
  private boolean ativo = true;

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected Usuario() {}

  public Usuario(String nome, String login, String senhaHash, Perfil perfil, boolean ativo) {
    this.nome = nome;
    this.login = login;
    this.senhaHash = senhaHash;
    this.perfil = perfil;
    this.ativo = ativo;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getLogin() {
    return login;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public Perfil getPerfil() {
    return perfil;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}
