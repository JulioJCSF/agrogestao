package com.agrogestao.api.service.painel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.apuracao.ApuracaoResultadoService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * M07 com repositórios e apuração mockados: período padrão (decisão 2 de 04/11), plantios ativos no
 * período (as bordas da A03a), produtor opcional e totais como soma dos plantios.
 */
class PainelServiceTest {
  static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");
  static final Clock HOJE =
      Clock.fixed(OffsetDateTime.parse("2026-11-04T12:00:00-03:00").toInstant(), FORTALEZA);
  static final LocalDate INICIO = LocalDate.parse("2026-01-01");
  static final LocalDate FIM = LocalDate.parse("2026-06-30");

  ProdutorRepository produtores = mock(ProdutorRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  ApuracaoResultadoService apuracao = mock(ApuracaoResultadoService.class);
  PainelService service = servico(HOJE);

  Cultura milho = new Cultura();
  List<Plantio> doProdutor = new ArrayList<>();
  List<Plantio> todos = new ArrayList<>();
  Map<Long, String[]> valores = new HashMap<>();
  long proximoId = 100;

  PainelService servico(Clock relogio) {
    return new PainelService(produtores, plantios, apuracao, relogio);
  }

  @BeforeEach
  void preparar() {
    milho.setNome("Milho");
    milho.setUnidadeMedida("kg");
    when(produtores.existsById(1L)).thenReturn(true);
    when(plantios.buscarDoProdutorComCultura(1L)).thenReturn(doProdutor);
    when(plantios.buscarTodosComCultura()).thenReturn(todos);
    when(apuracao.apurarVarios(anyList(), any(), any()))
        .thenAnswer(
            inv -> {
              Map<Long, ResultadoPlantioResposta> lote = new LinkedHashMap<>();
              for (Plantio plantio : inv.<List<Plantio>>getArgument(0))
                lote.put(
                    plantio.getId(),
                    apurado(
                        plantio, valores.getOrDefault(plantio.getId(), new String[] {"0", "0"})));
              return lote;
            });
  }

  Plantio plantio(boolean doProdutor1, String inicio, String encerramento) {
    Plantio plantio = new Plantio();
    ReflectionTestUtils.setField(plantio, "id", proximoId++);
    plantio.setCultura(milho);
    plantio.setTipoCiclo(TipoCiclo.LONGO);
    plantio.setDataInicio(LocalDate.parse(inicio));
    plantio.setDataEncerramento(encerramento == null ? null : LocalDate.parse(encerramento));
    if (doProdutor1) doProdutor.add(plantio);
    todos.add(plantio);
    return plantio;
  }

  static ResultadoPlantioResposta apurado(Plantio plantio, String[] receitaECusto) {
    BigDecimal receita = new BigDecimal(receitaECusto[0]);
    BigDecimal custo = new BigDecimal(receitaECusto[1]);
    return new ResultadoPlantioResposta(
        plantio.getId(),
        "Milho",
        "Sítio Teste",
        plantio.getTipoCiclo(),
        INICIO,
        FIM,
        new BigDecimal("0.000"),
        new BigDecimal("0.000"),
        "kg",
        receita,
        custo,
        BigDecimal.ZERO,
        custo,
        receita.subtract(custo));
  }

  static void assertPeriodo(PainelResposta painel, String inicio, String fim) {
    assertThat(painel.periodoInicio()).isEqualTo(LocalDate.parse(inicio));
    assertThat(painel.periodoFim()).isEqualTo(LocalDate.parse(fim));
  }

  @Test
  void deveUsarDoComecoDoAnoAteHojeQuandoNaoHouverPeriodo() {
    assertPeriodo(service.consultar(null, null, 1L), "2026-01-01", "2026-11-04");
  }

  @Test
  void deveIrAteHojeQuandoSoHouverInicio() {
    assertPeriodo(
        service.consultar(LocalDate.parse("2026-06-01"), null, 1L), "2026-06-01", "2026-11-04");
  }

  @Test
  void deveComecarNoAnoDoFimQuandoSoHouverFim() {
    assertPeriodo(
        service.consultar(null, LocalDate.parse("2025-08-31"), 1L), "2025-01-01", "2025-08-31");
  }

  @Test
  void deveUsarAnoDeFortalezaQuandoUtcJaVirouOAno() {
    // 01/01/2027 01h30 em UTC ainda é 31/12/2026 22h30 em Fortaleza.
    Clock virada = Clock.fixed(Instant.parse("2027-01-01T01:30:00Z"), FORTALEZA);
    assertPeriodo(servico(virada).consultar(null, null, 1L), "2026-01-01", "2026-12-31");
  }

  @Test
  void deveRecusarSemConsultarQuandoPeriodoForInvertido() {
    assertThatThrownBy(() -> service.consultar(FIM, INICIO, 1L))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("O fim do período não pode ser anterior ao início.");
    // Só início, depois de hoje: o período padrão fica invertido.
    assertThatThrownBy(() -> service.consultar(LocalDate.parse("2026-11-05"), null, null))
        .isInstanceOf(RegraNegocioException.class);
    verifyNoInteractions(plantios, apuracao);
  }

  @Test
  void deveRecusarComNaoEncontradoQuandoProdutorNaoExistir() {
    assertThatThrownBy(() -> service.consultar(INICIO, FIM, 99L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Produtor não encontrado.");
    verifyNoInteractions(plantios, apuracao);
  }

  @Test
  void deveIncluirSoPlantiosAtivosNoPeriodoQuandoConsultar() {
    Plantio aberto = plantio(true, "2026-02-01", null);
    Plantio encerradoNoInicio = plantio(true, "2025-08-01", "2026-01-01");
    Plantio comecaNoFim = plantio(true, "2026-06-30", null);
    plantio(true, "2026-07-01", null);
    plantio(true, "2025-08-01", "2025-12-31");

    assertThat(service.consultar(INICIO, FIM, 1L).plantios())
        .extracting(ResultadoPlantioResposta::plantioId)
        .containsExactly(aberto.getId(), encerradoNoInicio.getId(), comecaNoFim.getId());
    verify(apuracao).apurarVarios(List.of(aberto, encerradoNoInicio, comecaNoFim), INICIO, FIM);
  }

  @Test
  void deveSomarOsPlantiosQuandoMontarOsTotais() {
    Plantio milhoCt050 = plantio(true, "2026-02-01", null);
    Plantio hortalica = plantio(true, "2026-02-01", null);
    valores.put(milhoCt050.getId(), new String[] {"900.00", "696.67"});
    valores.put(hortalica.getId(), new String[] {"90.00", "33.33"});

    PainelResposta painel = service.consultar(INICIO, FIM, 1L);
    assertThat(painel.plantios())
        .extracting(ResultadoPlantioResposta::resultado)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("203.33"), new BigDecimal("56.67"));
    assertThat(painel.receita()).isEqualTo(new BigDecimal("990.00"));
    assertThat(painel.custoDesembolso()).isEqualTo(new BigDecimal("730.00"));
    assertThat(painel.resultado()).isEqualTo(new BigDecimal("260.00"));
  }

  @Test
  void deveDevolverZerosComEscalaQuandoNaoHouverPlantio() {
    PainelResposta painel = service.consultar(INICIO, FIM, 1L);
    assertThat(painel.plantios()).isEmpty();
    assertThat(painel.receita()).isEqualTo(new BigDecimal("0.00"));
    assertThat(painel.custoDesembolso()).isEqualTo(new BigDecimal("0.00"));
    assertThat(painel.resultado()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void deveSomarTodosOsPlantiosQuandoNaoHouverProdutor() {
    Plantio deA = plantio(true, "2026-02-01", null);
    Plantio deB = plantio(false, "2026-03-01", null);
    valores.put(deA.getId(), new String[] {"100.00", "40.00"});
    valores.put(deB.getId(), new String[] {"50.00", "80.00"});

    PainelResposta painel = service.consultar(INICIO, FIM, null);
    assertThat(painel.plantios())
        .extracting(ResultadoPlantioResposta::plantioId)
        .containsExactly(deA.getId(), deB.getId());
    assertThat(painel.resultado()).isEqualTo(new BigDecimal("30.00"));
    verify(plantios, never()).buscarDoProdutorComCultura(anyLong());
    verifyNoInteractions(produtores);
  }
}
