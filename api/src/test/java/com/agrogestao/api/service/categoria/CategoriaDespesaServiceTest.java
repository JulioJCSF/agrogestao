package com.agrogestao.api.service.categoria;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

class CategoriaDespesaServiceTest {
  CategoriaDespesaRepository repo = mock(CategoriaDespesaRepository.class);
  CategoriaDespesaService service = new CategoriaDespesaService(repo);

  @Test
  void deveCriarCategoriaManualQuandoNomeForNovo() {
    when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    assertThat(service.criar(new CategoriaDespesaRequisicao(" Sementes fictícias ")).sistema())
        .isFalse();
  }

  @Test
  void deveRejeitarQuandoNomeDuplicado() {
    when(repo.existsByNome("Diária")).thenReturn(true);
    assertThatThrownBy(() -> service.criar(new CategoriaDespesaRequisicao("Diária")))
        .isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void deveProtegerQuandoCategoriaForDeSistema() {
    CategoriaDespesa c = new CategoriaDespesa();
    c.setSistema(true);
    when(repo.findById(1L)).thenReturn(Optional.of(c));
    assertThatThrownBy(() -> service.alterar(1L, new CategoriaDespesaRequisicao("Outro nome")))
        .isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(RegraNegocioException.class);
    verify(repo, never()).saveAndFlush(any());
    verify(repo, never()).delete(any());
  }

  @Test
  void deveExcluirQuandoCategoriaForManual() {
    CategoriaDespesa c = new CategoriaDespesa();
    when(repo.findById(2L)).thenReturn(Optional.of(c));
    service.excluir(2L);
    verify(repo).delete(c);
    verify(repo).flush();
  }

  @Test
  void deveRejeitarQuandoCategoriaInexistente() {
    assertThatThrownBy(() -> service.excluir(999L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
