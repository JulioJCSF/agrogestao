package com.agrogestao.api.service.relatorio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Produtor;
import com.agrogestao.api.repository.*;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** A03b: o lugar único de "período emitido" e do lock do produtor. */
class ProtecaoEmissaoTest {
  static final LocalDate FATO = LocalDate.of(2026, 3, 10);
  static final String MENSAGEM = "Mensagem de teste da proteção.";

  EmissaoRelatorioRepository emissoes = mock(EmissaoRelatorioRepository.class);
  ProdutorRepository produtores = mock(ProdutorRepository.class);
  ProtecaoEmissao protecao = new ProtecaoEmissao(emissoes, produtores);

  @Test
  void deveConsultarOPeriodoDoMesmoProdutorQuandoPerguntarSeFoiEmitido() {
    when(emissoes.existeEmissaoNoPeriodo(1L, FATO)).thenReturn(true);
    assertThat(protecao.emitido(1L, FATO)).isTrue();
    assertThat(protecao.emitido(2L, FATO)).isFalse();
    assertThat(protecao.emitido(1L, FATO.plusDays(1))).isFalse();
  }

  @Test
  void deveResponder422ComAMensagemInformadaQuandoDataEstiverEmPeriodoEmitido() {
    when(emissoes.existeEmissaoNoPeriodo(1L, FATO)).thenReturn(true);
    assertThatThrownBy(() -> protecao.exigirNaoEmitido(1L, FATO, MENSAGEM))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(MENSAGEM);
    assertThatNoException().isThrownBy(() -> protecao.exigirNaoEmitido(2L, FATO, MENSAGEM));
  }

  @Test
  void deveTravarPeloLockOuResponder404QuandoProdutorNaoExistir() {
    Produtor produtor = new Produtor();
    when(produtores.buscarParaAtualizar(1L)).thenReturn(Optional.of(produtor));
    assertThat(protecao.travarProdutor(1L)).isSameAs(produtor);
    assertThatThrownBy(() -> protecao.travarProdutor(9L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Produtor não encontrado.");
    verify(produtores, never()).findById(any());
  }
}
