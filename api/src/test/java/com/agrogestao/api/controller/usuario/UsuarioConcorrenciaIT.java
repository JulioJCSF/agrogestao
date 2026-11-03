package com.agrogestao.api.controller.usuario;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.dto.UsuarioAlteracaoRequisicao;
import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.repository.UsuarioRepository;
import com.agrogestao.api.service.usuario.UsuarioService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Último ADMIN ativo (decisão de 03/11, M01): dois admins, os únicos ativos, se desativando em
 * paralelo. No máximo um consegue e sempre sobra um ADMIN ativo.
 */
class UsuarioConcorrenciaIT extends IntegracaoTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired UsuarioService usuarios;
  @Autowired EntityManager em;
  @MockitoSpyBean UsuarioRepository repository;
  List<Long> outrosAdmins;
  Long primeiro, segundo;

  @BeforeEach
  void preparar() {
    primeiro = admin("admin-um-teste-m01");
    segundo = admin("admin-dois-teste-m01");
    outrosAdmins =
        jdbc.queryForList(
            "select id from usuario where perfil = 'ADMIN' and ativo and id not in (?,?)",
            Long.class,
            primeiro,
            segundo);
    for (Long id : outrosAdmins) jdbc.update("update usuario set ativo = false where id = ?", id);
  }

  @AfterEach
  void limpar() {
    for (Long id : outrosAdmins) jdbc.update("update usuario set ativo = true where id = ?", id);
    jdbc.update("delete from usuario where id in (?,?)", primeiro, segundo);
  }

  Long admin(String login) {
    return jdbc.queryForObject(
        "insert into usuario(nome,login,senha_hash,perfil) values ('Admin Teste M01',?,'hash-ficticio','ADMIN') returning id",
        Long.class,
        login);
  }

  @Test
  void deveSobrarUmAdminAtivoQuandoDoisSeDesativaremEmParalelo() throws Exception {
    // As duas transações carregam o próprio usuário e só então seguem juntas para a contagem. O
    // spy do repositório é proxy de interface: a leitura real vem de outra instância dele.
    UsuarioRepository real = new JpaRepositoryFactory(em).getRepository(UsuarioRepository.class);
    CyclicBarrier carregados = new CyclicBarrier(2);
    doAnswer(
            inv -> {
              Object usuario = real.findById(inv.getArgument(0));
              carregados.await(10, TimeUnit.SECONDS);
              return usuario;
            })
        .when(repository)
        .findById(anyLong());
    UsuarioAlteracaoRequisicao desativar =
        new UsuarioAlteracaoRequisicao("Admin Teste M01", Perfil.ADMIN, false);
    List<Throwable> falhas = new ArrayList<>();
    int sucessos = 0;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<?>> tarefas =
          List.of(
              executor.submit(() -> usuarios.alterar(primeiro, desativar)),
              executor.submit(() -> usuarios.alterar(segundo, desativar)));
      for (Future<?> tarefa : tarefas)
        try {
          tarefa.get(20, TimeUnit.SECONDS);
          sucessos++;
        } catch (ExecutionException e) {
          falhas.add(e.getCause());
        }
    }
    assertThat(sucessos).isEqualTo(1);
    assertThat(falhas)
        .singleElement()
        .isInstanceOf(RegraNegocioException.class)
        .hasFieldOrPropertyWithValue(
            "message", "O sistema precisa de pelo menos um administrador ativo.");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where id in (?,?) and ativo",
                Integer.class,
                primeiro,
                segundo))
        .isEqualTo(1);
  }
}
