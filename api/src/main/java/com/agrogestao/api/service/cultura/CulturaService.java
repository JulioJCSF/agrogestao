package com.agrogestao.api.service.cultura;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Cultura;
import com.agrogestao.api.repository.CulturaRepository;
import com.agrogestao.api.repository.ProducaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CulturaService {
  private final CulturaRepository culturaRepository;
  private final ProducaoRepository producoes;

  public PaginaResposta<CulturaResposta> listar(String busca, PaginacaoConsulta consulta) {
    return PaginaResposta.de(
        culturaRepository
            .findByNomeContainingIgnoreCase(busca, consulta.pageable())
            .map(CulturaService::resposta));
  }

  public CulturaResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public CulturaResposta criar(CulturaRequisicao dto) {
    validarNome(dto.nome().trim(), null);
    Cultura entidade = new Cultura();
    preencher(entidade, dto);
    return resposta(culturaRepository.saveAndFlush(entidade));
  }

  @Transactional
  public CulturaResposta alterar(Long id, CulturaRequisicao dto) {
    Cultura entidade =
        culturaRepository
            .buscarParaAtualizar(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cultura não encontrada."));

    validarNome(dto.nome().trim(), id);
    if (producoes.existsByPlantioCulturaId(id)
        && !entidade.getUnidadeMedida().trim().equalsIgnoreCase(dto.unidadeMedida().trim()))
      throw new RegraNegocioException(
          "A unidade da cultura não pode mudar porque já há produção registrada.");
    preencher(entidade, dto);
    return resposta(culturaRepository.saveAndFlush(entidade));
  }

  private Cultura encontrar(Long id) {
    return culturaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Cultura não encontrada."));
  }

  private void validarNome(String nome, Long id) {
    if (id == null
        ? culturaRepository.existsByNomeIgnoreCase(nome)
        : culturaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id))
      throw new RegraNegocioException("Já existe uma cultura com este nome.");
  }

  private static void preencher(Cultura entidade, CulturaRequisicao dto) {
    entidade.setNome(dto.nome().trim());
    entidade.setTipoCicloPadrao(dto.tipoCicloPadrao());
    entidade.setUnidadeMedida(dto.unidadeMedida().trim());
  }

  private static CulturaResposta resposta(Cultura entidade) {
    return new CulturaResposta(
        entidade.getId(),
        entidade.getNome(),
        entidade.getTipoCicloPadrao(),
        entidade.getUnidadeMedida());
  }
}
