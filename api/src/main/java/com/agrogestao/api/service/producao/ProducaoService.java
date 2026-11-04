package com.agrogestao.api.service.producao;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.relatorio.ProtecaoEmissao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro, consulta, alteração e exclusão de produção do plantio.
 *
 * <p>A03d (RN11, RN34; decisão 3 de 03/11): como a venda, a produção com data do fato em período
 * emitido do produtor do plantio não é excluída nem tem a data trocada; quantidade e unidade (RN38)
 * continuam corrigíveis. Alterar e excluir travam o produtor antes da produção, do plantio e da
 * cultura ({@link ProtecaoEmissao}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProducaoService {
  private final ProducaoRepository repository;
  private final PlantioRepository plantios;
  private final CulturaRepository culturas;
  private final LancamentoService lancamentos;
  private final ProtecaoEmissao protecao;

  static final String EXCLUSAO_EMITIDA =
      "Esta produção já consta em relatório emitido e não pode ser excluída. Corrija pela alteração.";
  static final String DATA_EMITIDA =
      "Esta produção já consta em relatório emitido. A data do fato não pode ser alterada.";

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
    Long produtorId = travarProdutor(id);
    Producao producao = encontrar(id);
    producao.setPlantio(bloquearPlantio(producao.getPlantio().getId()));
    if (!dto.dataFato().equals(producao.getDataFato()))
      protecao.exigirNaoEmitido(produtorId, producao.getDataFato(), DATA_EMITIDA);
    preencher(producao, dto);
    return resposta(repository.saveAndFlush(producao));
  }

  /** Produção não tem estorno: em período emitido, corrige-se pela alteração. */
  @Transactional
  public void excluir(Long id) {
    Long produtorId = travarProdutor(id);
    Producao producao = encontrar(id);
    protecao.exigirNaoEmitido(produtorId, producao.getDataFato(), EXCLUSAO_EMITIDA);
    repository.delete(producao);
  }

  /**
   * Trava o produtor do plantio antes de ler a produção, para a leitura já ver o que outra
   * transação gravou. O plantio da produção não muda, e o plantio não muda de produtor.
   */
  private Long travarProdutor(Long id) {
    Long produtorId =
        repository
            .buscarProdutorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produção não encontrada."));
    protecao.travarProdutor(produtorId);
    return produtorId;
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
