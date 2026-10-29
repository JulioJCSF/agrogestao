package com.agrogestao.api.service.producao;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProducaoService {
  private final ProducaoRepository repository;
  private final PlantioRepository plantios;
  private final CulturaRepository culturas;
  private final LancamentoService lancamentos;

  public PaginaResposta<ProducaoResposta> listar(Long plantioId, PaginacaoConsulta consulta) {
    encontrarPlantio(plantioId);
    return PaginaResposta.de(
        repository.findByPlantioId(plantioId, consulta.pageable()).map(ProducaoService::resposta));
  }

  public ProducaoResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public ProducaoResposta criar(Long plantioId, ProducaoRequisicao dto) {
    Producao producao = new Producao();
    producao.setPlantio(bloquearPlantio(plantioId));
    preencher(producao, dto);
    return resposta(repository.saveAndFlush(producao));
  }

  @Transactional
  public ProducaoResposta alterar(Long id, ProducaoRequisicao dto) {
    Producao producao = encontrar(id);
    producao.setPlantio(bloquearPlantio(producao.getPlantio().getId()));
    preencher(producao, dto);
    return resposta(repository.saveAndFlush(producao));
  }

  @Transactional
  public void excluir(Long id) {
    repository.delete(encontrar(id));
  }

  private Plantio encontrarPlantio(Long id) {
    return plantios
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
  }

  private Plantio bloquearPlantio(Long id) {
    return plantios
        .buscarParaAtualizar(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
  }

  private Producao encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Produção não encontrada."));
  }

  private void preencher(Producao producao, ProducaoRequisicao dto) {
    if (dto.quantidade().signum() <= 0)
      throw new RegraNegocioException("A quantidade produzida deve ser maior que zero.");
    lancamentos.validarDataFato(dto.dataFato(), producao.getPlantio());
    Cultura cultura =
        culturas
            .buscarParaAtualizar(producao.getPlantio().getCultura().getId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cultura não encontrada."));
    String unidade = cultura.getUnidadeMedida().trim();
    if (dto.unidadeMedida() != null && !unidade.equalsIgnoreCase(dto.unidadeMedida().trim()))
      throw new RegraNegocioException(
          "A unidade da produção deve ser a mesma da cultura do plantio (" + unidade + ").",
          List.of(
              new ErroResposta.CampoErro(
                  "unidadeMedida", "Use a unidade da cultura: " + unidade + ".")));
    producao.setQuantidade(dto.quantidade());
    producao.setUnidadeMedida(unidade);
    producao.setDataFato(dto.dataFato());
  }

  private static ProducaoResposta resposta(Producao p) {
    return new ProducaoResposta(
        p.getId(),
        p.getPlantio().getId(),
        p.getQuantidade(),
        p.getUnidadeMedida(),
        p.getDataFato(),
        p.getDataColeta());
  }
}
