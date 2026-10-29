package com.agrogestao.api.service.plantio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class PlantioServiceTest {
  PlantioRepository repo = mock(PlantioRepository.class);
  PropriedadeRepository propriedades = mock(PropriedadeRepository.class);
  CulturaRepository culturas = mock(CulturaRepository.class);
  ProducaoRepository producoes = mock(ProducaoRepository.class);
  DespesaRepository despesas = mock(DespesaRepository.class);
  DiariaRepository diarias = mock(DiariaRepository.class);
  VendaRepository vendas = mock(VendaRepository.class);
  Clock clock = Clock.fixed(Instant.parse("2026-10-12T01:30:00Z"), ZoneId.of("America/Fortaleza"));
  PlantioService service = servico(clock);
  Cultura cultura = new Cultura();
  Propriedade propriedade = new Propriedade();

  @BeforeEach
  void preparar() {
    Produtor produtor = new Produtor();
    ReflectionTestUtils.setField(produtor, "id", 3L);
    ReflectionTestUtils.setField(propriedade, "id", 1L);
    ReflectionTestUtils.setField(cultura, "id", 2L);
    propriedade.setProdutor(produtor);
    cultura.setTipoCicloPadrao(TipoCiclo.CURTO);
    when(propriedades.findById(1L)).thenReturn(Optional.of(propriedade));
    when(culturas.findById(2L)).thenReturn(Optional.of(cultura));
    when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  PlantioService servico(Clock relogio) {
    return new PlantioService(
        repo, propriedades, culturas, producoes, despesas, diarias, vendas, relogio);
  }

  PlantioRequisicao entrada(String area, LocalDate inicio, TipoCiclo ciclo) {
    return new PlantioRequisicao(1L, 2L, new BigDecimal(area), ciclo, inicio, null);
  }

  @Test
  void deveAceitarInicioHojeENoPassadoQuandoRelogioEstiverNoCeara() {
    for (LocalDate inicio :
        java.util.List.of(LocalDate.of(2026, 10, 11), LocalDate.of(2025, 11, 20))) {
      var resposta = service.criar(entrada("0.001", inicio, null));
      assertThat(resposta.dataInicio()).isEqualTo(inicio);
      assertThat(resposta.area()).isEqualByComparingTo("0.001");
      assertThat(resposta.tipoCiclo()).isEqualTo(TipoCiclo.CURTO);
      assertThat(resposta.situacao()).isEqualTo(SituacaoPlantio.ABERTO);
    }
    verify(repo, times(2)).saveAndFlush(any());
  }

  @Test
  void deveRejeitarAmanhaQuandoUtcJaEstiverNoDiaSeguinte() {
    assertThatThrownBy(() -> service.criar(entrada("1", LocalDate.of(2026, 10, 12), null)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("A data de início não pode ser posterior a hoje.");
    verify(repo, never()).saveAndFlush(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1"})
  void deveRejeitarAreaQuandoNaoPositiva(String area) {
    assertThatThrownBy(() -> service.criar(entrada(area, LocalDate.of(2026, 2, 1), null)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("A área do plantio deve ser maior que zero.");
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void deveSobreporCicloQuandoInformado() {
    assertThat(
            service.criar(entrada("2.500", LocalDate.of(2026, 2, 1), TipoCiclo.LONGO)).tipoCiclo())
        .isEqualTo(TipoCiclo.LONGO);
    assertThat(cultura.getTipoCicloPadrao()).isEqualTo(TipoCiclo.CURTO);
  }

  @Test
  void deveRejeitarQuandoVinculoNaoExistir() {
    when(propriedades.findById(1L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.criar(entrada("1", LocalDate.of(2026, 2, 1), null)))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    when(propriedades.findById(1L)).thenReturn(Optional.of(propriedade));
    when(culturas.findById(2L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.criar(entrada("1", LocalDate.of(2026, 2, 1), null)))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void devePreservarEncerramentoQuandoAlterarInicioValido() {
    Plantio plantio = new Plantio();
    plantio.setPropriedade(propriedade);
    plantio.setDataEncerramento(LocalDate.of(2026, 6, 30));
    when(repo.buscarParaAtualizar(4L)).thenReturn(Optional.of(plantio));
    var resposta = service.alterar(4L, entrada("3.125", LocalDate.of(2026, 6, 30), null));
    assertThat(resposta.dataEncerramento()).isEqualTo(LocalDate.of(2026, 6, 30));
    assertThat(resposta.situacao()).isEqualTo(SituacaoPlantio.ENCERRADO);
    assertThat(resposta.area()).isEqualByComparingTo("3.125");
  }

  @Test
  void deveRejeitarAlteracaoQuandoInicioUltrapassarEncerramento() {
    Plantio plantio = new Plantio();
    plantio.setPropriedade(propriedade);
    plantio.setDataEncerramento(LocalDate.of(2026, 6, 30));
    when(repo.buscarParaAtualizar(4L)).thenReturn(Optional.of(plantio));
    assertThatThrownBy(() -> service.alterar(4L, entrada("1", LocalDate.of(2026, 7, 1), null)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("01/07/2026");
    verify(repo, never()).saveAndFlush(any());
  }

  Propriedade propriedadeDe(long id, long produtorId) {
    Produtor produtor = new Produtor();
    ReflectionTestUtils.setField(produtor, "id", produtorId);
    Propriedade nova = new Propriedade();
    ReflectionTestUtils.setField(nova, "id", id);
    nova.setProdutor(produtor);
    when(propriedades.findById(id)).thenReturn(Optional.of(nova));
    return nova;
  }

  PlantioRequisicao entradaEm(long propriedadeId) {
    return new PlantioRequisicao(
        propriedadeId, 2L, BigDecimal.ONE, null, LocalDate.of(2026, 2, 1), null);
  }

  @Test
  void deveRejeitarAlteracaoQuandoPropriedadeForDeOutroProdutor() {
    propriedadeDe(5L, 7L);
    Plantio plantio = new Plantio();
    plantio.setPropriedade(propriedade);
    when(repo.buscarParaAtualizar(4L)).thenReturn(Optional.of(plantio));
    assertThatThrownBy(() -> service.alterar(4L, entradaEm(5L)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("O plantio não pode ser transferido para propriedade de outro produtor.");
    assertThat(plantio.getPropriedade()).isSameAs(propriedade);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void devePermitirAlteracaoQuandoPropriedadeForDoMesmoProdutor() {
    Propriedade mesmoProdutor = propriedadeDe(6L, 3L);
    Plantio plantio = new Plantio();
    plantio.setPropriedade(propriedade);
    when(repo.buscarParaAtualizar(4L)).thenReturn(Optional.of(plantio));
    var resposta = service.alterar(4L, entradaEm(6L));
    assertThat(plantio.getPropriedade()).isSameAs(mesmoProdutor);
    assertThat(resposta.propriedadeId()).isEqualTo(6L);
    assertThat(resposta.produtorId()).isEqualTo(3L);
  }

  @Test
  void deveCriarQuandoPropriedadeForDeOutroProdutor() {
    propriedadeDe(5L, 7L);
    var resposta = service.criar(entradaEm(5L));
    assertThat(resposta.propriedadeId()).isEqualTo(5L);
    assertThat(resposta.produtorId()).isEqualTo(7L);
    verify(repo).saveAndFlush(any());
  }

  @Test
  void deveRejeitarConsultaQuandoPeriodoInvertido() {
    assertThatThrownBy(
            () ->
                service.listar(
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 1, 1),
                    new PaginacaoConsulta()))
        .isInstanceOf(RegraNegocioException.class);
    verifyNoInteractions(repo);
  }

  @Test
  void deveRetornarNaoEncontradoQuandoPlantioNaoExistir() {
    assertThatThrownBy(() -> service.detalhar(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.alterar(99L, entrada("1", LocalDate.of(2026, 2, 1), null)))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.encerrar(99L, encerramento(2026, 6, 30)))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Plantio não encontrado.");
  }

  // C06b: "hoje" é 2026-10-11 em Fortaleza, salvo no teste da armadilha do UTC.
  PlantioService meioDia =
      servico(
          Clock.fixed(Instant.parse("2026-10-11T12:00:00-03:00"), ZoneId.of("America/Fortaleza")));

  Plantio abertoDesde(LocalDate inicio) {
    Plantio plantio = new Plantio();
    ReflectionTestUtils.setField(plantio, "id", 4L);
    plantio.setPropriedade(propriedade);
    plantio.setCultura(cultura);
    plantio.setArea(BigDecimal.ONE);
    plantio.setDataInicio(inicio);
    when(repo.buscarParaAtualizar(4L)).thenReturn(Optional.of(plantio));
    return plantio;
  }

  static EncerramentoPlantioRequisicao encerramento(int ano, int mes, int dia) {
    return new EncerramentoPlantioRequisicao(LocalDate.of(ano, mes, dia));
  }

  void assertRejeitado(PlantioService alvo, EncerramentoPlantioRequisicao dto, String mensagem) {
    assertThatThrownBy(() -> alvo.encerrar(4L, dto))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(mensagem);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void deveEncerrarQuandoDataForHoje() {
    Plantio plantio = abertoDesde(LocalDate.of(2026, 2, 1));
    var resposta = meioDia.encerrar(4L, encerramento(2026, 10, 11));
    assertThat(resposta.dataEncerramento()).isEqualTo(LocalDate.of(2026, 10, 11));
    assertThat(resposta.situacao()).isEqualTo(SituacaoPlantio.ENCERRADO);
    assertThat(plantio.getDataInicio()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(plantio.getArea()).isEqualByComparingTo("1");
    verify(repo).saveAndFlush(plantio);
  }

  @Test
  void deveEncerrarQuandoDataForODiaDoInicio() {
    abertoDesde(LocalDate.of(2026, 2, 1));
    assertThat(meioDia.encerrar(4L, encerramento(2026, 2, 1)).dataEncerramento())
        .isEqualTo(LocalDate.of(2026, 2, 1));
  }

  @Test
  void deveRejeitarEncerramentoAmanhaQuandoUtcJaEstiverNoDiaSeguinte() {
    Plantio plantio = abertoDesde(LocalDate.of(2026, 2, 1));
    assertRejeitado(
        service,
        encerramento(2026, 10, 12),
        "A data de encerramento não pode ser posterior a hoje.");
    assertThat(plantio.getDataEncerramento()).isNull();
    assertThat(service.encerrar(4L, encerramento(2026, 10, 11)).situacao())
        .isEqualTo(SituacaoPlantio.ENCERRADO);
  }

  @Test
  void deveRejeitarSemGravarQuandoPlantioJaEstiverEncerrado() {
    Plantio plantio = abertoDesde(LocalDate.of(2026, 2, 1));
    plantio.setDataEncerramento(LocalDate.of(2026, 6, 30));
    assertRejeitado(
        meioDia, encerramento(2026, 7, 15), "O plantio já foi encerrado em 30/06/2026.");
    assertThat(plantio.getDataEncerramento()).isEqualTo(LocalDate.of(2026, 6, 30));
    verifyNoInteractions(producoes, despesas, diarias, vendas);
  }

  @Test
  void deveRejeitarComMensagemFixaQuandoEncerramentoForAnteriorAoInicio() {
    Plantio plantio = abertoDesde(LocalDate.of(2026, 2, 1));
    assertRejeitado(
        meioDia,
        encerramento(2026, 1, 31),
        "A data de encerramento não pode ser anterior à data de início (01/02/2026).");
    assertThat(plantio.getDataEncerramento()).isNull();
  }

  @Test
  void deveSeguirOrdemDasValidacoesQuandoMaisDeUmaFalhar() {
    Plantio plantio = abertoDesde(LocalDate.of(2026, 2, 1));
    when(producoes.ultimaDataFatoDoPlantio(4L)).thenReturn(LocalDate.of(2026, 6, 10));
    assertRejeitado(
        meioDia,
        encerramento(2026, 1, 31),
        "A data de encerramento não pode ser anterior à data de início (01/02/2026).");
    plantio.setDataEncerramento(LocalDate.of(2026, 6, 30));
    assertRejeitado(
        meioDia, encerramento(2026, 10, 12), "O plantio já foi encerrado em 30/06/2026.");
  }

  @Test
  void deveEncerrarQuandoUltimoLancamentoForNoProprioDia() {
    abertoDesde(LocalDate.of(2026, 2, 1));
    when(despesas.ultimaDataFatoDoPlantio(4L)).thenReturn(LocalDate.of(2026, 6, 30));
    when(producoes.ultimaDataFatoDoPlantio(4L)).thenReturn(LocalDate.of(2026, 6, 10));
    assertThat(meioDia.encerrar(4L, encerramento(2026, 6, 30)).situacao())
        .isEqualTo(SituacaoPlantio.ENCERRADO);
  }

  @Test
  void deveCitarLancamentoMaisTardeQuandoHouverLancamentoPosterior() {
    Plantio plantio = abertoDesde(LocalDate.of(2026, 2, 1));
    when(producoes.ultimaDataFatoDoPlantio(4L)).thenReturn(LocalDate.of(2026, 6, 10));
    when(despesas.ultimaDataFatoDoPlantio(4L)).thenReturn(LocalDate.of(2026, 7, 10));
    when(diarias.ultimaDataFatoDoPlantio(4L)).thenReturn(LocalDate.of(2026, 7, 5));
    assertRejeitado(
        meioDia,
        encerramento(2026, 6, 30),
        "Há lançamento em 10/07/2026, posterior à data de encerramento informada.");
    assertThat(plantio.getDataEncerramento()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"producao", "despesa", "diaria", "venda"})
  void deveRejeitarQuandoSoUmaFonteTiverLancamentoPosterior(String fonte) {
    abertoDesde(LocalDate.of(2026, 2, 1));
    LocalDate posterior = LocalDate.of(2026, 7, 1);
    switch (fonte) {
      case "producao" -> when(producoes.ultimaDataFatoDoPlantio(4L)).thenReturn(posterior);
      case "despesa" -> when(despesas.ultimaDataFatoDoPlantio(4L)).thenReturn(posterior);
      case "diaria" -> when(diarias.ultimaDataFatoDoPlantio(4L)).thenReturn(posterior);
      default -> when(vendas.ultimaDataFatoDoPlantio(4L)).thenReturn(posterior);
    }
    assertRejeitado(
        meioDia,
        encerramento(2026, 6, 30),
        "Há lançamento em 01/07/2026, posterior à data de encerramento informada.");
  }
}
