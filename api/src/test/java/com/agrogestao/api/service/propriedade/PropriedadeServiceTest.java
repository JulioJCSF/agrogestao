package com.agrogestao.api.service.propriedade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

class PropriedadeServiceTest {
  PropriedadeRepository repo = mock(PropriedadeRepository.class);
  ProdutorRepository produtores = mock(ProdutorRepository.class);
  PropriedadeService service = new PropriedadeService(repo, produtores);

  @Test
  void deveCriarQuandoApenasNomeForInformado() {
    Produtor p = new Produtor();
    ReflectionTestUtils.setField(p, "id", 1L);
    when(produtores.findById(1L)).thenReturn(Optional.of(p));
    when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    PropriedadeResposta dto =
        service.criar(1L, new PropriedadeRequisicao("Sítio Fictício", null, null));
    assertThat(dto.produtorId()).isEqualTo(1L);
    assertThat(dto.areaTotal()).isNull();
    assertThat(dto.localizacao()).isNull();
  }

  @Test
  void deveRejeitarQuandoProdutorInexistente() {
    assertThatThrownBy(
            () -> service.criar(999L, new PropriedadeRequisicao("Sítio Fictício", null, null)))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void devePreservarProdutorQuandoAlterar() {
    Produtor p = new Produtor();
    ReflectionTestUtils.setField(p, "id", 1L);
    Propriedade pr = new Propriedade();
    pr.setProdutor(p);
    when(repo.findById(2L)).thenReturn(Optional.of(pr));
    when(repo.saveAndFlush(pr)).thenReturn(pr);
    assertThat(
            service
                .alterar(
                    2L,
                    new PropriedadeRequisicao(
                        "Novo nome", new BigDecimal("5.125"), "Local fictício"))
                .produtorId())
        .isEqualTo(1L);
  }

  @Test
  void deveListarSomentePropriedadesQuandoProdutorForInformado() {
    when(produtores.existsById(1L)).thenReturn(true);
    when(repo.findByProdutorId(eq(1L), any())).thenReturn(Page.empty());
    assertThat(service.listar(1L, new PaginacaoConsulta()).total()).isZero();
    verify(repo).findByProdutorId(eq(1L), any());
  }

  @Test
  void deveRejeitarDetalheQuandoPropriedadeInexistente() {
    assertThatThrownBy(() -> service.detalhar(999L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
