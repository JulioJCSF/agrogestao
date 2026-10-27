package com.agrogestao.api.service.despesa;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lançamento, detalhe e alteração de despesa com rateio gravado (RN06, RN09, RN10, RF28).
 *
 * <p>As validações rodam antes do {@link RateioDespesaService}, que assim nunca recebe entrada que
 * o faça lançar {@code IllegalArgumentException}. Consultas leem {@code valor_rateado} gravado e
 * nunca recalculam (RN10). Na alteração, o rateio só é recalculado quando mudam o valor ou o
 * conjunto de plantios (RN05).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DespesaService {
  private static final BigDecimal CENTAVO = new BigDecimal("0.01");
  private static final Comparator<RateioDespesa> POR_PLANTIO =
      Comparator.comparing(rateio -> rateio.getPlantio().getId());

  private final DespesaRepository repository;
  private final ProdutorRepository produtores;
  private final CategoriaDespesaRepository categorias;
  private final PlantioRepository plantios;
  private final RateioDespesaService rateio;
  private final LancamentoService lancamentos;

  public DespesaResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public DespesaResposta criar(DespesaRequisicao dto) {
    Vinculos vinculos = validar(dto);
    Despesa despesa = new Despesa();
    preencher(despesa, dto, vinculos);
    despesa.setEstornada(false);
    ratear(despesa, dto.valorTotal(), vinculos.plantios());
    return resposta(repository.saveAndFlush(despesa));
  }

  @Transactional
  public DespesaResposta alterar(Long id, DespesaRequisicao dto) {
    Despesa despesa = encontrar(id);
    // TODO(L04b): despesa com diaria_id (gerada pela diária paga) não é editável → 422 (RN16).
    // TODO(A03b): despesa com data do fato em período já emitido → bloqueio da RN11.
    Vinculos vinculos = validar(dto);
    Set<Long> plantiosAtuais =
        despesa.getRateios().stream().map(r -> r.getPlantio().getId()).collect(Collectors.toSet());
    boolean recalcular =
        despesa.getValorTotal().compareTo(dto.valorTotal()) != 0
            || !plantiosAtuais.equals(new HashSet<>(dto.plantioIds()));
    preencher(despesa, dto, vinculos);
    if (recalcular) {
      ratear(despesa, dto.valorTotal(), vinculos.plantios());
    }
    return resposta(repository.saveAndFlush(despesa));
  }

  private Despesa encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada."));
  }

  /** Validações 2 a 9 da L04a, nesta ordem; a 1 (formato) é do Bean Validation no DTO. */
  private Vinculos validar(DespesaRequisicao dto) {
    if (dto.valorTotal().signum() <= 0)
      throw new RegraNegocioException("O valor da despesa deve ser maior que zero.");
    List<Long> ids = dto.plantioIds();
    if (ids == null || ids.isEmpty())
      throw new RegraNegocioException("A despesa precisa estar vinculada a pelo menos um plantio.");
    if (new HashSet<>(ids).size() != ids.size())
      throw new RegraNegocioException("O mesmo plantio foi informado mais de uma vez.");
    Produtor produtor =
        produtores
            .findById(dto.produtorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
    CategoriaDespesa categoria =
        categorias
            .findById(dto.categoriaDespesaId())
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada."));
    Map<Long, Plantio> encontrados =
        plantios.findAllById(ids).stream()
            .collect(Collectors.toMap(Plantio::getId, Function.identity()));
    if (encontrados.size() != ids.size())
      throw new RecursoNaoEncontradoException("Plantio não encontrado.");
    if (categoria.isSistema())
      throw new RegraNegocioException(
          "A categoria Diária é lançada automaticamente pela diária paga e não pode ser usada em"
              + " despesa manual.");
    // Mantém a ordem de plantioIds: o rateio devolve as parcelas na ordem da entrada.
    List<Plantio> vinculados = ids.stream().map(encontrados::get).toList();
    if (vinculados.stream()
        .anyMatch(p -> !p.getPropriedade().getProdutor().getId().equals(dto.produtorId())))
      throw new RegraNegocioException(
          "Todos os plantios da despesa devem ser do produtor informado.");
    if (dto.valorTotal().compareTo(CENTAVO.multiply(BigDecimal.valueOf(ids.size()))) < 0)
      throw new RegraNegocioException(
          "O valor da despesa não cobre um centavo por plantio. Vincule menos plantios.");
    lancamentos.validarDataFato(dto.dataFato(), vinculados);
    return new Vinculos(produtor, categoria, vinculados);
  }

  /** Entidades já carregadas e validadas; plantios na ordem de plantioIds. */
  private record Vinculos(Produtor produtor, CategoriaDespesa categoria, List<Plantio> plantios) {}

  private void preencher(Despesa despesa, DespesaRequisicao dto, Vinculos vinculos) {
    despesa.setProdutor(vinculos.produtor());
    despesa.setCategoriaDespesa(vinculos.categoria());
    despesa.setDescricao(dto.descricao());
    despesa.setValorTotal(dto.valorTotal());
    despesa.setDataFato(dto.dataFato());
  }

  /**
   * Grava o rateio copiando percentual e valor de cada parcela. Plantio que continua é atualizado
   * na mesma linha, e não removido e reinserido: com {@code orphanRemoval}, o Hibernate executa os
   * INSERT antes dos DELETE e violaria {@code uk_rateio_despesa_plantio}.
   */
  private void ratear(Despesa despesa, BigDecimal valorTotal, List<Plantio> vinculados) {
    List<ParcelaRateio> parcelas =
        rateio.ratear(
            valorTotal,
            vinculados.stream().map(p -> new PlantioRateavel(p.getId(), p.getArea())).toList());
    Map<Long, Plantio> porId =
        vinculados.stream().collect(Collectors.toMap(Plantio::getId, Function.identity()));
    Map<Long, RateioDespesa> existentes =
        despesa.getRateios().stream()
            .collect(Collectors.toMap(r -> r.getPlantio().getId(), Function.identity()));
    despesa.getRateios().removeIf(r -> !porId.containsKey(r.getPlantio().getId()));
    for (ParcelaRateio parcela : parcelas) {
      RateioDespesa linha = existentes.get(parcela.plantioId());
      if (linha == null) {
        linha = new RateioDespesa();
        linha.setDespesa(despesa);
        linha.setPlantio(porId.get(parcela.plantioId()));
        despesa.getRateios().add(linha);
      }
      linha.setPercentual(parcela.percentual());
      linha.setValorRateado(parcela.valorRateado());
    }
  }

  private static DespesaResposta resposta(Despesa d) {
    return new DespesaResposta(
        d.getId(),
        d.getProdutor().getId(),
        d.getCategoriaDespesa().getId(),
        d.getDiaria() == null ? null : d.getDiaria().getId(),
        d.getDescricao(),
        d.getValorTotal(),
        d.getDataFato(),
        d.getDataColeta(),
        d.isEstornada(),
        d.getRateios().stream()
            .sorted(POR_PLANTIO)
            .map(
                r ->
                    new RateioDespesaResposta(
                        r.getPlantio().getId(), r.getPercentual(), r.getValorRateado()))
            .toList());
  }
}
