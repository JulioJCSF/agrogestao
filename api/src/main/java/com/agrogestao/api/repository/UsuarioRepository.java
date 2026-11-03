package com.agrogestao.api.repository;

import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.Usuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByLogin(String login);

  /** Login sem diferenciar caixa (decisão de 03/11, M01). */
  Optional<Usuario> findByLoginIgnoreCase(String login);

  boolean existsByLoginIgnoreCase(String login);

  /**
   * Usuários ativos do perfil, com as linhas travadas ({@code for update}): duas alterações
   * concorrentes não deixam o sistema sem administrador ativo (M01).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<Usuario> findByPerfilAndAtivoTrue(Perfil perfil);

  @Query(
      "select new com.agrogestao.api.repository.SituacaoUsuario(u.perfil, u.ativo)"
          + " from Usuario u where u.id = :id")
  Optional<SituacaoUsuario> findSituacaoById(@Param("id") Long id);
}
