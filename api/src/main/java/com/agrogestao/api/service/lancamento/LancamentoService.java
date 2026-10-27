package com.agrogestao.api.service.lancamento;

import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.model.Lancamento;
import com.agrogestao.api.model.Plantio;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de data comuns a produção, despesa, diária e venda: RN35, RN04 e RN36.
 *
 * <p>As APIs de lançamento (L01, L04a, L07, L09b) chamam {@link #validarDataFato} no service, antes
 * de gravar, na criação e na alteração, passando os plantios que já carregaram. Por exemplo, na
 * despesa rateada, {@code lancamentos.validarDataFato(dto.dataFato(), plantiosDaDespesa)}; na
 * venda, {@code lancamentos.validarDataFato(dto.dataFato(),
 * itens.stream().map(ItemVenda::getPlantio).toList())}. A data de coleta não precisa de chamada:
 * {@link DataColetaListener} a atribui ao persistir. A L07 só chama {@link #herdarDataColeta} para
 * a despesa gerada pela diária paga.
 */
@Service
@RequiredArgsConstructor
public class LancamentoService {
  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final Clock relogio;

  /** Atalho para lançamento de um plantio só (produção, diária). */
  public void validarDataFato(LocalDate dataFato, Plantio plantio) {
    validarDataFato(dataFato, List.of(plantio));
  }

  /**
   * Valida a data do fato: primeiro a RN35 (não posterior a hoje em {@code America/Fortaleza}),
   * depois a RN04 (não posterior ao encerramento de nenhum dos plantios). Bordas inclusivas: hoje e
   * o próprio dia do encerramento são aceitos. Plantio sem data de encerramento está aberto.
   *
   * <p>Basta um plantio encerrado antes da data do fato para rejeitar o lançamento inteiro. Se
   * houver mais de um, a mensagem cita o encerramento mais antigo.
   *
   * <p>Lista nula ou vazia só valida a RN35: o vínculo mínimo é a RN06 (L04a) e a RN26 (L09b), que
   * o chamador valida.
   *
   * @throws RegraNegocioException (422) se a data do fato for futura ou posterior a um encerramento
   * @throws IllegalArgumentException se {@code dataFato} for nula ou a lista tiver plantio nulo; é
   *     erro de programação, porque a API já rejeita a data ausente com 400 pelo {@code @NotNull}
   */
  public void validarDataFato(LocalDate dataFato, Collection<Plantio> plantios) {
    if (dataFato == null) {
      throw new IllegalArgumentException("Data do fato do lançamento não pode ser nula.");
    }
    if (dataFato.isAfter(LocalDate.now(relogio))) {
      throw new RegraNegocioException("A data do fato não pode ser posterior a hoje.");
    }
    if (plantios == null) {
      return;
    }
    if (plantios.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Lista de plantios do lançamento contém item nulo.");
    }
    plantios.stream()
        .map(Plantio::getDataEncerramento)
        .filter(encerramento -> encerramento != null && dataFato.isAfter(encerramento))
        .min(Comparator.naturalOrder())
        .ifPresent(
            encerramento -> {
              throw new RegraNegocioException(
                  "O plantio foi encerrado em "
                      + encerramento.format(DATA)
                      + ". A data do fato não pode ser posterior ao encerramento.");
            });
  }

  /**
   * Atribui hoje como data de coleta (RN36). Descarta valor posto por {@code setDataColeta}, mas
   * não sobrescreve coleta já atribuída ou herdada pela base. Chamado por {@link
   * DataColetaListener} ao persistir; na alteração, a coluna é {@code updatable = false}.
   */
  public void atribuirDataColeta(Lancamento lancamento) {
    if (lancamento.isColetaAtribuida()) {
      return;
    }
    lancamento.setDataColeta(LocalDate.now(relogio));
    lancamento.setColetaAtribuida(true);
  }

  /**
   * Copia a data de coleta de {@code origem} para {@code destino}, que o listener então mantém. Uso
   * da L07: depois de salvar a diária paga, {@code lancamentos.herdarDataColeta(diaria, despesa)}
   * antes de salvar a despesa gerada (RN16).
   *
   * @throws IllegalArgumentException se {@code origem} ainda não tiver data de coleta
   */
  public void herdarDataColeta(Lancamento origem, Lancamento destino) {
    if (origem.getDataColeta() == null) {
      throw new IllegalArgumentException("Lançamento de origem ainda não tem data de coleta.");
    }
    destino.setDataColeta(origem.getDataColeta());
    destino.setColetaAtribuida(true);
  }
}
