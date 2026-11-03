package com.agrogestao.api.service.producao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.ProducaoRequisicao;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class ProducaoServiceTest {
  ProducaoRepository repository = mock(ProducaoRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  CulturaRepository culturas = mock(CulturaRepository.class);
  LancamentoService lancamentos =
      new LancamentoService(
          Clock.fixed(Instant.parse("2026-10-12T01:30:00Z"), ZoneId.of("America/Fortaleza")));
  ProducaoService service = new ProducaoService(repository, plantios, culturas, lancamentos);
  Plantio plantio = new Plantio();

  @BeforeEach
  void preparar() {
    ReflectionTestUtils.setField(plantio, "id", 7L);
    Cultura cultura = new Cultura();
    ReflectionTestUtils.setField(cultura, "id", 2L);
    cultura.setUnidadeMedida("kg");
    plantio.setCultura(cultura);
    when(plantios.findById(7L)).thenReturn(Optional.of(plantio));
    when(plantios.buscarParaAtualizar(7L)).thenReturn(Optional.of(plantio));
    when(culturas.buscarParaAtualizar(2L)).thenReturn(Optional.of(cultura));
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              Producao p = inv.getArgument(0);
              ReflectionTestUtils.setField(p, "id", 1L);
              lancamentos.atribuirDataColeta(p);
              return p;
            });
  }

  ProducaoRequisicao dto(String quantidade, String unidade, String data) {
    return new ProducaoRequisicao(new BigDecimal(quantidade), unidade, LocalDate.parse(data));
  }

  @Test
  void deveUsarCulturaQuandoUnidadeForOmitida() {
    var resposta = service.criar(7L, dto("250.125", null, "2024-03-10"));
    assertThat(resposta.unidadeMedida()).isEqualTo("kg");
    assertThat(resposta.quantidade()).isEqualByComparingTo("250.125");
    assertThat(resposta.dataColeta()).isEqualTo(LocalDate.of(2026, 10, 11));
  }

  @Test
  void devePreservarUnidadeInformadaEColetaQuandoAlterar() {
    Producao p = new Producao();
    p.setPlantio(plantio);
    p.setDataColeta(LocalDate.of(2024, 3, 11));
    p.setColetaAtribuida(true);
    when(repository.findById(1L)).thenReturn(Optional.of(p));
    var resposta = service.alterar(1L, dto("0.001", " KG ", "2024-03-12"));
    assertThat(resposta.unidadeMedida()).isEqualTo("kg");
    assertThat(resposta.quantidade()).isEqualByComparingTo("0.001");
    assertThat(resposta.dataColeta()).isEqualTo(LocalDate.of(2024, 3, 11));
  }

  @Test
  void deveRejeitarSemGravarQuandoQuantidadeForNaoPositiva() {
    for (String q : List.of("0", "-0.001"))
      assertThatThrownBy(() -> service.criar(7L, dto(q, null, "2024-03-10")))
          .isInstanceOf(RegraNegocioException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void deveValidarDatasAntesDeGravarQuandoCriarOuAlterar() {
    plantio.setDataEncerramento(LocalDate.of(2024, 6, 30));
    Producao p = new Producao();
    p.setPlantio(plantio);
    when(repository.findById(1L)).thenReturn(Optional.of(p));
    for (String data : List.of("2024-07-01", "2026-10-12")) {
      assertThatThrownBy(() -> service.criar(7L, dto("1", null, data)))
          .isInstanceOf(RegraNegocioException.class);
      assertThatThrownBy(() -> service.alterar(1L, dto("1", null, data)))
          .isInstanceOf(RegraNegocioException.class);
    }
    verify(repository, never()).saveAndFlush(any());
    assertThat(service.criar(7L, dto("1", null, "2024-06-30")).dataFato())
        .isEqualTo(LocalDate.of(2024, 6, 30));
  }

  @Test
  void deveResponderNaoEncontradoQuandoRegistroNaoExistir() {
    assertThatThrownBy(() -> service.criar(99L, dto("1", null, "2024-03-10")))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.detalhar(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.excluir(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    verify(repository, never()).delete(any());
  }
}
