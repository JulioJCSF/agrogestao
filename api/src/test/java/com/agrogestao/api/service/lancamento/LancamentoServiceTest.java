package com.agrogestao.api.service.lancamento;

import static org.assertj.core.api.Assertions.*;

import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.model.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** "Hoje" é 11/10/2026 às 22h30 em Fortaleza, já 12/10 em UTC (CT055 e CT056, borda). */
class LancamentoServiceTest {
  static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");
  static final String FUTURA = "A data do fato não pode ser posterior a hoje.";

  LancamentoService service =
      new LancamentoService(Clock.fixed(Instant.parse("2026-10-12T01:30:00Z"), FORTALEZA));
  Plantio pl1 = plantio(LocalDate.of(2026, 6, 30));
  Plantio pl2 = plantio(null);
  Plantio pl3 = plantio(LocalDate.of(2026, 5, 31));

  static Plantio plantio(LocalDate encerramento) {
    Plantio plantio = new Plantio();
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    plantio.setDataEncerramento(encerramento);
    return plantio;
  }

  static String encerrado(String data) {
    return "O plantio foi encerrado em "
        + data
        + ". A data do fato não pode ser posterior ao encerramento.";
  }

  @Test
  void deveAceitarLancamentoQuandoDataDoFatoForAteOEncerramento() {
    assertThatNoException()
        .isThrownBy(
            () -> {
              service.validarDataFato(LocalDate.of(2026, 6, 30), pl1);
              service.validarDataFato(LocalDate.of(2026, 6, 15), pl1);
              service.validarDataFato(LocalDate.of(2026, 9, 1), pl2);
              service.validarDataFato(LocalDate.of(2026, 6, 30), List.of(pl1, pl2));
            });
  }

  @Test
  void deveRejeitarLancamentoQuandoDataDoFatoForPosteriorAoEncerramento() {
    assertThatThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 7, 1), pl1))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(encerrado("30/06/2026"));
    assertThatThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 7, 1), List.of(pl1, pl2)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(encerrado("30/06/2026"));
  }

  @Test
  void deveAceitarHojeQuandoAindaForNoiteEmFortaleza() {
    assertThatNoException()
        .isThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 10, 11), pl2));
  }

  @Test
  void deveRejeitarDataFuturaQuandoAmanhaEmFortalezaJaForHojeEmUtc() {
    for (LocalDate futura : List.of(LocalDate.of(2026, 10, 12), LocalDate.of(2099, 1, 1))) {
      assertThatThrownBy(() -> service.validarDataFato(futura, pl2))
          .isInstanceOf(RegraNegocioException.class)
          .hasMessage(FUTURA);
    }
  }

  @Test
  void deveAceitarDataDoFatoQuandoForPassadoDistante() {
    assertThatNoException()
        .isThrownBy(
            () -> {
              service.validarDataFato(LocalDate.of(2026, 3, 10), pl2);
              service.validarDataFato(LocalDate.of(2000, 1, 1), pl2);
            });
  }

  @Test
  void deveCitarDataFuturaQuandoDataDoFatoForFuturaEPlantioEncerrado() {
    assertThatThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 10, 12), pl1))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(FUTURA);
  }

  @Test
  void deveCitarEncerramentoMaisAntigoQuandoDoisPlantiosEstiveremEncerrados() {
    for (List<Plantio> plantios : List.of(List.of(pl1, pl3), List.of(pl3, pl1))) {
      assertThatThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 7, 1), plantios))
          .isInstanceOf(RegraNegocioException.class)
          .hasMessage(encerrado("31/05/2026"));
    }
  }

  @Test
  void deveValidarSoADataFuturaQuandoListaDePlantiosForNulaOuVazia() {
    assertThatNoException()
        .isThrownBy(
            () -> {
              service.validarDataFato(LocalDate.of(2026, 7, 1), (List<Plantio>) null);
              service.validarDataFato(LocalDate.of(2026, 7, 1), List.of());
            });
    assertThatThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 10, 12), List.of()))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(FUTURA);
  }

  @Test
  void deveRejeitarComoErroDeProgramacaoQuandoDataDoFatoOuPlantioForNulo() {
    assertThatThrownBy(() -> service.validarDataFato(null, pl2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Data do fato do lançamento não pode ser nula.");
    List<Plantio> comNulo = new ArrayList<>();
    comNulo.add(pl2);
    comNulo.add(null);
    assertThatThrownBy(() -> service.validarDataFato(LocalDate.of(2026, 7, 1), comNulo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Lista de plantios do lançamento contém item nulo.");
  }

  @Test
  void deveAtribuirHojeEmFortalezaQuandoGravarLancamento() {
    Producao producao = new Producao();
    producao.setDataColeta(LocalDate.of(2020, 1, 1));
    service.atribuirDataColeta(producao);
    assertThat(producao.getDataColeta()).isEqualTo(LocalDate.of(2026, 10, 11));
    assertThat(producao.isColetaAtribuida()).isTrue();
  }

  @Test
  void deveManterColetaQuandoBaseJaTiverAtribuido() {
    Producao producao = new Producao();
    new LancamentoService(Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), FORTALEZA))
        .atribuirDataColeta(producao);
    new LancamentoService(Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), FORTALEZA))
        .atribuirDataColeta(producao);
    assertThat(producao.getDataColeta()).isEqualTo(LocalDate.of(2026, 10, 1));
  }

  @Test
  void deveManterColetaDaDiariaQuandoDespesaGeradaHerdar() {
    Diaria diaria = new Diaria();
    diaria.setDataColeta(LocalDate.of(2026, 10, 1));
    Despesa despesa = new Despesa();
    service.herdarDataColeta(diaria, despesa);
    service.atribuirDataColeta(despesa);
    assertThat(despesa.getDataColeta()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThatThrownBy(() -> service.herdarDataColeta(new Diaria(), new Despesa()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
