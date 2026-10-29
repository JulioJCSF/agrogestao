package com.agrogestao.api.service.cultura;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

class CulturaServiceTest {
  CulturaRepository repo = mock(CulturaRepository.class);
  ProducaoRepository producoes = mock(ProducaoRepository.class);
  CulturaService service = new CulturaService(repo, producoes);
  CulturaRequisicao dto = new CulturaRequisicao(" Cultura Fictícia ", TipoCiclo.CURTO, "kg");

  @Test
  void deveCriarQuandoNomeForNovo() {
    when(repo.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              Cultura c = inv.getArgument(0);
              ReflectionTestUtils.setField(c, "id", 1L);
              return c;
            });
    assertThat(service.criar(dto))
        .isEqualTo(new CulturaResposta(1L, "Cultura Fictícia", TipoCiclo.CURTO, "kg"));
  }

  @Test
  void deveRejeitarQuandoNomeDuplicarOutroCadastro() {
    when(repo.existsByNomeIgnoreCase("Cultura Fictícia")).thenReturn(true);
    assertThatThrownBy(() -> service.criar(dto)).isInstanceOf(RegraNegocioException.class);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void deveAlterarQuandoNomePertencerAoMesmoCadastro() {
    Cultura cultura = new Cultura();
    ReflectionTestUtils.setField(cultura, "id", 1L);
    when(repo.buscarParaAtualizar(1L)).thenReturn(Optional.of(cultura));
    when(repo.saveAndFlush(cultura)).thenReturn(cultura);
    assertThat(service.alterar(1L, dto).nome()).isEqualTo("Cultura Fictícia");
    verify(repo).existsByNomeIgnoreCaseAndIdNot("Cultura Fictícia", 1L);
  }

  @Test
  void deveRejeitarAlteracaoQuandoNomePertencerAOutraCultura() {
    when(repo.buscarParaAtualizar(1L)).thenReturn(Optional.of(new Cultura()));
    when(repo.existsByNomeIgnoreCaseAndIdNot("Cultura Fictícia", 1L)).thenReturn(true);
    assertThatThrownBy(() -> service.alterar(1L, dto)).isInstanceOf(RegraNegocioException.class);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void deveRetornarNaoEncontradoQuandoIdInexistente() {
    assertThatThrownBy(() -> service.detalhar(999L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
