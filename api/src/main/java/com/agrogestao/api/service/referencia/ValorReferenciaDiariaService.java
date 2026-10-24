package com.agrogestao.api.service.referencia;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.ValorReferenciaDiaria;
import com.agrogestao.api.repository.ValorReferenciaDiariaRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValorReferenciaDiariaService {
  private final ValorReferenciaDiariaRepository repository;
  static final String SOBREPOSICAO =
      "Já existe valor de referência da diária com vigência neste período. Informe um período sem sobreposição.";

  public PaginaResposta<ValorReferenciaDiariaResposta> listar(PaginacaoConsulta consulta) {
    return PaginaResposta.de(
        repository.findAll(consulta.pageable()).map(ValorReferenciaDiariaService::resposta));
  }

  public ValorReferenciaDiariaResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  public ValorReferenciaDiariaResposta vigente(LocalDate dataFato) {
    return resposta(
        repository
            .buscarVigente(dataFato)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Não há valor de referência da diária vigente nesta data. Cadastre uma referência para o período.")));
  }

  @Transactional
  public ValorReferenciaDiariaResposta criar(ValorReferenciaDiariaRequisicao dto) {
    validar(dto, -1L);
    var entidade = new ValorReferenciaDiaria();
    preencher(entidade, dto);
    return salvar(entidade);
  }

  @Transactional
  public ValorReferenciaDiariaResposta alterar(Long id, ValorReferenciaDiariaRequisicao dto) {
    var entidade = encontrar(id);
    validar(dto, id);
    preencher(entidade, dto);
    return salvar(entidade);
  }

  private void validar(ValorReferenciaDiariaRequisicao dto, Long ignorarId) {
    if (dto.valor().signum() <= 0)
      throw new RegraNegocioException("O valor de referência da diária deve ser maior que zero.");
    if (dto.vigenciaFim() != null && dto.vigenciaFim().isBefore(dto.vigenciaInicio()))
      throw new RegraNegocioException("O fim da vigência não pode ser anterior ao início.");
    if (repository.existeSobreposicao(dto.vigenciaInicio(), dto.vigenciaFim(), ignorarId))
      throw new RegraNegocioException(SOBREPOSICAO);
  }

  private ValorReferenciaDiariaResposta salvar(ValorReferenciaDiaria entidade) {
    try {
      return resposta(repository.saveAndFlush(entidade));
    } catch (DataIntegrityViolationException ex) {
      if (ex.getMostSpecificCause().getMessage() != null
          && ex.getMostSpecificCause().getMessage().contains("ex_vrd_vigencia"))
        throw new RegraNegocioException(SOBREPOSICAO);
      throw ex;
    }
  }

  private ValorReferenciaDiaria encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new RecursoNaoEncontradoException("Valor de referência da diária não encontrado."));
  }

  private static void preencher(
      ValorReferenciaDiaria entidade, ValorReferenciaDiariaRequisicao dto) {
    entidade.setValor(dto.valor());
    entidade.setVigenciaInicio(dto.vigenciaInicio());
    entidade.setVigenciaFim(dto.vigenciaFim());
  }

  private static ValorReferenciaDiariaResposta resposta(ValorReferenciaDiaria entidade) {
    return new ValorReferenciaDiariaResposta(
        entidade.getId(),
        entidade.getValor(),
        entidade.getVigenciaInicio(),
        entidade.getVigenciaFim());
  }
}
