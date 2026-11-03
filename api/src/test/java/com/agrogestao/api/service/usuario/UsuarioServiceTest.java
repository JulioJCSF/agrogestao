package com.agrogestao.api.service.usuario;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.dto.ErroResposta.CampoErro;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.Usuario;
import com.agrogestao.api.repository.UsuarioRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * M01 com repositório mockado e BCrypt real (custo mínimo); senhas de fixture claramente falsas.
 */
class UsuarioServiceTest {
  static final String SENHA = "senha-ficticia-m01";

  UsuarioRepository repository = mock(UsuarioRepository.class);
  PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));
  UsuarioService service = new UsuarioService(repository, encoder);
  Map<Long, Usuario> cadastrados = new HashMap<>();

  @BeforeEach
  void preparar() {
    when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(repository.findById(anyLong()))
        .thenAnswer(inv -> Optional.ofNullable(cadastrados.get(inv.<Long>getArgument(0))));
    when(repository.findByPerfilAndAtivoTrue(Perfil.ADMIN))
        .thenAnswer(
            inv ->
                cadastrados.values().stream()
                    .filter(u -> u.getPerfil() == Perfil.ADMIN && u.isAtivo())
                    .toList());
  }

  Usuario usuario(Long id, Perfil perfil, boolean ativo) {
    Usuario usuario =
        new Usuario("Usuário Fictício " + id, "usuario.ficticio." + id, "hash", perfil, ativo);
    ReflectionTestUtils.setField(usuario, "id", id);
    cadastrados.put(id, usuario);
    return usuario;
  }

  static UsuarioRequisicao novo(String login, String senha) {
    return new UsuarioRequisicao(" Operador Fictício ", login, senha, Perfil.LANCAMENTO);
  }

  static UsuarioAlteracaoRequisicao alteracao(Perfil perfil, boolean ativo) {
    return new UsuarioAlteracaoRequisicao("Nome Alterado Fictício", perfil, ativo);
  }

  void assertCampo(Throwable erro, String campo) {
    assertThat(
            erro instanceof ValidacaoException v
                ? v.getCampos()
                : ((RegraNegocioException) erro).getCampos())
        .extracting(CampoErro::campo)
        .containsExactly(campo);
  }

  @Test
  void deveGravarLoginAparadoEmMinusculasEHashQuandoCriar() {
    UsuarioResposta resposta = service.criar(novo("  Operador.Teste  ", SENHA));
    ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
    verify(repository).saveAndFlush(gravado.capture());
    assertThat(gravado.getValue().getLogin()).isEqualTo("operador.teste");
    assertThat(gravado.getValue().getNome()).isEqualTo("Operador Fictício");
    assertThat(gravado.getValue().isAtivo()).isTrue();
    verify(encoder).encode(SENHA);
    assertThat(gravado.getValue().getSenhaHash()).startsWith("$2").isNotEqualTo(SENHA);
    assertThat(encoder.matches(SENHA, gravado.getValue().getSenhaHash())).isTrue();
    assertThat(resposta.login()).isEqualTo("operador.teste");
    assertThat(resposta.toString()).doesNotContain(SENHA, gravado.getValue().getSenhaHash());
  }

  @Test
  void deveRecusarSemGravarQuandoLoginRepetirComOutraCaixa() {
    when(repository.existsByLoginIgnoreCase("operador.teste")).thenReturn(true);
    Throwable erro = catchThrowable(() -> service.criar(novo("OPERADOR.Teste", SENHA)));
    assertThat(erro)
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("Já existe um usuário com este login.");
    assertCampo(erro, "login");
    verify(repository, never()).saveAndFlush(any());
    verify(repository, never()).save(any());
  }

  @Test
  void deveAceitar72BytesERecusarMaisQuandoSenhaTiverAcentos() {
    assertThatNoException().isThrownBy(() -> service.criar(novo("ascii.72", "a".repeat(72))));
    // 40 "á" são 40 caracteres (passam no @Size) e 80 bytes em UTF-8: o BCrypt truncaria.
    Throwable erro = catchThrowable(() -> service.criar(novo("acentos.80", "á".repeat(40))));
    assertThat(erro)
        .isInstanceOf(ValidacaoException.class)
        .hasMessage("A senha deve ter no máximo 72 bytes.")
        .hasMessageNotContaining("áá");
    assertCampo(erro, "senha");
    verify(repository, times(1)).saveAndFlush(any());
  }

  @Test
  void deveRecusarSemGravarQuandoDesativarOUnicoAdminAtivo() {
    Usuario admin = usuario(1L, Perfil.ADMIN, true);
    usuario(2L, Perfil.ADMIN, false);
    assertThatThrownBy(() -> service.alterar(1L, alteracao(Perfil.ADMIN, false)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("O sistema precisa de pelo menos um administrador ativo.");
    assertThat(admin.isAtivo()).isTrue();
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void deveRecusarQuandoRebaixarOUnicoAdminAtivo() {
    Usuario admin = usuario(1L, Perfil.ADMIN, true);
    assertThatThrownBy(() -> service.alterar(1L, alteracao(Perfil.LANCAMENTO, true)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("O sistema precisa de pelo menos um administrador ativo.");
    assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void deveDesativarAdminQuandoHouverOutroAdminAtivo() {
    usuario(1L, Perfil.ADMIN, true);
    usuario(2L, Perfil.ADMIN, true);
    UsuarioResposta resposta = service.alterar(1L, alteracao(Perfil.ADMIN, false));
    assertThat(resposta.ativo()).isFalse();
    assertThat(resposta.login()).isEqualTo("usuario.ficticio.1");
    verify(repository).findByPerfilAndAtivoTrue(Perfil.ADMIN);
  }

  @Test
  void deveAlterarSemContarAdminsQuandoUsuarioNaoForAdminAtivo() {
    usuario(3L, Perfil.LANCAMENTO, true);
    assertThat(service.alterar(3L, alteracao(Perfil.CONSULTA, false)).perfil())
        .isEqualTo(Perfil.CONSULTA);
    verify(repository, never()).findByPerfilAndAtivoTrue(any());
  }

  @Test
  void deveResponder404QuandoUsuarioNaoExistir() {
    assertThatThrownBy(() -> service.alterar(9L, alteracao(Perfil.ADMIN, true)))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Usuário não encontrado.");
    assertThatThrownBy(() -> service.redefinirSenha(9L, new UsuarioSenhaRequisicao(SENHA)))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void deveGravarNovoHashQuandoRedefinirSenhaInclusiveDeInativo() {
    Usuario inativo = usuario(4L, Perfil.CONSULTA, false);
    inativo.redefinirSenha(encoder.encode("senha-antiga-ficticia"));
    String antigo = inativo.getSenhaHash();
    service.redefinirSenha(4L, new UsuarioSenhaRequisicao("senha-nova-ficticia"));
    assertThat(inativo.getSenhaHash()).isNotEqualTo(antigo);
    assertThat(encoder.matches("senha-nova-ficticia", inativo.getSenhaHash())).isTrue();
    assertThat(encoder.matches("senha-antiga-ficticia", inativo.getSenhaHash())).isFalse();
    assertThat(inativo.isAtivo()).isFalse();
    assertThatThrownBy(() -> service.redefinirSenha(4L, new UsuarioSenhaRequisicao("á".repeat(40))))
        .isInstanceOf(ValidacaoException.class);
  }
}
