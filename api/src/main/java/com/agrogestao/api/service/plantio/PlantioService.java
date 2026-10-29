package com.agrogestao.api.service.plantio;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantioService {
  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final PlantioRepository repository;
  private final PropriedadeRepository propriedades;
  private final CulturaRepository culturas;
  private final ProducaoRepository producoes;
  private final DespesaRepository despesas;
  private final DiariaRepository diarias;
  private final VendaRepository vendas;
  private final Clock relogio;

  public PaginaResposta<PlantioResposta> listar(
      Long produtorId,
      Long culturaId,
      SituacaoPlantio situacao,
      LocalDate periodoInicio,
      LocalDate periodoFim,
      PaginacaoConsulta consulta) {
    if (periodoInicio != null && periodoFim != null && periodoFim.isBefore(periodoInicio))
      throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
    Specification<Plantio> filtro =
        (raiz, query, cb) -> {
          var condicoes = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
          if (produtorId != null)
            condicoes.add(cb.equal(raiz.get("propriedade").get("produtor").get("id"), produtorId));
          if (culturaId != null) condicoes.add(cb.equal(raiz.get("cultura").get("id"), culturaId));
          if (situacao != null)
            condicoes.add(
                situacao == SituacaoPlantio.ABERTO
                    ? cb.isNull(raiz.get("dataEncerramento"))
                    : cb.isNotNull(raiz.get("dataEncerramento")));
          if (periodoInicio != null)
            condicoes.add(cb.greaterThanOrEqualTo(raiz.get("dataInicio"), periodoInicio));
          if (periodoFim != null)
            condicoes.add(cb.lessThanOrEqualTo(raiz.get("dataInicio"), periodoFim));
          return cb.and(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return PaginaResposta.de(
        repository.findAll(filtro, consulta.pageable()).map(PlantioService::resposta));
  }

  public PlantioResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public PlantioResposta criar(PlantioRequisicao dto) {
    Plantio entidade = new Plantio();
    preencher(entidade, dto, null);
    return resposta(repository.saveAndFlush(entidade));
  }

  @Transactional
  public PlantioResposta alterar(Long id, PlantioRequisicao dto) {
    Plantio entidade = encontrarParaAtualizar(id);
    preencher(entidade, dto, entidade.getPropriedade().getProdutor().getId());
    return resposta(repository.saveAndFlush(entidade));
  }

  /**
   * Validações da C06b nesta ordem: já encerrado, data futura, RN03 e lançamento posterior (a
   * contrapartida da RN04 para o que já está gravado). Só grava {@code data_encerramento}.
   */
  @Transactional
  public PlantioResposta encerrar(Long id, EncerramentoPlantioRequisicao dto) {
    Plantio entidade = encontrarParaAtualizar(id);
    LocalDate encerramento = dto.dataEncerramento();
    if (entidade.getDataEncerramento() != null)
      throw new RegraNegocioException(
          "O plantio já foi encerrado em " + entidade.getDataEncerramento().format(DATA) + ".");
    if (encerramento.isAfter(LocalDate.now(relogio)))
      throw new RegraNegocioException("A data de encerramento não pode ser posterior a hoje.");
    exigirEncerramentoAposInicio(entidade.getDataInicio(), encerramento);
    Optional<LocalDate> ultimo = ultimoLancamento(id);
    if (ultimo.isPresent() && ultimo.get().isAfter(encerramento))
      throw new RegraNegocioException(
          "Há lançamento em "
              + ultimo.get().format(DATA)
              + ", posterior à data de encerramento informada.");
    entidade.setDataEncerramento(encerramento);
    return resposta(repository.saveAndFlush(entidade));
  }

  /** Maior data do fato entre produção, despesa (pelo rateio), diária e item de venda. */
  private Optional<LocalDate> ultimoLancamento(Long plantioId) {
    return Stream.of(
            producoes.ultimaDataFatoDoPlantio(plantioId),
            despesas.ultimaDataFatoDoPlantio(plantioId),
            diarias.ultimaDataFatoDoPlantio(plantioId),
            vendas.ultimaDataFatoDoPlantio(plantioId))
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder());
  }

  /** RN03, com a mesma mensagem no encerramento e na alteração do início. */
  private static void exigirEncerramentoAposInicio(LocalDate inicio, LocalDate encerramento) {
    if (encerramento.isBefore(inicio))
      throw new RegraNegocioException(
          "A data de encerramento não pode ser anterior à data de início ("
              + inicio.format(DATA)
              + ").");
  }

  private Plantio encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
  }

  private Plantio encontrarParaAtualizar(Long id) {
    return repository
        .buscarParaAtualizar(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
  }

  /** produtorAtual é nulo na criação; na alteração, trava o plantio no mesmo produtor. */
  private void preencher(Plantio entidade, PlantioRequisicao dto, Long produtorAtual) {
    if (dto.area().signum() <= 0)
      throw new RegraNegocioException("A área do plantio deve ser maior que zero.");
    if (dto.dataInicio().isAfter(LocalDate.now(relogio)))
      throw new RegraNegocioException("A data de início não pode ser posterior a hoje.");
    if (entidade.getDataEncerramento() != null)
      exigirEncerramentoAposInicio(dto.dataInicio(), entidade.getDataEncerramento());
    Propriedade propriedade =
        propriedades
            .findById(dto.propriedadeId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Propriedade não encontrada."));
    if (produtorAtual != null && !produtorAtual.equals(propriedade.getProdutor().getId()))
      throw new RegraNegocioException(
          "O plantio não pode ser transferido para propriedade de outro produtor.");
    Cultura cultura =
        culturas
            .findById(dto.culturaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cultura não encontrada."));
    entidade.setPropriedade(propriedade);
    entidade.setCultura(cultura);
    entidade.setArea(dto.area());
    entidade.setTipoCiclo(dto.tipoCiclo() == null ? cultura.getTipoCicloPadrao() : dto.tipoCiclo());
    entidade.setDataInicio(dto.dataInicio());
    entidade.setPrevisaoColheita(dto.previsaoColheita());
  }

  private static PlantioResposta resposta(Plantio p) {
    return new PlantioResposta(
        p.getId(),
        p.getPropriedade().getProdutor().getId(),
        p.getPropriedade().getId(),
        p.getCultura().getId(),
        p.getArea(),
        p.getTipoCiclo(),
        p.getDataInicio(),
        p.getPrevisaoColheita(),
        p.getDataEncerramento(),
        p.getDataEncerramento() == null ? SituacaoPlantio.ABERTO : SituacaoPlantio.ENCERRADO);
  }
}
