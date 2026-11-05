package com.agrogestao.api.service.apuracao;

import com.agrogestao.api.dto.ResultadoPlantioResposta;
import com.agrogestao.api.exception.RecursoNaoEncontradoException;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apuração do resultado do plantio (A01): RN19 a RN25.
 *
 * <p>Cada fonte é lida inteira para os plantios apurados (uma consulta por fonte, para todos eles)
 * e o recorte por período e por estorno é feito aqui, em Java, para que os casos unitários da Q00d
 * o exercitem.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApuracaoResultadoService {
  private static final int ESCALA_DINHEIRO = 2;
  private static final int ESCALA_QUANTIDADE = 3;
  private static final int BLOCO = 1000;

  private final PlantioRepository plantios;
  private final RateioDespesaRepository rateios;
  private final DiariaRepository diarias;
  private final ProducaoRepository producoes;
  private final ItemVendaRepository itensVenda;
  private final Clock relogio;

  /**
   * Apura pelo período da RN24, derivado do ciclo efetivo do plantio ({@code plantio.tipoCiclo},
   * nunca o da cultura): ciclo longo do início ao encerramento, ou até hoje se aberto, ignorando
   * {@code mes}; ciclo curto no mês-calendário de {@code mes}, ou no mês corrente.
   */
  public ResultadoPlantioResposta apurar(Long plantioId, YearMonth mes) {
    Plantio plantio =
        plantios
            .findById(plantioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Plantio não encontrado."));
    LocalDate hoje = LocalDate.now(relogio);
    if (plantio.getTipoCiclo() == TipoCiclo.LONGO) {
      LocalDate fim = plantio.getDataEncerramento() == null ? hoje : plantio.getDataEncerramento();
      return apurar(plantio, plantio.getDataInicio(), fim);
    }
    YearMonth mesApurado = mes == null ? YearMonth.from(hoje) : mes;
    return apurar(plantio, mesApurado.atDay(1), mesApurado.atEndOfMonth());
  }

  /**
   * Apura o plantio num período já definido, com as duas bordas inclusivas. A emissão do relatório
   * (A03a) e o painel (M07) usam o lote, {@link #apurarVarios}; este método é o lote de um plantio.
   */
  public ResultadoPlantioResposta apurar(Plantio plantio, LocalDate inicio, LocalDate fim) {
    return apurarVarios(List.of(plantio), inicio, fim).get(plantio.getId());
  }

  /**
   * Apura vários plantios no mesmo período com uma consulta por fonte para todos eles (em blocos de
   * {@value #BLOCO} ids), em vez de uma por plantio. Cada plantio passa pela mesma conta de {@link
   * #apurar(Plantio, LocalDate, LocalDate)}. O mapa segue a ordem da lista; lista vazia não
   * consulta.
   */
  public Map<Long, ResultadoPlantioResposta> apurarVarios(
      List<Plantio> lista, LocalDate inicio, LocalDate fim) {
    Map<Long, ResultadoPlantioResposta> apurados = new LinkedHashMap<>();
    for (int de = 0; de < lista.size(); de += BLOCO) {
      List<Plantio> bloco = lista.subList(de, Math.min(de + BLOCO, lista.size()));
      List<Long> ids = bloco.stream().map(Plantio::getId).toList();
      Map<Long, List<RateioDespesa>> rateiosPorPlantio =
          porPlantio(rateios.buscarDosPlantiosComDespesa(ids), RateioDespesa::getPlantio);
      Map<Long, List<Diaria>> diariasPorPlantio =
          porPlantio(diarias.buscarDosPlantios(ids), Diaria::getPlantio);
      Map<Long, List<Producao>> producoesPorPlantio =
          porPlantio(producoes.buscarDosPlantios(ids), Producao::getPlantio);
      Map<Long, List<ItemVenda>> itensPorPlantio =
          porPlantio(itensVenda.buscarDosPlantiosComVenda(ids), ItemVenda::getPlantio);
      for (Plantio plantio : bloco) {
        Long id = plantio.getId();
        apurados.put(
            id,
            calcular(
                plantio,
                inicio,
                fim,
                rateiosPorPlantio.getOrDefault(id, List.of()),
                diariasPorPlantio.getOrDefault(id, List.of()),
                producoesPorPlantio.getOrDefault(id, List.of()),
                itensPorPlantio.getOrDefault(id, List.of())));
      }
    }
    return apurados;
  }

  private static <T> Map<Long, List<T>> porPlantio(
      List<T> registros, Function<T, Plantio> plantio) {
    return registros.stream().collect(Collectors.groupingBy(r -> plantio.apply(r).getId()));
  }

  /** A conta de um plantio, sobre a lista completa de registros de cada fonte. */
  private static ResultadoPlantioResposta calcular(
      Plantio plantio,
      LocalDate inicio,
      LocalDate fim,
      List<RateioDespesa> rateiosDoPlantio,
      List<Diaria> diariasDoPlantio,
      List<Producao> producoesDoPlantio,
      List<ItemVenda> itensDoPlantio) {
    Predicate<LocalDate> noPeriodo = data -> !data.isBefore(inicio) && !data.isAfter(fim);
    Long id = plantio.getId();

    // RN19: valor rateado gravado ao plantio, de despesa não estornada. RN20: a diária paga entra
    // só por aqui, pela despesa que gerou; diária nenhuma é somada ao desembolso diretamente.
    BigDecimal custoDesembolso =
        somar(
            rateiosDoPlantio.stream()
                .filter(rateio -> !rateio.getDespesa().isEstornada())
                .filter(rateio -> noPeriodo.test(rateio.getDespesa().getDataFato()))
                .map(RateioDespesa::getValorRateado),
            ESCALA_DINHEIRO);

    // RN21: cada diária familiar arredondada em 2 casas antes de somar (decisão 7 da Q00c).
    BigDecimal valorMaoObraFamiliar =
        somar(
            diariasDoPlantio.stream()
                .filter(diaria -> diaria.getNatureza() == NaturezaDiaria.FAMILIAR)
                .filter(diaria -> noPeriodo.test(diaria.getDataFato()))
                .map(
                    diaria ->
                        diaria
                            .getQuantidadeDias()
                            .multiply(diaria.getValorUnitario())
                            .setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP)),
            ESCALA_DINHEIRO);

    // RN22: itens deste plantio, pela data da venda; nunca o total da venda.
    List<ItemVenda> itens =
        itensDoPlantio.stream()
            .filter(item -> noPeriodo.test(item.getVenda().getDataFato()))
            .toList();
    BigDecimal receita = somar(itens.stream().map(ItemVenda::getValorTotal), ESCALA_DINHEIRO);

    // RN25: produzido e vendido no mesmo período do resultado (decisão de 03/11).
    BigDecimal quantidadeProduzida =
        somar(
            producoesDoPlantio.stream()
                .filter(producao -> noPeriodo.test(producao.getDataFato()))
                .map(Producao::getQuantidade),
            ESCALA_QUANTIDADE);
    BigDecimal quantidadeVendida =
        somar(itens.stream().map(ItemVenda::getQuantidade), ESCALA_QUANTIDADE);

    return new ResultadoPlantioResposta(
        id,
        plantio.getCultura().getNome(),
        plantio.getPropriedade().getNome(),
        plantio.getTipoCiclo(),
        inicio,
        fim,
        quantidadeProduzida,
        quantidadeVendida,
        plantio.getCultura().getUnidadeMedida(),
        receita,
        custoDesembolso,
        valorMaoObraFamiliar,
        // RN21: complementar, ao lado do desembolso; nunca o substitui.
        custoDesembolso.add(valorMaoObraFamiliar),
        // RN23 e RN25: com sinal, negativo inclusive.
        receita.subtract(custoDesembolso));
  }

  private static BigDecimal somar(Stream<BigDecimal> valores, int escala) {
    return valores.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(escala, RoundingMode.HALF_UP);
  }
}
