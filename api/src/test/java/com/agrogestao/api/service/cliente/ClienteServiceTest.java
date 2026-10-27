package com.agrogestao.api.service.cliente;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

class ClienteServiceTest {
  ClienteRepository repo = mock(ClienteRepository.class);
  ClienteService service = new ClienteService(repo);

  @BeforeEach
  void preparar() {
    when(repo.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              Cliente c = inv.getArgument(0);
              if (c.getId() == null) ReflectionTestUtils.setField(c, "id", 1L);
              return c;
            });
  }

  Cliente existente() {
    Cliente cliente = new Cliente();
    ReflectionTestUtils.setField(cliente, "id", 7L);
    cliente.setNome("Cliente Teste Feira");
    cliente.setTipoCanal(TipoCanal.FEIRA);
    when(repo.findById(7L)).thenReturn(Optional.of(cliente));
    return cliente;
  }

  @Test
  void deveGravarNomeSemEspacosQuandoCriar() {
    var resposta = service.criar(new ClienteRequisicao("  Cliente Teste Feira  ", TipoCanal.FEIRA));
    ArgumentCaptor<Cliente> gravado = ArgumentCaptor.forClass(Cliente.class);
    verify(repo).saveAndFlush(gravado.capture());
    assertThat(gravado.getValue().getNome()).isEqualTo("Cliente Teste Feira");
    assertThat(gravado.getValue().getTipoCanal()).isEqualTo(TipoCanal.FEIRA);
    assertThat(resposta).isEqualTo(new ClienteResposta(1L, "Cliente Teste Feira", TipoCanal.FEIRA));
  }

  @Test
  void deveTrocarNomeECanalQuandoAlterarExistente() {
    Cliente cliente = existente();
    var resposta =
        service.alterar(7L, new ClienteRequisicao(" Cliente Teste Merenda ", TipoCanal.MERENDA));
    assertThat(resposta)
        .isEqualTo(new ClienteResposta(7L, "Cliente Teste Merenda", TipoCanal.MERENDA));
    verify(repo).saveAndFlush(cliente);
  }

  @Test
  void deveRejeitarSemGravarQuandoClienteNaoExistir() {
    assertThatThrownBy(() -> service.detalhar(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Cliente não encontrado.");
    assertThatThrownBy(
            () -> service.alterar(99L, new ClienteRequisicao("Cliente Teste", TipoCanal.OUTRO)))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Cliente não encontrado.");
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void deveAceitarSemConsultarNomeQuandoNomeSeRepetir() {
    var dto = new ClienteRequisicao("Cliente Teste Repetido", TipoCanal.CEASA);
    service.criar(dto);
    service.criar(dto);
    verify(repo, times(2)).saveAndFlush(any());
    verify(repo, never()).findByNomeContainingIgnoreCase(any(), any());
    verifyNoMoreInteractions(repo);
  }

  @Test
  void deveBuscarSemEspacosQuandoBuscaTiverEspacos() {
    var consulta = new PaginacaoConsulta();
    Cliente cliente = existente();
    when(repo.findByNomeContainingIgnoreCase("feira", consulta.pageable()))
        .thenReturn(new PageImpl<>(java.util.List.of(cliente)));
    var pagina = service.listar("  feira ", consulta);
    assertThat(pagina.itens())
        .containsExactly(new ClienteResposta(7L, "Cliente Teste Feira", TipoCanal.FEIRA));
    verify(repo).findByNomeContainingIgnoreCase("feira", consulta.pageable());
  }
}
