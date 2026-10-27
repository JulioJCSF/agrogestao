package com.agrogestao.api.service.referencia;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.ValorReferenciaDiaria;
import com.agrogestao.api.repository.ValorReferenciaDiariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

class ValorReferenciaDiariaServiceTest {
  ValorReferenciaDiariaRepository repo = mock(ValorReferenciaDiariaRepository.class);
  ValorReferenciaDiariaService service = new ValorReferenciaDiariaService(repo);
  LocalDate inicio = LocalDate.of(2024, 1, 1);
  ValorReferenciaDiariaRequisicao dto =
      new ValorReferenciaDiariaRequisicao(new BigDecimal("80.25"), inicio, null);

  @Test
  void deveCriarQuandoPeriodoLivre() {
    when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    var resposta = service.criar(dto);
    assertThat(resposta.valor()).isEqualByComparingTo("80.25");
    assertThat(resposta.vigenciaFim()).isNull();
    verify(repo).existeSobreposicao(inicio, null, -1L);
  }

  @Test
  void deveRejeitarCriacaoEAlteracaoQuandoPeriodoSobreposto() {
    when(repo.existeSobreposicao(inicio, null, -1L)).thenReturn(true);
    when(repo.existeSobreposicao(inicio, null, 1L)).thenReturn(true);
    when(repo.findById(1L)).thenReturn(Optional.of(new ValorReferenciaDiaria()));
    assertThatThrownBy(() -> service.criar(dto))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("sobreposição");
    assertThatThrownBy(() -> service.alterar(1L, dto)).isInstanceOf(RegraNegocioException.class);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void deveIgnorarProprioIdQuandoAlterarSemConflito() {
    when(repo.findById(1L)).thenReturn(Optional.of(new ValorReferenciaDiaria()));
    when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    assertThat(service.alterar(1L, dto).valor()).isEqualByComparingTo("80.25");
    verify(repo).existeSobreposicao(inicio, null, 1L);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-0.01"})
  void deveRejeitarQuandoValorNaoPositivo(String valor) {
    assertThatThrownBy(
            () ->
                service.criar(
                    new ValorReferenciaDiariaRequisicao(new BigDecimal(valor), inicio, null)))
        .isInstanceOf(RegraNegocioException.class);
    verifyNoInteractions(repo);
  }

  @Test
  void deveRejeitarQuandoFimAnteriorAoInicio() {
    assertThatThrownBy(
            () ->
                service.criar(
                    new ValorReferenciaDiariaRequisicao(
                        BigDecimal.ONE, inicio, inicio.minusDays(1))))
        .isInstanceOf(RegraNegocioException.class);
    verifyNoInteractions(repo);
  }

  @Test
  void deveOrientarCadastroQuandoNaoHouverVigente() {
    assertThatThrownBy(() -> service.vigente(inicio))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessageContaining("Cadastre");
    assertThatThrownBy(() -> service.detalhar(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.alterar(99L, dto))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveConsultarValorQuandoReferenciaVigente() {
    var referencia = new ValorReferenciaDiaria();
    referencia.setValor(new BigDecimal("80.25"));
    referencia.setVigenciaInicio(inicio);
    when(repo.buscarVigente(inicio)).thenReturn(Optional.of(referencia));
    assertThat(service.vigente(inicio).valor()).isEqualByComparingTo("80.25");
  }

  @Test
  void deveTraduzirConflitoQuandoConstraintBloquearConcorrencia() {
    when(repo.saveAndFlush(any()))
        .thenThrow(
            new DataIntegrityViolationException(
                "conflito", new java.sql.SQLException("ex_vrd_vigencia")));
    assertThatThrownBy(() -> service.criar(dto))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("sobreposição");
  }

  @Test
  void devePreservarTratamentoPadraoQuandoOutraConstraintFalhar() {
    when(repo.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("outra constraint"));
    assertThatThrownBy(() -> service.criar(dto))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
