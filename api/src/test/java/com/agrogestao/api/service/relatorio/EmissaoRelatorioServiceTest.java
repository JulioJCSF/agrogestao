package com.agrogestao.api.service.relatorio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.apuracao.ApuracaoResultadoService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * A03a com repositórios e apuração mockados: o recorte de plantios e despesas, o período passado à
 * apuração, os totais e as ressalvas. Massa sintética, CPF da Q00f.
 */
class EmissaoRelatorioServiceTest {
  static final LocalDate INICIO = LocalDate.parse("2026-01-01");
  static final LocalDate FIM = LocalDate.parse("2026-06-30");
  static final Instant AGORA = Instant.parse("2026-07-01T15:00:00Z");
  static final Long OPERADOR = 7L;

  EmissaoRelatorioRepository repository = mock(EmissaoRelatorioRepository.class);
  ProdutorRepository produtores = mock(ProdutorRepository.class);
  PropriedadeRepository propriedades = mock(PropriedadeRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  DespesaRepository despesas = mock(DespesaRepository.class);
  DiariaRepository diarias = mock(DiariaRepository.class);
  UsuarioRepository usuarios = mock(UsuarioRepository.class);
  ApuracaoResultadoService apuracao = mock(ApuracaoResultadoService.class);
  JsonMapper json = JsonMapper.builder().build();
  EmissaoRelatorioService service =
      new EmissaoRelatorioService(
          repository,
          produtores,
          propriedades,
          plantios,
          despesas,
          diarias,
          usuarios,
          apuracao,
          () -> Optional.of(OPERADOR),
          Clock.fixed(AGORA, ZoneId.of("America/Fortaleza")),
          json);

  Produtor produtor;
  Propriedade propriedade;
  Cultura milho;
  List<Plantio> doProdutor = new ArrayList<>();
  long proximoId = 100;

  @BeforeEach
  void preparar() {
    produtor = new Produtor();
    ReflectionTestUtils.setField(produtor, "id", 1L);
    produtor.setNome("Produtor Teste Relatório");
    produtor.setDocumento("99900001311");
    produtor.setConsentimentoEm(LocalDate.parse("2026-01-15"));
    propriedade = new Propriedade();
    ReflectionTestUtils.setField(propriedade, "id", 2L);
    propriedade.setProdutor(produtor);
    propriedade.setNome("Sítio Teste");
    propriedade.setAreaTotal(new BigDecimal("10.000"));
    milho = new Cultura();
    milho.setNome("Milho");
    milho.setUnidadeMedida("kg");

    when(produtores.buscarParaAtualizar(1L)).thenReturn(Optional.of(produtor));
    when(propriedades.findAllByProdutorIdOrderById(1L)).thenReturn(List.of(propriedade));
    when(plantios.buscarDoProdutorComCultura(1L)).thenReturn(doProdutor);
    when(despesas.buscarDoRelatorio(1L, INICIO, FIM)).thenReturn(List.of());
    when(diarias.buscarDoRelatorio(1L, INICIO, FIM)).thenReturn(List.of());
    Usuario operador =
        new Usuario("Operador Fictício", "operador.ficticio", "hash", Perfil.LANCAMENTO, true);
    ReflectionTestUtils.setField(operador, "id", OPERADOR);
    when(usuarios.getReferenceById(OPERADOR)).thenReturn(operador);
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              ReflectionTestUtils.setField(inv.<EmissaoRelatorio>getArgument(0), "id", 50L);
              return inv.getArgument(0);
            });
    when(apuracao.apurar(any(Plantio.class), any(), any()))
        .thenAnswer(inv -> apurado(inv.getArgument(0), "0.00", "0.00", "0.00"));
  }

  Plantio plantio(TipoCiclo ciclo, String inicio, String encerramento) {
    Plantio plantio = new Plantio();
    ReflectionTestUtils.setField(plantio, "id", proximoId++);
    plantio.setPropriedade(propriedade);
    plantio.setCultura(milho);
    plantio.setTipoCiclo(ciclo);
    plantio.setArea(new BigDecimal("1.000"));
    plantio.setDataInicio(LocalDate.parse(inicio));
    plantio.setDataEncerramento(encerramento == null ? null : LocalDate.parse(encerramento));
    doProdutor.add(plantio);
    return plantio;
  }

  static ResultadoPlantioResposta apurado(
      Plantio plantio, String receita, String custo, String familiar) {
    BigDecimal r = new BigDecimal(receita), c = new BigDecimal(custo), f = new BigDecimal(familiar);
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
        r,
        c,
        f,
        c.add(f),
        r.subtract(c));
  }

  Despesa despesa(String valor, boolean estornada) {
    Despesa despesa = new Despesa();
    ReflectionTestUtils.setField(despesa, "id", proximoId++);
    despesa.setProdutor(produtor);
    CategoriaDespesa categoria = new CategoriaDespesa();
    ReflectionTestUtils.setField(categoria, "id", 3L);
    despesa.setCategoriaDespesa(categoria);
    despesa.setDescricao("Adubo Teste Relatório");
    despesa.setValorTotal(new BigDecimal(valor));
    despesa.setDataFato(LocalDate.parse("2026-03-10"));
    despesa.setEstornada(estornada);
    return despesa;
  }

  EmissaoRelatorioResposta emitir() {
    return service.emitir(new EmissaoRelatorioRequisicao(1L, INICIO, FIM));
  }

  @Test
  void deveIncluirSoPlantiosAtivosNoPeriodoQuandoEmitir() {
    Plantio aberto = plantio(TipoCiclo.LONGO, "2026-02-01", null);
    Plantio encerradoNoInicio = plantio(TipoCiclo.LONGO, "2025-08-01", "2026-01-01");
    Plantio comecaNoFim = plantio(TipoCiclo.LONGO, "2026-06-30", null);
    plantio(TipoCiclo.LONGO, "2026-07-01", null);
    plantio(TipoCiclo.LONGO, "2025-08-01", "2025-12-31");

    assertThat(emitir().conteudo().plantios())
        .extracting(ResultadoPlantioResposta::plantioId)
        .containsExactly(aberto.getId(), encerradoNoInicio.getId(), comecaNoFim.getId());
    verify(apuracao, times(3)).apurar(any(Plantio.class), any(), any());
  }

  @Test
  void deveApurarPeloPeriodoDaEmissaoQuandoPlantioForCurto() {
    Plantio curto = plantio(TipoCiclo.CURTO, "2026-02-01", null);
    emitir();
    verify(apuracao).apurar(curto, INICIO, FIM);
    verify(apuracao, never()).apurar(anyLong(), any());
  }

  @Test
  void deveSomarOsPlantiosQuandoMontarOsTotais() {
    Plantio milhoCt050 = plantio(TipoCiclo.LONGO, "2026-02-01", null);
    Plantio hortalica = plantio(TipoCiclo.CURTO, "2026-02-01", null);
    when(apuracao.apurar(milhoCt050, INICIO, FIM))
        .thenReturn(apurado(milhoCt050, "900.00", "696.67", "80.00"));
    when(apuracao.apurar(hortalica, INICIO, FIM))
        .thenReturn(apurado(hortalica, "90.00", "33.33", "0.00"));

    ConteudoRelatorioResposta conteudo = emitir().conteudo();
    assertThat(conteudo.plantios())
        .extracting(ResultadoPlantioResposta::resultado)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("203.33"), new BigDecimal("56.67"));
    assertThat(conteudo.receita()).isEqualByComparingTo("990.00");
    assertThat(conteudo.custoDesembolso()).isEqualByComparingTo("730.00");
    assertThat(conteudo.valorMaoObraFamiliar()).isEqualByComparingTo("80.00");
    assertThat(conteudo.resultado()).isEqualByComparingTo("260.00");
  }

  @Test
  void deveDeixarEstornadaForaDaListaQuandoEstiverNoPeriodo() {
    Despesa valida = despesa("100.00", false);
    when(despesas.buscarDoRelatorio(1L, INICIO, FIM))
        .thenReturn(List.of(valida, despesa("300.00", true)));
    assertThat(emitir().conteudo().despesas())
        .extracting(DespesaResposta::id)
        .containsExactly(valida.getId());
  }

  @Test
  void deveRecusarSemGravarQuandoProdutorNaoTiverConsentimento() {
    produtor.setConsentimentoEm(null);
    assertThatThrownBy(this::emitir)
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage(
            "O produtor ainda não tem consentimento registrado. Registre o consentimento antes de"
                + " emitir o relatório.");
    verify(repository, never()).saveAndFlush(any());
    verify(repository, never()).save(any());
    verifyNoInteractions(apuracao);
  }

  @Test
  void deveRecusarPeriodoInvertidoAntesDeProcurarOProdutor() {
    assertThatThrownBy(() -> service.emitir(new EmissaoRelatorioRequisicao(99L, FIM, INICIO)))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessage("O fim do período não pode ser anterior ao início.");
    verifyNoInteractions(produtores);
  }

  @Test
  void deveTrazerAsRessalvasExatasQuandoTudoEstiverZerado() {
    EmissaoRelatorioResposta emissao = emitir();
    ConteudoRelatorioResposta conteudo = emissao.conteudo();
    assertThat(conteudo.ressalvaValorDeclarado())
        .isEqualTo("Valores declarados pelo produtor, não comprovados por documento fiscal.");
    assertThat(conteudo.ressalvaFinalidade())
        .isEqualTo(
            "Este documento não constitui garantia de aceitação para qualquer finalidade externa.");
    assertThat(conteudo.plantios()).isEmpty();
    assertThat(conteudo.despesas()).isEmpty();
    assertThat(conteudo.diarias()).isEmpty();
    for (BigDecimal total :
        List.of(
            conteudo.receita(),
            conteudo.custoDesembolso(),
            conteudo.resultado(),
            conteudo.valorMaoObraFamiliar()))
      assertThat(total).isEqualByComparingTo("0").hasScaleOf(2);
    assertThat(emissao.usuarioId()).isEqualTo(OPERADOR);
    assertThat(emissao.dataEmissao()).isEqualTo(AGORA);
    assertThat(emissao.conteudo().produtor().documento()).isEqualTo("99900001311");
  }

  @Test
  void deveGravarOConteudoComoJsonQuandoEmitir() {
    emitir();
    verify(repository)
        .saveAndFlush(
            argThat(
                e ->
                    e.getConteudo().contains("\"ressalvaFinalidade\":")
                        && e.getPeriodoInicio().equals(INICIO)
                        && e.getPeriodoFim().equals(FIM)
                        && e.getDataEmissao().equals(AGORA)));
  }

  @Test
  void deveManterOExemploDoSwaggerIgualAsConstantesQuandoDocumentarAsRessalvas() {
    for (Class<?> dto : List.of(ConteudoRelatorioResposta.class, EmissaoRelatorioResposta.class)) {
      String exemplo = dto.getAnnotation(Schema.class).example();
      ConteudoRelatorioResposta conteudo =
          dto == ConteudoRelatorioResposta.class
              ? json.readValue(exemplo, ConteudoRelatorioResposta.class)
              : json.readValue(exemplo, EmissaoRelatorioResposta.class).conteudo();
      assertThat(conteudo.ressalvaValorDeclarado())
          .as(dto.getSimpleName())
          .isEqualTo(ConteudoRelatorioResposta.RESSALVA_VALOR_DECLARADO);
      assertThat(conteudo.ressalvaFinalidade())
          .as(dto.getSimpleName())
          .isEqualTo(ConteudoRelatorioResposta.RESSALVA_FINALIDADE);
    }
  }
}
