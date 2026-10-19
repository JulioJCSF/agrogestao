package com.agrogestao.api.service.categoria;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.CategoriaDespesa;
import com.agrogestao.api.repository.CategoriaDespesaRepository;
import com.agrogestao.api.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaDespesaService {
  private final CategoriaDespesaRepository categoriaDespesaRepository;
  private final DespesaRepository despesaRepository;

  public PaginaResposta<CategoriaDespesaResposta> listar(String busca, PaginacaoConsulta consulta) {
    return PaginaResposta.de(
        categoriaDespesaRepository
            .findByNomeContainingIgnoreCase(busca, consulta.pageable())
            .map(CategoriaDespesaService::resposta));
  }

  public CategoriaDespesaResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public CategoriaDespesaResposta criar(CategoriaDespesaRequisicao dto) {
    validarNome(dto.nome().trim(), null);
    CategoriaDespesa entidade = new CategoriaDespesa();
    preencher(entidade, dto);
    return resposta(categoriaDespesaRepository.saveAndFlush(entidade));
  }

  @Transactional
  public CategoriaDespesaResposta alterar(Long id, CategoriaDespesaRequisicao dto) {
    CategoriaDespesa entidade = encontrar(id);
    validarEditavel(entidade);
    validarNome(dto.nome().trim(), id);
    preencher(entidade, dto);
    return resposta(categoriaDespesaRepository.saveAndFlush(entidade));
  }

  @Transactional
  public void excluir(Long id) {
    CategoriaDespesa entidade = encontrar(id);
    validarEditavel(entidade);
    if (despesaRepository.existsByCategoriaDespesaId(id))
      throw new RegraNegocioException(
          "Esta categoria já foi usada em despesas e não pode ser excluída.");
    categoriaDespesaRepository.delete(entidade);
    categoriaDespesaRepository.flush();
  }

  private CategoriaDespesa encontrar(Long id) {
    return categoriaDespesaRepository
        .findById(id)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada."));
  }

  private void validarNome(String nome, Long id) {
    if (id == null
        ? categoriaDespesaRepository.existsByNomeIgnoreCase(nome)
        : categoriaDespesaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id))
      throw new RegraNegocioException("Já existe uma categoria com este nome.");
  }

  private static void preencher(CategoriaDespesa entidade, CategoriaDespesaRequisicao dto) {
    entidade.setNome(dto.nome().trim());
  }

  private static CategoriaDespesaResposta resposta(CategoriaDespesa entidade) {
    return new CategoriaDespesaResposta(entidade.getId(), entidade.getNome(), entidade.isSistema());
  }

  private void validarEditavel(CategoriaDespesa entidade) {
    if (entidade.isSistema())
      throw new RegraNegocioException("Categoria de sistema não pode ser alterada nem removida.");
  }
}
