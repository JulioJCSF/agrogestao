package com.agrogestao.api.service.propriedade;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.RecursoNaoEncontradoException;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropriedadeService {
  private final PropriedadeRepository propriedadeRepository;
  private final ProdutorRepository produtorRepository;

  public PaginaResposta<PropriedadeResposta> listar(Long produtorId, PaginacaoConsulta consulta) {
    if (!produtorRepository.existsById(produtorId))
      throw new RecursoNaoEncontradoException("Produtor não encontrado.");
    return PaginaResposta.de(
        propriedadeRepository
            .findByProdutorId(produtorId, consulta.pageable())
            .map(PropriedadeService::resposta));
  }

  public PropriedadeResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public PropriedadeResposta criar(Long produtorId, PropriedadeRequisicao dto) {
    Produtor produtor =
        produtorRepository
            .findById(produtorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
    Propriedade entidade = new Propriedade();
    entidade.setProdutor(produtor);
    preencher(entidade, dto);
    return resposta(propriedadeRepository.saveAndFlush(entidade));
  }

  @Transactional
  public PropriedadeResposta alterar(Long id, PropriedadeRequisicao dto) {
    Propriedade entidade = encontrar(id);
    preencher(entidade, dto);
    return resposta(propriedadeRepository.saveAndFlush(entidade));
  }

  private Propriedade encontrar(Long id) {
    return propriedadeRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Propriedade não encontrada."));
  }

  private static void preencher(Propriedade entidade, PropriedadeRequisicao dto) {
    entidade.setNome(dto.nome().trim());
    entidade.setAreaTotal(dto.areaTotal());
    entidade.setLocalizacao(dto.localizacao());
  }

  private static PropriedadeResposta resposta(Propriedade entidade) {
    return new PropriedadeResposta(
        entidade.getId(),
        entidade.getProdutor().getId(),
        entidade.getNome(),
        entidade.getAreaTotal(),
        entidade.getLocalizacao());
  }
}
