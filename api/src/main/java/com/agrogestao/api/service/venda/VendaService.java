package com.agrogestao.api.service.venda;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.dto.ErroResposta.CampoErro;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.relatorio.ProtecaoEmissao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro, consulta, alteração e exclusão de venda com itens ligados ao plantio de origem (RF30,
 * RF31; RN26, RN27, RN29, RN30).
 *
 * <p>Totais calculados aqui: item com {@code HALF_UP} em 2 casas e venda pela soma dos itens já
 * arredondados (RN30). O saldo nunca bloqueia (RN29): quando o vendido acumulado de um plantio
 * passa do produzido, a resposta leva um aviso, recalculado também nas consultas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendaService {
  private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

  private final VendaRepository repository;
  private final ItemVendaRepository itensVenda;
  private final ProdutorRepository produtores;
  private final ClienteRepository clientes;
  private final PlantioRepository plantios;
  private final ProducaoRepository producoes;
  private final LancamentoService lancamentos;
  private final ProtecaoEmissao protecao;

  static final String EXCLUSAO_EMITIDA =
      "Esta venda já consta em relatório emitido e não pode ser excluída. Corrija pela alteração.";
  static final String DATA_EMITIDA =
      "Esta venda já consta em relatório emitido. A data do fato não pode ser alterada.";

  public PaginaResposta<VendaResposta> listar(
      Long produtorId, LocalDate periodoInicio, LocalDate periodoFim, PaginacaoConsulta consulta) {
    if (periodoInicio != null && periodoFim != null && periodoFim.isBefore(periodoInicio))
      throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
    Specification<Venda> filtro =
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
    Page<Venda> pagina = repository.findAll(filtro, consulta.pageable());
    // Itens e totais por plantio da página inteira em três consultas, sem N+1.
    Map<Long, List<ItemVenda>> itensPorVenda =
        pagina.isEmpty()
            ? Map.of()
            : itensVenda.buscarDasVendas(pagina.map(Venda::getId).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getVenda().getId()));
    Totais totais =
        totais(
            itensPorVenda.values().stream()
                .flatMap(List::stream)
                .map(i -> i.getPlantio().getId())
                .collect(Collectors.toSet()));
    return PaginaResposta.de(
        pagina.map(v -> resposta(v, itensPorVenda.getOrDefault(v.getId(), List.of()), totais)));
  }

  public VendaResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public VendaResposta criar(VendaRequisicao dto) {
    Vinculos vinculos = validar(dto);
    Venda venda = new Venda();
    preencher(venda, dto, vinculos);
    return resposta(repository.saveAndFlush(venda));
  }

  /**
   * Substitui todos os itens; a data de coleta fica a original (RN36). Em período emitido, tudo
   * continua corrigível menos a data do fato (decisão 2 de 03/11). Trava o produtor antes de ler a
   * venda e antes dos plantios.
   */
  @Transactional
  public VendaResposta alterar(Long id, VendaRequisicao dto) {
    Venda venda = travar(id);
    if (!venda.getDataFato().equals(dto.dataFato()))
      protecao.exigirNaoEmitido(venda.getProdutor().getId(), venda.getDataFato(), DATA_EMITIDA);
    Vinculos vinculos = validar(dto);
    // Com orphanRemoval, o Hibernate faria os INSERT antes dos DELETE; o flush remove primeiro.
    venda.getItens().clear();
    repository.flush();
    preencher(venda, dto, vinculos);
    return resposta(repository.saveAndFlush(venda));
  }

  /** Venda não tem estorno: em período emitido, corrige-se pela alteração (decisão 2 de 03/11). */
  @Transactional
  public void excluir(Long id) {
    Venda venda = travar(id);
    protecao.exigirNaoEmitido(venda.getProdutor().getId(), venda.getDataFato(), EXCLUSAO_EMITIDA);
    repository.delete(venda);
  }

  /** Trava o produtor da venda e só então a lê; se a venda mudou de produtor, trava o novo. */
  private Venda travar(Long id) {
    Long produtorId =
        repository
            .buscarProdutorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Venda não encontrada."));
    protecao.travarProdutor(produtorId);
    Venda venda = encontrar(id);
    if (!venda.getProdutor().getId().equals(produtorId))
      protecao.travarProdutor(venda.getProdutor().getId());
    return venda;
  }

  private Venda encontrar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Venda não encontrada."));
  }

  /**
   * Validações 2 a 6 da L09b, nesta ordem; a 1 (formato) é do Bean Validation no DTO. Os plantios
   * são travados em ordem crescente de id, sem repetir, antes das regras que leem o estado deles:
   * fecha a corrida com o encerramento (RN04) sem deadlock entre vendas de vários plantios.
   */
  private Vinculos validar(VendaRequisicao dto) {
    List<ItemVendaRequisicao> itens = dto.itens();
    for (int n = 0; n < itens.size(); n++) {
      if (itens.get(n).quantidade().signum() <= 0)
        throw regra(
            "A quantidade do item deve ser maior que zero.",
            n,
            "quantidade",
            "Informe uma quantidade maior que zero.");
      if (itens.get(n).valorUnitario().signum() <= 0)
        throw regra(
            "O valor unitário do item deve ser maior que zero.",
            n,
            "valorUnitario",
            "Informe um valor unitário maior que zero.");
    }
    Produtor produtor =
        produtores
            .findById(dto.produtorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
    Cliente cliente =
        clientes
            .findById(dto.clienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    Map<Long, Plantio> travados = new LinkedHashMap<>();
    itens.stream()
        .map(ItemVendaRequisicao::plantioId)
        .distinct()
        .sorted()
        .forEach(
            plantioId ->
                travados.put(
                    plantioId,
                    plantios
                        .buscarParaAtualizar(plantioId)
                        .orElseThrow(
                            () -> new RecursoNaoEncontradoException("Plantio não encontrado."))));
    for (int n = 0; n < itens.size(); n++) {
      Plantio plantio = travados.get(itens.get(n).plantioId());
      if (!plantio.getPropriedade().getProdutor().getId().equals(dto.produtorId()))
        throw regra(
            "Todos os itens devem ser de plantios do produtor informado.",
            n,
            "plantioId",
            "Escolha um plantio do produtor informado.");
    }
    Map<Long, Referencia> referencias = new HashMap<>();
    for (int n = 0; n < itens.size(); n++) {
      Referencia referencia =
          referencias.computeIfAbsent(itens.get(n).plantioId(), id -> referencia(travados.get(id)));
      String informada = itens.get(n).unidadeMedida().trim();
      if (!referencia.unidade().equalsIgnoreCase(informada))
        throw regra(
            "A unidade do item ("
                + informada
                + ") é diferente da unidade "
                + (referencia.daProducao()
                    ? "da produção registrada no plantio"
                    : "da cultura do plantio")
                + " ("
                + referencia.unidade()
                + ").",
            n,
            "unidadeMedida",
            "Use a unidade " + referencia.unidade() + ".");
    }
    lancamentos.validarDataFato(dto.dataFato(), List.copyOf(travados.values()));
    Map<Long, String> unidades = new HashMap<>();
    referencias.forEach((id, referencia) -> unidades.put(id, referencia.unidade()));
    return new Vinculos(produtor, cliente, travados, unidades);
  }

  /** RN30: a unidade da produção registrada no plantio; sem produção, a da cultura do plantio. */
  private Referencia referencia(Plantio plantio) {
    return producoes
        .findFirstByPlantioIdOrderByIdAsc(plantio.getId())
        .map(p -> new Referencia(p.getUnidadeMedida().trim(), true))
        .orElseGet(() -> new Referencia(plantio.getCultura().getUnidadeMedida().trim(), false));
  }

  private record Referencia(String unidade, boolean daProducao) {}

  private static RegraNegocioException regra(
      String mensagem, int item, String campo, String orientacao) {
    return new RegraNegocioException(
        mensagem, List.of(new CampoErro("itens[" + item + "]." + campo, orientacao)));
  }

  /** Entidades já carregadas e validadas; unidade de referência (grafia gravada) por plantio. */
  private record Vinculos(
      Produtor produtor,
      Cliente cliente,
      Map<Long, Plantio> plantios,
      Map<Long, String> unidades) {}

  private static void preencher(Venda venda, VendaRequisicao dto, Vinculos vinculos) {
    venda.setProdutor(vinculos.produtor());
    venda.setCliente(vinculos.cliente());
    venda.setDataFato(dto.dataFato());
    BigDecimal total = BigDecimal.ZERO;
    for (ItemVendaRequisicao entrada : dto.itens()) {
      ItemVenda item = new ItemVenda();
      item.setVenda(venda);
      item.setPlantio(vinculos.plantios().get(entrada.plantioId()));
      // @Digits já limita as casas: a escala só iguala o POST ao que o GET lê do banco.
      item.setQuantidade(entrada.quantidade().setScale(3, RoundingMode.HALF_UP));
      item.setUnidadeMedida(vinculos.unidades().get(entrada.plantioId()));
      item.setValorUnitario(entrada.valorUnitario().setScale(2, RoundingMode.HALF_UP));
      item.setValorTotal(
          entrada.quantidade().multiply(entrada.valorUnitario()).setScale(2, RoundingMode.HALF_UP));
      total = total.add(item.getValorTotal());
      venda.getItens().add(item);
    }
    venda.setValorTotal(total.setScale(2, RoundingMode.HALF_UP));
  }

  private VendaResposta resposta(Venda venda) {
    List<ItemVenda> itens = venda.getItens();
    return resposta(
        venda,
        itens,
        totais(itens.stream().map(i -> i.getPlantio().getId()).collect(Collectors.toSet())));
  }

  /** Produzido e vendido acumulados dos plantios, sem recorte de período; vendido inclui esta. */
  private Totais totais(Set<Long> plantioIds) {
    if (plantioIds.isEmpty()) return new Totais(Map.of(), Map.of());
    return new Totais(
        porPlantio(producoes.somarQuantidadePorPlantio(plantioIds)),
        porPlantio(itensVenda.somarQuantidadePorPlantio(plantioIds)));
  }

  private static Map<Long, BigDecimal> porPlantio(List<TotalPorPlantio> linhas) {
    return linhas.stream()
        .collect(Collectors.toMap(TotalPorPlantio::getPlantioId, TotalPorPlantio::getTotal));
  }

  private record Totais(Map<Long, BigDecimal> produzido, Map<Long, BigDecimal> vendido) {}

  private static VendaResposta resposta(Venda v, List<ItemVenda> itens, Totais totais) {
    List<ItemVenda> ordenados =
        itens.stream().sorted(Comparator.comparing(ItemVenda::getId, nullsLast())).toList();
    return new VendaResposta(
        v.getId(),
        v.getProdutor().getId(),
        v.getCliente().getId(),
        v.getDataFato(),
        v.getDataColeta(),
        v.getValorTotal(),
        ordenados.stream()
            .map(
                i ->
                    new ItemVendaResposta(
                        i.getId(),
                        i.getPlantio().getId(),
                        i.getQuantidade(),
                        i.getUnidadeMedida(),
                        i.getValorUnitario(),
                        i.getValorTotal()))
            .toList(),
        avisos(ordenados, totais));
  }

  /** Um aviso por plantio distinto da venda cujo vendido acumulado passa do produzido (RN29). */
  private static List<String> avisos(List<ItemVenda> itens, Totais totais) {
    Map<Long, String> unidadePorPlantio = new TreeMap<>();
    itens.forEach(i -> unidadePorPlantio.putIfAbsent(i.getPlantio().getId(), i.getUnidadeMedida()));
    List<String> avisos = new ArrayList<>();
    unidadePorPlantio.forEach(
        (plantioId, unidade) -> {
          BigDecimal produzido = totais.produzido().getOrDefault(plantioId, BigDecimal.ZERO);
          BigDecimal vendido = totais.vendido().getOrDefault(plantioId, BigDecimal.ZERO);
          if (vendido.compareTo(produzido) > 0)
            avisos.add(
                "A quantidade vendida do plantio ("
                    + quantidade(vendido)
                    + " "
                    + unidade
                    + ") supera a produção registrada ("
                    + quantidade(produzido)
                    + " "
                    + unidade
                    + "). Confira os registros.");
        });
    return avisos;
  }

  private static String quantidade(BigDecimal valor) {
    NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
    formato.setMinimumFractionDigits(3);
    formato.setMaximumFractionDigits(3);
    formato.setRoundingMode(RoundingMode.HALF_UP);
    return formato.format(valor);
  }

  private static <T extends Comparable<T>> Comparator<T> nullsLast() {
    return Comparator.nullsLast(Comparator.naturalOrder());
  }
}
