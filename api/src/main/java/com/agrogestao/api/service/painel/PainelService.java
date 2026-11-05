package com.agrogestao.api.service.painel;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Plantio;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.apuracao.ApuracaoResultadoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Painel (M07): totais por período (RF48) e resultado de cada plantio para comparar culturas
 * (RF49). Não faz conta própria: cada plantio vem da apuração em lote, com o período informado,
 * como no relatório (A03a), e os totais são a soma dos plantios. Quantidades não são somadas:
 * cultura diferente tem unidade diferente; elas ficam por plantio.
 *
 * <p>Decisões de 04/11: sem {@code produtorId}, o painel é o geral, com todos os plantios. Sem
 * período, vai de 01/01 do ano corrente até hoje, em Fortaleza; só com início, vai até hoje; só com
 * fim, começa em 01/01 do ano do fim. Painel é só leitura e não trava o produtor.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PainelService {
  static final String PERIODO_INVERTIDO = "O fim do período não pode ser anterior ao início.";
  static final String PRODUTOR_NAO_ENCONTRADO = "Produtor não encontrado.";
  private static final int ESCALA_DINHEIRO = 2;

  private final ProdutorRepository produtores;
  private final PlantioRepository plantios;
  private final ApuracaoResultadoService apuracao;
  private final Clock relogio;

  /** Valida nesta ordem: período (422) e produtor (404). */
  public PainelResposta consultar(LocalDate periodoInicio, LocalDate periodoFim, Long produtorId) {
    LocalDate fim = periodoFim != null ? periodoFim : LocalDate.now(relogio);
    LocalDate inicio = periodoInicio != null ? periodoInicio : fim.withDayOfYear(1);
    if (fim.isBefore(inicio)) throw new RegraNegocioException(PERIODO_INVERTIDO);
    if (produtorId != null && !produtores.existsById(produtorId))
      throw new RecursoNaoEncontradoException(PRODUTOR_NAO_ENCONTRADO);

    List<Plantio> ativos =
        (produtorId == null
                ? plantios.buscarTodosComCultura()
                : plantios.buscarDoProdutorComCultura(produtorId))
            .stream().filter(plantio -> plantio.ativoNoPeriodo(inicio, fim)).toList();
    List<ResultadoPlantioResposta> apurados =
        List.copyOf(apuracao.apurarVarios(ativos, inicio, fim).values());
    BigDecimal receita = somar(apurados, ResultadoPlantioResposta::receita);
    BigDecimal custoDesembolso = somar(apurados, ResultadoPlantioResposta::custoDesembolso);
    // RN23 e RN25: com sinal.
    return new PainelResposta(
        inicio, fim, receita, custoDesembolso, receita.subtract(custoDesembolso), apurados);
  }

  private static BigDecimal somar(
      List<ResultadoPlantioResposta> apurados,
      Function<ResultadoPlantioResposta, BigDecimal> campo) {
    return apurados.stream()
        .map(campo)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);
  }
}
