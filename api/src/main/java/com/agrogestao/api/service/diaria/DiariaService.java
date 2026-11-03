package com.agrogestao.api.service.diaria;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.dto.ErroResposta.CampoErro;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.relatorio.ProtecaoEmissao;
import java.math.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiariaService {
  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final BigDecimal MAIOR_VALOR = new BigDecimal("9999999999.99");

  private final DiariaRepository repository;
  private final PlantioRepository plantios;
  private final ValorReferenciaDiariaRepository referencias;
  private final CategoriaDespesaRepository categorias;
  private final DespesaRepository despesas;
  private final RateioDespesaRepository rateios;
  private final LancamentoService lancamentos;
  private final ProtecaoEmissao protecao;

  static final String DESPESA_ESTORNADA =
      "A despesa desta diária foi estornada. A diária não pode mais ser alterada nem excluída.";

  public PaginaResposta<DiariaResposta> listar(Long plantioId, PaginacaoConsulta consulta) {
    if (!plantios.existsById(plantioId))
      throw new RecursoNaoEncontradoException("Plantio não encontrado.");
    return PaginaResposta.de(
        repository.findByPlantioId(plantioId, consulta.pageable()).map(DiariaResposta::de));
  }

  public DiariaResposta detalhar(Long id) {
    return DiariaResposta.de(
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Diária não encontrada.")));
  }

  @Transactional
  public DiariaResposta criar(Long plantioId, DiariaRequisicao dto) {
    Diaria diaria = new Diaria();
    diaria.setPlantio(bloquearPlantio(plantioId));
    preencher(diaria, dto);
    repository.saveAndFlush(diaria);
    if (diaria.getNatureza() == NaturezaDiaria.PAGA) {
      Despesa despesa = new Despesa();
      despesa.setDiaria(diaria);
      despesa.setProdutor(diaria.getPlantio().getPropriedade().getProdutor());
      despesa.setCategoriaDespesa(
          categorias
              .findByNomeIgnoreCaseAndSistemaTrue("Diária")
              .orElseThrow(
                  () ->
                      new RegraNegocioException(
                          "A categoria de sistema Diária não está cadastrada. Solicite a configuração ao administrador.")));
      atualizarDespesa(despesa, diaria);
      lancamentos.herdarDataColeta(diaria, despesa);
      despesas.saveAndFlush(despesa);
      RateioDespesa rateio = new RateioDespesa();
      rateio.setDespesa(despesa);
      rateio.setPlantio(diaria.getPlantio());
      rateio.setPercentual(new BigDecimal("1.000000"));
      rateio.setValorRateado(despesa.getValorTotal());
      rateios.saveAndFlush(rateio);
      despesa.getRateios().add(rateio);
    }
    return DiariaResposta.de(diaria);
  }

  @Transactional
  public DiariaResposta alterar(Long id, DiariaRequisicao dto) {
    Diaria diaria = travar(id);
    diaria.setPlantio(bloquearPlantio(diaria.getPlantio().getId()));
    exigirDespesaNaoEstornada(diaria);
    if (diaria.getNatureza() != dto.natureza())
      throw new RegraNegocioException(
          "A natureza da diária não pode ser alterada. Exclua a diária e registre outra com a natureza correta.",
          List.of(
              new CampoErro("natureza", "Exclua e registre outra diária para mudar a natureza.")));
    if (diaria.getNatureza() == NaturezaDiaria.PAGA && !dto.dataFato().equals(diaria.getDataFato()))
      exigirDespesaNaoEmitida(despesaGerada(id));
    preencher(diaria, dto);
    if (diaria.getNatureza() == NaturezaDiaria.PAGA) {
      Despesa despesa = despesaGerada(id);
      atualizarDespesa(despesa, diaria);
      despesa.getRateios().forEach(r -> r.setValorRateado(despesa.getValorTotal()));
      despesas.saveAndFlush(despesa);
    }
    return DiariaResposta.de(repository.saveAndFlush(diaria));
  }

  @Transactional
  public void excluir(Long id) {
    Diaria diaria = travar(id);
    bloquearPlantio(diaria.getPlantio().getId());
    exigirDespesaNaoEstornada(diaria);
    if (diaria.getNatureza() == NaturezaDiaria.PAGA) {
      Despesa despesa = despesaGerada(id);
      exigirDespesaNaoEmitida(despesa);
      despesas.delete(despesa);
      despesas.flush();
    }
    repository.delete(diaria);
  }

  /**
   * Ordem de locks da A03b: produtor, diária e plantio. O produtor é lido sem carregar a diária,
   * para a diária travada em seguida já vir com o que outra transação gravou.
   */
  private Diaria travar(Long id) {
    protecao.travarProdutor(
        repository
            .buscarProdutorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Diária não encontrada.")));
    return bloquearDiaria(id);
  }

  /** Decisão 3 de 03/11: a diária paga acompanha a despesa estornada e fica congelada. */
  private void exigirDespesaNaoEstornada(Diaria diaria) {
    if (diaria.getNatureza() == NaturezaDiaria.PAGA && despesaGerada(diaria.getId()).isEstornada())
      throw new RegraNegocioException(DESPESA_ESTORNADA);
  }

  private Diaria bloquearDiaria(Long id) {
    return repository
        .buscarParaAtualizar(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Diária não encontrada."));
  }

  private Plantio bloquearPlantio(Long id) {
    return plantios
        .buscarParaAtualizar(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
  }

  private Despesa despesaGerada(Long diariaId) {
    return despesas
        .findByDiariaId(diariaId)
        .orElseThrow(
            () ->
                new RegraNegocioException(
                    "A despesa desta diária paga não foi encontrada. Solicite a correção ao administrador."));
  }

  private void exigirDespesaNaoEmitida(Despesa despesa) {
    protecao.exigirNaoEmitido(
        despesa.getProdutor().getId(),
        despesa.getDataFato(),
        "A despesa desta diária já consta em relatório emitido. Não altere a data nem exclua a diária. Use o estorno da despesa.");
  }

  private void preencher(Diaria diaria, DiariaRequisicao dto) {
    if (dto.quantidadeDias().signum() <= 0)
      throw new RegraNegocioException(
          "A quantidade de dias deve ser maior que zero.",
          List.of(
              new CampoErro("quantidadeDias", "Informe uma quantidade de dias maior que zero.")));
    lancamentos.validarDataFato(dto.dataFato(), diaria.getPlantio());
    if (dto.natureza() == NaturezaDiaria.PAGA) {
      if (dto.valorUnitario() == null || dto.valorUnitario().signum() <= 0)
        throw new RegraNegocioException(
            "Informe um valor maior que zero para a diária paga.",
            List.of(
                new CampoErro("valorUnitario", "Informe o valor da diária paga, maior que zero.")));
      BigDecimal total = total(dto.quantidadeDias(), dto.valorUnitario());
      if (total.signum() <= 0 || total.compareTo(MAIOR_VALOR) > 0)
        throw new RegraNegocioException(
            "O valor total da diária paga deve estar entre R$ 0,01 e R$ 9.999.999.999,99.");
      diaria.setValorUnitario(dto.valorUnitario());
      diaria.setValorReferenciaDiaria(null);
    } else if (diaria.getId() == null || !dto.dataFato().equals(diaria.getDataFato())) {
      ValorReferenciaDiaria referencia =
          referencias
              .buscarVigente(dto.dataFato())
              .orElseThrow(
                  () ->
                      new RegraNegocioException(
                          "Não há valor de referência da diária vigente em "
                              + dto.dataFato().format(DATA)
                              + ". Cadastre o valor de referência para essa data antes de lançar a diária familiar."));
      diaria.setValorReferenciaDiaria(referencia);
      diaria.setValorUnitario(referencia.getValor());
    }
    diaria.setNatureza(dto.natureza());
    diaria.setQuantidadeDias(dto.quantidadeDias());
    diaria.setDataFato(dto.dataFato());
  }

  private static BigDecimal total(BigDecimal dias, BigDecimal valor) {
    return dias.multiply(valor).setScale(2, RoundingMode.HALF_UP);
  }

  private static void atualizarDespesa(Despesa despesa, Diaria diaria) {
    despesa.setDescricao("Diária paga de " + diaria.getDataFato().format(DATA));
    despesa.setDataFato(diaria.getDataFato());
    despesa.setValorTotal(total(diaria.getQuantidadeDias(), diaria.getValorUnitario()));
  }
}
