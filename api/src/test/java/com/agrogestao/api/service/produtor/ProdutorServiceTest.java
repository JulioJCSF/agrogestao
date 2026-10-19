package com.agrogestao.api.service.produtor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agrogestao.api.dto.ErroResposta.CampoErro;
import com.agrogestao.api.dto.PaginaResposta;
import com.agrogestao.api.dto.ProdutorRequisicao;
import com.agrogestao.api.dto.ProdutorResposta;
import com.agrogestao.api.exception.RecursoNaoEncontradoException;
import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.model.Produtor;
import com.agrogestao.api.repository.DespesaRepository;
import com.agrogestao.api.repository.DiariaRepository;
import com.agrogestao.api.repository.EmissaoRelatorioRepository;
import com.agrogestao.api.repository.ProducaoRepository;
import com.agrogestao.api.repository.ProdutorRepository;
import com.agrogestao.api.repository.PropriedadeRepository;
import com.agrogestao.api.repository.VendaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class ProdutorServiceTest {

  private static final String CPF = "99900001079";
  private static final LocalDate CONSENTIMENTO = LocalDate.of(2026, 1, 10);

  private final ProdutorRepository produtorRepository = mock(ProdutorRepository.class);
  private final PropriedadeRepository propriedadeRepository = mock(PropriedadeRepository.class);
  private final ProducaoRepository producaoRepository = mock(ProducaoRepository.class);
  private final DespesaRepository despesaRepository = mock(DespesaRepository.class);
  private final DiariaRepository diariaRepository = mock(DiariaRepository.class);
  private final VendaRepository vendaRepository = mock(VendaRepository.class);
  private final EmissaoRelatorioRepository emissaoRelatorioRepository =
      mock(EmissaoRelatorioRepository.class);

  private final ProdutorService service =
      new ProdutorService(
          produtorRepository,
          propriedadeRepository,
          producaoRepository,
          despesaRepository,
          diariaRepository,
          vendaRepository,
          emissaoRelatorioRepository);

  private static ProdutorRequisicao requisicao(LocalDate consentimentoEm, boolean ativo) {
    return new ProdutorRequisicao(
        "Produtor Fictício", CPF, "Endereço Fictício", "Agricultor", null, consentimentoEm, ativo);
  }

  private static Produtor produtor(Long id) {
    Produtor produtor = new Produtor();
    ReflectionTestUtils.setField(produtor, "id", id);
    produtor.setNome("Produtor Fictício");
    produtor.setDocumento(CPF);
    return produtor;
  }

  private void salvarDevolvendoComId() {
    when(produtorRepository.save(any(Produtor.class)))
        .thenAnswer(
            chamada -> {
              Produtor salvo = chamada.getArgument(0);
              if (salvo.getId() == null) {
                ReflectionTestUtils.setField(salvo, "id", 5L);
              }
              return salvo;
            });
  }

  @Test
  void deveCriarComConsentimentoPendenteQuandoDataNaoInformada() {
    salvarDevolvendoComId();

    ProdutorResposta resposta = service.criar(requisicao(null, true));

    assertThat(resposta.id()).isEqualTo(5L);
    assertThat(resposta.documento()).isEqualTo(CPF);
    assertThat(resposta.ativo()).isTrue();
    assertThat(resposta.consentimentoEm()).isNull();
    assertThat(resposta.consentimentoPendente()).isTrue();
  }

  @Test
  void deveCriarSemPendenciaQuandoConsentimentoInformado() {
    salvarDevolvendoComId();

    ProdutorResposta resposta = service.criar(requisicao(CONSENTIMENTO, true));

    assertThat(resposta.consentimentoEm()).isEqualTo(CONSENTIMENTO);
    assertThat(resposta.consentimentoPendente()).isFalse();
  }

  @Test
  void deveRecusarCriacaoQuandoCpfJaCadastrado() {
    when(produtorRepository.existsByDocumento(CPF)).thenReturn(true);

    assertThatThrownBy(() -> service.criar(requisicao(null, true)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(ProdutorService.MENSAGEM_DOCUMENTO_DUPLICADO)
        .extracting(ex -> ((RegraNegocioException) ex).getCampos())
        .isEqualTo(
            List.of(new CampoErro("documento", ProdutorService.MENSAGEM_DOCUMENTO_DUPLICADO)));
    verify(produtorRepository, never()).save(any());
  }

  @Test
  void deveRecusarAlteracaoQuandoCpfPertencerAOutroProdutor() {
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(produtor(1L)));
    when(produtorRepository.existsByDocumentoAndIdNot(CPF, 1L)).thenReturn(true);

    assertThatThrownBy(() -> service.alterar(1L, requisicao(null, true)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(ProdutorService.MENSAGEM_DOCUMENTO_DUPLICADO);
    verify(produtorRepository, never()).save(any());
  }

  @Test
  void deveInativarERegistrarConsentimentoQuandoAlterarMantendoProprioCpf() {
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(produtor(1L)));
    salvarDevolvendoComId();

    ProdutorResposta resposta = service.alterar(1L, requisicao(CONSENTIMENTO, false));

    assertThat(resposta.id()).isEqualTo(1L);
    assertThat(resposta.ativo()).isFalse();
    assertThat(resposta.consentimentoPendente()).isFalse();
  }

  @Test
  void deveVoltarAPendenciaQuandoConsentimentoForRemovido() {
    Produtor existente = produtor(1L);
    existente.setConsentimentoEm(CONSENTIMENTO);
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(existente));
    salvarDevolvendoComId();

    ProdutorResposta resposta = service.alterar(1L, requisicao(null, true));

    assertThat(resposta.consentimentoPendente()).isTrue();
  }

  @Test
  void deveLancarNaoEncontradoQuandoProdutorInexistente() {
    when(produtorRepository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detalhar(9L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage(ProdutorService.MENSAGEM_NAO_ENCONTRADO);
    assertThatThrownBy(() -> service.alterar(9L, requisicao(null, true)))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.excluir(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveBuscarPorNomeEDigitosQuandoCpfVierComMascara() {
    Pageable pagina = PageRequest.of(0, 20);
    when(produtorRepository.buscar("999.000.010", "999000010", pagina))
        .thenReturn(new PageImpl<>(List.of(produtor(1L)), pagina, 1));

    PaginaResposta<ProdutorResposta> resposta = service.listar(" 999.000.010 ", pagina);

    assertThat(resposta.total()).isEqualTo(1);
    assertThat(resposta.itens()).extracting(ProdutorResposta::id).containsExactly(1L);
  }

  @Test
  void deveExcluirQuandoProdutorNaoTiverVinculos() {
    Produtor existente = produtor(1L);
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(existente));

    service.excluir(1L);

    verify(produtorRepository).delete(existente);
  }

  static Stream<Arguments> lancamentos() {
    return Stream.of(
        Arguments.of("despesa", (Consumer<ProdutorServiceTest>) t -> t.despesaExiste()),
        Arguments.of("venda", (Consumer<ProdutorServiceTest>) t -> t.vendaExiste()),
        Arguments.of("producao", (Consumer<ProdutorServiceTest>) t -> t.producaoExiste()),
        Arguments.of("diaria", (Consumer<ProdutorServiceTest>) t -> t.diariaExiste()));
  }

  private void despesaExiste() {
    when(despesaRepository.existsByProdutorId(1L)).thenReturn(true);
  }

  private void vendaExiste() {
    when(vendaRepository.existsByProdutorId(1L)).thenReturn(true);
  }

  private void producaoExiste() {
    when(producaoRepository.existsByPlantioPropriedadeProdutorId(1L)).thenReturn(true);
  }

  private void diariaExiste() {
    when(diariaRepository.existsByPlantioPropriedadeProdutorId(1L)).thenReturn(true);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("lancamentos")
  void deveRecusarExclusaoQuandoProdutorTiverLancamento(
      String tipo, Consumer<ProdutorServiceTest> cenario) {
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(produtor(1L)));
    cenario.accept(this);

    assertThatThrownBy(() -> service.excluir(1L))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(ProdutorService.MENSAGEM_COM_LANCAMENTOS);
    verify(produtorRepository, never()).delete(any());
  }

  @Test
  void deveRecusarExclusaoQuandoProdutorTiverPropriedade() {
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(produtor(1L)));
    when(propriedadeRepository.existsByProdutorId(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(1L))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(ProdutorService.MENSAGEM_COM_VINCULOS);
    verify(produtorRepository, never()).delete(any());
  }

  @Test
  void deveRecusarExclusaoQuandoProdutorTiverRelatorioEmitido() {
    when(produtorRepository.findById(1L)).thenReturn(Optional.of(produtor(1L)));
    when(emissaoRelatorioRepository.existsByProdutorId(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(1L))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(ProdutorService.MENSAGEM_COM_VINCULOS);
  }
}
