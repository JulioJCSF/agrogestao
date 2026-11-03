package com.agrogestao.api.service.usuario;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.dto.ErroResposta.CampoErro;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.Usuario;
import com.agrogestao.api.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de usuários pelo administrador (RF05, M01). Usuário nunca é excluído, só desativado,
 * porque é autor de registros (RNF07). Decisões de 03/11: o login é gravado aparado e em minúsculas
 * e é único sem diferenciar caixa; o sistema nunca fica sem ADMIN ativo; o admin redefine a senha.
 *
 * <p>A senha em claro só passa pelo {@link PasswordEncoder}: não vai para a resposta, para o log
 * nem para mensagem de erro.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {
  /** O BCrypt considera só os primeiros 72 bytes e ignora o resto em silêncio. */
  static final int MAXIMO_BYTES_SENHA = 72;

  static final String SENHA_LONGA = "A senha deve ter no máximo 72 bytes.";
  static final String LOGIN_REPETIDO = "Já existe um usuário com este login.";
  static final String ULTIMO_ADMIN = "O sistema precisa de pelo menos um administrador ativo.";

  private final UsuarioRepository repository;
  private final PasswordEncoder passwordEncoder;

  public PaginaResposta<UsuarioResposta> listar(PaginacaoConsulta consulta) {
    return PaginaResposta.de(repository.findAll(consulta.pageable()).map(UsuarioService::resposta));
  }

  @Transactional
  public UsuarioResposta criar(UsuarioRequisicao dto) {
    exigirSenhaNoLimite(dto.senha());
    String login = dto.login().trim().toLowerCase(Locale.ROOT);
    if (repository.existsByLoginIgnoreCase(login))
      throw new RegraNegocioException(
          LOGIN_REPETIDO, List.of(new CampoErro("login", LOGIN_REPETIDO)));
    Usuario usuario =
        new Usuario(
            dto.nome().trim(), login, passwordEncoder.encode(dto.senha()), dto.perfil(), true);
    return resposta(repository.saveAndFlush(usuario));
  }

  /**
   * Altera nome, perfil e situação; o login não muda. Se a alteração tirar um ADMIN ativo, trava as
   * linhas dos ADMIN ativos antes de contar: com duas alterações concorrentes, a segunda espera o
   * commit da primeira e já não conta o usuário que ela desativou ou rebaixou.
   */
  @Transactional
  public UsuarioResposta alterar(Long id, UsuarioAlteracaoRequisicao dto) {
    Usuario usuario = encontrar(id);
    boolean deixaDeSerAdminAtivo =
        usuario.getPerfil() == Perfil.ADMIN
            && usuario.isAtivo()
            && (dto.perfil() != Perfil.ADMIN || !dto.ativo());
    if (deixaDeSerAdminAtivo
        && repository.findByPerfilAndAtivoTrue(Perfil.ADMIN).stream()
            .noneMatch(admin -> !admin.getId().equals(id)))
      throw new RegraNegocioException(ULTIMO_ADMIN);
    usuario.alterar(dto.nome().trim(), dto.perfil(), dto.ativo());
    return resposta(repository.saveAndFlush(usuario));
  }

  /** Vale também para usuário inativo: o admin pode preparar a volta dele. */
  @Transactional
  public void redefinirSenha(Long id, UsuarioSenhaRequisicao dto) {
    exigirSenhaNoLimite(dto.senha());
    Usuario usuario = encontrar(id);
    usuario.redefinirSenha(passwordEncoder.encode(dto.senha()));
    repository.saveAndFlush(usuario);
  }

  private Usuario encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
  }

  private static void exigirSenhaNoLimite(String senha) {
    if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_SENHA)
      throw new ValidacaoException(SENHA_LONGA, List.of(new CampoErro("senha", SENHA_LONGA)));
  }

  private static UsuarioResposta resposta(Usuario u) {
    return new UsuarioResposta(u.getId(), u.getNome(), u.getLogin(), u.getPerfil(), u.isAtivo());
  }
}
