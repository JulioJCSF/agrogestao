package com.agrogestao.api.repository;

import com.agrogestao.api.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByLogin(String login);

  @Query(
      "select new com.agrogestao.api.repository.SituacaoUsuario(u.perfil, u.ativo)"
          + " from Usuario u where u.id = :id")
  Optional<SituacaoUsuario> findSituacaoById(@Param("id") Long id);
}
