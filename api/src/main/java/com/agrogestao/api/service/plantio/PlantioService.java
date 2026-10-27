package com.agrogestao.api.service.plantio;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantioService {
  private final PlantioRepository repository;
  private final PropriedadeRepository propriedades;
  private final CulturaRepository culturas;
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
    Plantio entidade = encontrar(id);
    preencher(entidade, dto, entidade.getPropriedade().getProdutor().getId());
    return resposta(repository.saveAndFlush(entidade));
  }

  private Plantio encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
  }

  /** produtorAtual é nulo na criação; na alteração, trava o plantio no mesmo produtor. */
  private void preencher(Plantio entidade, PlantioRequisicao dto, Long produtorAtual) {
    if (dto.area().signum() <= 0)
      throw new RegraNegocioException("A área do plantio deve ser maior que zero.");
    if (dto.dataInicio().isAfter(LocalDate.now(relogio)))
      throw new RegraNegocioException("A data de início não pode ser posterior a hoje.");
    if (entidade.getDataEncerramento() != null
        && entidade.getDataEncerramento().isBefore(dto.dataInicio()))
      throw new RegraNegocioException(
          "A data de encerramento não pode ser anterior à data de início ("
              + dto.dataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
              + ").");
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
