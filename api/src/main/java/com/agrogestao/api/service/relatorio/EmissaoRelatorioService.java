package com.agrogestao.api.service.relatorio;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import com.agrogestao.api.service.apuracao.ApuracaoResultadoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Emissão do relatório do produtor (A03a): RN31 a RN34 e RN37.
 *
 * <p>O conteúdo é apurado uma vez, na emissão, e gravado como JSON em {@code
 * emissao_relatorio.conteudo}. Toda leitura desserializa o texto gravado e nunca reapura (RN34):
 * reemitir cria outra linha, e a anterior não muda.
 *
 * <p>Os plantios vêm de {@link ApuracaoResultadoService#apurarVarios(List, LocalDate, LocalDate)},
 * de uma vez, com o período da emissão, qualquer que seja o ciclo (RN24 e RN31); os totais são a
 * soma dos plantios. Decisões de 03/11: entram só os plantios ativos em algum dia do período, e a
 * despesa estornada fica fora da lista, como já fica fora do custo (RN19).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmissaoRelatorioService {
  static final String PERIODO_INVERTIDO = "O fim do período não pode ser anterior ao início.";
  static final String PRODUTOR_NAO_ENCONTRADO = "Produtor não encontrado.";
  static final String SEM_CONSENTIMENTO =
      "O produtor ainda não tem consentimento registrado. Registre o consentimento antes de emitir"
          + " o relatório.";
  static final String EMISSAO_NAO_ENCONTRADA = "Emissão de relatório não encontrada.";
  private static final int ESCALA_DINHEIRO = 2;

  private final EmissaoRelatorioRepository repository;
  private final ProdutorRepository produtores;
  private final PropriedadeRepository propriedades;
  private final PlantioRepository plantios;
  private final DespesaRepository despesas;
  private final DiariaRepository diarias;
  private final UsuarioRepository usuarios;
  private final ApuracaoResultadoService apuracao;
  private final AuditorAware<Long> autorDoRegistro;
  private final Clock relogio;
  private final JsonMapper jsonMapper;

  /**
   * Valida nesta ordem: período (422), produtor (404) e consentimento (422, RN37). O corpo só tem
   * produtor e período; o conteúdo, a data de emissão e o emissor são da API.
   */
  @Transactional
  public EmissaoRelatorioResposta emitir(EmissaoRelatorioRequisicao dto) {
    LocalDate inicio = dto.periodoInicio(), fim = dto.periodoFim();
    if (fim.isBefore(inicio)) throw new RegraNegocioException(PERIODO_INVERTIDO);
    Produtor produtor =
        produtores
            // A03b: trava o produtor, como as exclusões protegidas (ProtecaoEmissao).
            .buscarParaAtualizar(dto.produtorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(PRODUTOR_NAO_ENCONTRADO));
    if (produtor.getConsentimentoEm() == null) throw new RegraNegocioException(SEM_CONSENTIMENTO);

    EmissaoRelatorio emissao = new EmissaoRelatorio();
    emissao.setProdutor(produtor);
    // O mesmo id que a auditoria grava em criado_por: o do token, sem nova consulta por login.
    emissao.setUsuario(
        usuarios.getReferenceById(autorDoRegistro.getCurrentAuditor().orElseThrow()));
    emissao.setDataEmissao(Instant.now(relogio));
    emissao.setPeriodoInicio(inicio);
    emissao.setPeriodoFim(fim);
    emissao.setConteudo(jsonMapper.writeValueAsString(montar(produtor, inicio, fim)));
    // A resposta sai do texto gravado, como no GET.
    return resposta(repository.saveAndFlush(emissao));
  }

  public EmissaoRelatorioResposta detalhar(Long id) {
    return resposta(
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(EMISSAO_NAO_ENCONTRADA)));
  }

  /** Histórico do produtor, da emissão mais recente para a mais antiga (id decrescente). */
  public PaginaResposta<EmissaoRelatorioResposta> listar(
      Long produtorId, PaginacaoConsulta consulta) {
    if (!produtores.existsById(produtorId))
      throw new RecursoNaoEncontradoException(PRODUTOR_NAO_ENCONTRADO);
    Pageable pagina =
        PageRequest.of(
            consulta.getPagina(), consulta.getTamanho(), Sort.by(Sort.Direction.DESC, "id"));
    return PaginaResposta.de(repository.findByProdutorId(produtorId, pagina).map(this::resposta));
  }

  private ConteudoRelatorioResposta montar(Produtor produtor, LocalDate inicio, LocalDate fim) {
    Long produtorId = produtor.getId();
    // Decisão 2 de 03/11: ativo em algum dia do período, bordas inclusivas.
    List<Plantio> ativos =
        plantios.buscarDoProdutorComCultura(produtorId).stream()
            .filter(plantio -> plantio.ativoNoPeriodo(inicio, fim))
            .toList();
    List<ResultadoPlantioResposta> apurados =
        List.copyOf(apuracao.apurarVarios(ativos, inicio, fim).values());
    BigDecimal receita = somar(apurados, ResultadoPlantioResposta::receita);
    BigDecimal custoDesembolso = somar(apurados, ResultadoPlantioResposta::custoDesembolso);
    return new ConteudoRelatorioResposta(
        ProdutorResposta.de(produtor),
        propriedades.findAllByProdutorIdOrderById(produtorId).stream()
            .map(PropriedadeResposta::de)
            .toList(),
        apurados,
        // Decisão 3 de 03/11: a estornada sai da lista, como já sai do custo (RN19).
        despesas.buscarDoRelatorio(produtorId, inicio, fim).stream()
            .filter(despesa -> !despesa.isEstornada())
            .map(DespesaResposta::de)
            .toList(),
        diarias.buscarDoRelatorio(produtorId, inicio, fim).stream()
            .map(DiariaResposta::de)
            .toList(),
        receita,
        custoDesembolso,
        // RN23 e RN25: com sinal.
        receita.subtract(custoDesembolso),
        somar(apurados, ResultadoPlantioResposta::valorMaoObraFamiliar),
        ConteudoRelatorioResposta.RESSALVA_VALOR_DECLARADO,
        ConteudoRelatorioResposta.RESSALVA_FINALIDADE);
  }

  private static BigDecimal somar(
      List<ResultadoPlantioResposta> apurados,
      Function<ResultadoPlantioResposta, BigDecimal> campo) {
    return apurados.stream()
        .map(campo)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);
  }

  private EmissaoRelatorioResposta resposta(EmissaoRelatorio e) {
    return new EmissaoRelatorioResposta(
        e.getId(),
        e.getProdutor().getId(),
        e.getUsuario().getId(),
        e.getDataEmissao(),
        e.getPeriodoInicio(),
        e.getPeriodoFim(),
        jsonMapper.readValue(e.getConteudo(), ConteudoRelatorioResposta.class));
  }
}
