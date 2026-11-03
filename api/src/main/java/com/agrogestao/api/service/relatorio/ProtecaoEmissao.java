package com.agrogestao.api.service.relatorio;

import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Produtor;
import com.agrogestao.api.repository.*;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * O que já entrou num relatório emitido (RN11, RN34), num lugar só para despesa, venda e diária.
 * "Período emitido" é haver emissão do mesmo produtor com {@code periodoInicio ≤ dataFato ≤
 * periodoFim}.
 *
 * <p>Ordem de locks (A03b): o produtor primeiro, depois os plantios em ordem crescente. A emissão
 * também trava o produtor antes de montar o conteúdo; assim, uma exclusão conferida aqui e uma
 * emissão do mesmo produtor nunca se cruzam: ou a exclusão termina antes e a emissão não vê o
 * lançamento, ou a emissão termina antes e a exclusão encontra o período emitido.
 */
@Component
@RequiredArgsConstructor
public class ProtecaoEmissao {
  private final EmissaoRelatorioRepository emissoes;
  private final ProdutorRepository produtores;

  /** Trava a linha do produtor até o fim da transação; inexistente → 404. */
  public Produtor travarProdutor(Long produtorId) {
    return produtores
        .buscarParaAtualizar(produtorId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
  }

  public boolean emitido(Long produtorId, LocalDate dataFato) {
    return emissoes.existeEmissaoNoPeriodo(produtorId, dataFato);
  }

  /** 422 com a mensagem informada se a data do fato estiver em período já emitido. */
  public void exigirNaoEmitido(Long produtorId, LocalDate dataFato, String mensagem) {
    if (emitido(produtorId, dataFato)) throw new RegraNegocioException(mensagem);
  }
}
