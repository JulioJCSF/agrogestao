package com.agrogestao.api.service.despesa;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lançamento, detalhe e alteração de despesa com rateio gravado (RN06, RN09, RN10, RF28).
 *
 * <p>As validações rodam antes do {@link RateioDespesaService}, que assim nunca recebe entrada que
 * o faça lançar {@code IllegalArgumentException}. Consultas leem {@code valor_rateado} gravado e
 * nunca recalculam (RN10). Na alteração, o rateio só é recalculado quando mudam o valor ou o
 * conjunto de plantios (RN05).
 *
 * <p>As listas (L04b) mostram estornadas, com {@code estornada}, e geradas por diária, com {@code
 * diariaId}: tirar a estornada do custo é da apuração (RN19), não da consulta.
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
  private final RateioDespesaRepository rateios;
  private final RateioDespesaService rateio;
  private final LancamentoService lancamentos;

  public DespesaResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  /**
   * Despesas do produtor e do período, com os rateios gravados. Pagina as despesas e busca os
   * rateios da página numa consulta só, sem N+1 e sem {@code join fetch} de coleção paginada.
   */
  public PaginaResposta<DespesaResposta> listar(
      Long produtorId, LocalDate periodoInicio, LocalDate periodoFim, PaginacaoConsulta consulta) {
    exigirPeriodo(periodoInicio, periodoFim);
    Specification<Despesa> filtro =
        (raiz, query, cb) -> {
          var condicoes = new ArrayList<jakarta.persistence.criteria.Predicate>();
          if (produtorId != null)
            condicoes.add(cb.equal(raiz.get("produtor").get("id"), produtorId));
          if (periodoInicio != null)
            condicoes.add(cb.greaterThanOrEqualTo(raiz.get("dataFato"), periodoInicio));
          if (periodoFim != null)
            condicoes.add(cb.lessThanOrEqualTo(raiz.get("dataFato"), periodoFim));
          return cb.and(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    Page<Despesa> pagina = repository.findAll(filtro, consulta.pageable());
    Map<Long, List<RateioDespesa>> porDespesa =
        pagina.isEmpty()
            ? Map.of()
            : rateios.buscarDasDespesas(pagina.map(Despesa::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getDespesa().getId()));
    return PaginaResposta.de(
        pagina.map(d -> resposta(d, porDespesa.getOrDefault(d.getId(), List.of()))));
  }

  /**
   * Despesas rateadas ao plantio (RF23): uma linha por rateio, com {@code percentual} e {@code
   * valorRateado} lidos de {@code rateio_despesa}, nunca recalculados (RN10). Ordem pelo id da
   * despesa; a despesa vem no {@code join fetch} da mesma consulta.
   */
  public PaginaResposta<DespesaPlantioResposta> listarPorPlantio(
      Long plantioId, LocalDate periodoInicio, LocalDate periodoFim, PaginacaoConsulta consulta) {
    exigirPeriodo(periodoInicio, periodoFim);
    if (!plantios.existsById(plantioId))
      throw new RecursoNaoEncontradoException("Plantio não encontrado.");
    Specification<RateioDespesa> filtro =
        (raiz, query, cb) -> {
          // A consulta de contagem da página não aceita fetch.
          if (query.getResultType() != Long.class && query.getResultType() != long.class)
            raiz.fetch("despesa");
          var despesa = raiz.get("despesa");
          var condicoes = new ArrayList<jakarta.persistence.criteria.Predicate>();
          condicoes.add(cb.equal(raiz.get("plantio").get("id"), plantioId));
          if (periodoInicio != null)
            condicoes.add(cb.greaterThanOrEqualTo(despesa.get("dataFato"), periodoInicio));
          if (periodoFim != null)
            condicoes.add(cb.lessThanOrEqualTo(despesa.get("dataFato"), periodoFim));
          return cb.and(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    Pageable pagina =
        PageRequest.of(consulta.getPagina(), consulta.getTamanho(), Sort.by("despesa.id"));
    return PaginaResposta.de(
        rateios.findAll(filtro, pagina).map(DespesaService::respostaDoPlantio));
  }

  private static void exigirPeriodo(LocalDate inicio, LocalDate fim) {
    if (inicio != null && fim != null && fim.isBefore(inicio))
      throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
  }

  @Transactional
  public DespesaResposta criar(DespesaRequisicao dto) {
    Vinculos vinculos = validar(dto, Set.of());
    Despesa despesa = new Despesa();
    preencher(despesa, dto, vinculos);
    despesa.setEstornada(false);
    ratear(despesa, dto.valorTotal(), vinculos.plantios());
    return resposta(repository.saveAndFlush(despesa));
  }

  @Transactional
  public DespesaResposta alterar(Long id, DespesaRequisicao dto) {
    Despesa despesa = encontrar(id);
    exigirDespesaManual(despesa);
    // TODO(A03b): despesa com data do fato em período já emitido → bloqueio da RN11.
    Set<Long> plantiosAtuais =
        despesa.getRateios().stream().map(r -> r.getPlantio().getId()).collect(Collectors.toSet());
    Vinculos vinculos = validar(dto, plantiosAtuais);
    boolean recalcular =
        despesa.getValorTotal().compareTo(dto.valorTotal()) != 0
            || !plantiosAtuais.equals(new HashSet<>(dto.plantioIds()));
    preencher(despesa, dto, vinculos);
    if (recalcular) {
      ratear(despesa, dto.valorTotal(), vinculos.plantios());
    }
    return resposta(repository.saveAndFlush(despesa));
  }

  /**
   * Remove a despesa manual e todos os rateios dela na mesma transação (RN11). Não trava plantio:
   * excluir não cria lançamento depois de um encerramento.
   */
  @Transactional
  public void excluir(Long id) {
    Despesa despesa = encontrar(id);
    exigirDespesaManual(despesa);
    // TODO(A03b): despesa com data do fato em período já emitido → bloqueio da RN11 (CT020).
    repository.delete(despesa);
  }

  private static void exigirDespesaManual(Despesa despesa) {
    if (despesa.getDiaria() != null)
      throw new RegraNegocioException(
          "Esta despesa foi gerada por uma diária paga. Altere ou exclua a diária.");
  }

  private Despesa encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada."));
  }

  /**
   * Validações 2 a 9 da L04a, nesta ordem; a 1 (formato) é do Bean Validation no DTO. Os plantios
   * são lidos já travados, no mesmo ponto em que a L04a os procurava: o 404 e as regras seguintes
   * não mudam, e a RN04 é validada com o encerramento atual, sem corrida com o encerramento.
   *
   * @param atuais plantios atuais da despesa na alteração, travados junto com os novos
   */
  private Vinculos validar(DespesaRequisicao dto, Set<Long> atuais) {
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
    Map<Long, Plantio> encontrados = travar(ids, atuais);
    if (!encontrados.keySet().containsAll(ids))
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

  /**
   * Trava os plantios com {@code buscarParaAtualizar} em ordem crescente de id, sem repetir, como a
   * venda: duas transações com plantios em comum travam na mesma ordem e não entram em deadlock.
   * Plantio inexistente fica fora do mapa; o chamador responde 404.
   */
  private Map<Long, Plantio> travar(Collection<Long> ids, Collection<Long> atuais) {
    SortedSet<Long> ordem = new TreeSet<>(ids);
    ordem.addAll(atuais);
    Map<Long, Plantio> travados = new HashMap<>();
    ordem.forEach(id -> plantios.buscarParaAtualizar(id).ifPresent(p -> travados.put(id, p)));
    return travados;
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
    return resposta(d, d.getRateios());
  }

  private static DespesaResposta resposta(Despesa d, List<RateioDespesa> gravados) {
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
        gravados.stream()
            .sorted(POR_PLANTIO)
            .map(
                r ->
                    new RateioDespesaResposta(
                        r.getPlantio().getId(), r.getPercentual(), r.getValorRateado()))
            .toList());
  }

  private static DespesaPlantioResposta respostaDoPlantio(RateioDespesa r) {
    Despesa d = r.getDespesa();
    return new DespesaPlantioResposta(
        d.getId(),
        d.getProdutor().getId(),
        d.getCategoriaDespesa().getId(),
        d.getDiaria() == null ? null : d.getDiaria().getId(),
        d.getDescricao(),
        d.getDataFato(),
        d.isEstornada(),
        r.getPercentual(),
        r.getValorRateado());
  }
}
