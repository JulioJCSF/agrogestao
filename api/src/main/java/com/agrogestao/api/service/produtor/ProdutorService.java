package com.agrogestao.api.service.produtor;

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
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro, consulta, inativação e consentimento do produtor (RF06, RF08, RF09, RF50). */
@Service
public class ProdutorService {

  static final String MENSAGEM_NAO_ENCONTRADO = "Produtor não encontrado.";
  static final String MENSAGEM_DOCUMENTO_DUPLICADO = "Já existe produtor cadastrado com este CPF.";
  static final String MENSAGEM_COM_LANCAMENTOS =
      "Este produtor possui lançamentos e não pode ser excluído. Inative o cadastro.";
  static final String MENSAGEM_COM_VINCULOS =
      "Este produtor possui propriedades ou relatórios vinculados e não pode ser excluído."
          + " Inative o cadastro.";

  private final ProdutorRepository produtorRepository;
  private final PropriedadeRepository propriedadeRepository;
  private final ProducaoRepository producaoRepository;
  private final DespesaRepository despesaRepository;
  private final DiariaRepository diariaRepository;
  private final VendaRepository vendaRepository;
  private final EmissaoRelatorioRepository emissaoRelatorioRepository;

  public ProdutorService(
      ProdutorRepository produtorRepository,
      PropriedadeRepository propriedadeRepository,
      ProducaoRepository producaoRepository,
      DespesaRepository despesaRepository,
      DiariaRepository diariaRepository,
      VendaRepository vendaRepository,
      EmissaoRelatorioRepository emissaoRelatorioRepository) {
    this.produtorRepository = produtorRepository;
    this.propriedadeRepository = propriedadeRepository;
    this.producaoRepository = producaoRepository;
    this.despesaRepository = despesaRepository;
    this.diariaRepository = diariaRepository;
    this.vendaRepository = vendaRepository;
    this.emissaoRelatorioRepository = emissaoRelatorioRepository;
  }

  /** Busca por parte do nome ou do CPF; o CPF pode vir com máscara. */
  @Transactional(readOnly = true)
  public PaginaResposta<ProdutorResposta> listar(String busca, Pageable pageable) {
    String termo = busca == null ? "" : busca.trim();
    String digitos = termo.replaceAll("\\D", "");
    return PaginaResposta.de(
        produtorRepository.buscar(termo, digitos, pageable).map(ProdutorService::paraResposta));
  }

  @Transactional(readOnly = true)
  public ProdutorResposta detalhar(Long id) {
    return paraResposta(buscar(id));
  }

  @Transactional
  public ProdutorResposta criar(ProdutorRequisicao dto) {
    if (produtorRepository.existsByDocumento(dto.documento())) {
      throw documentoDuplicado();
    }
    Produtor produtor = new Produtor();
    copiarCampos(dto, produtor);
    return paraResposta(produtorRepository.save(produtor));
  }

  /** Também inativa ou reativa ({@code ativo}) e registra ou corrige o consentimento. */
  @Transactional
  public ProdutorResposta alterar(Long id, ProdutorRequisicao dto) {
    Produtor produtor = buscar(id);
    if (produtorRepository.existsByDocumentoAndIdNot(dto.documento(), id)) {
      throw documentoDuplicado();
    }
    copiarCampos(dto, produtor);
    return paraResposta(produtorRepository.save(produtor));
  }

  /** RF09: produtor com histórico é inativado, nunca excluído. */
  @Transactional
  public void excluir(Long id) {
    Produtor produtor = buscar(id);
    if (despesaRepository.existsByProdutorId(id)
        || vendaRepository.existsByProdutorId(id)
        || producaoRepository.existsByPlantioPropriedadeProdutorId(id)
        || diariaRepository.existsByPlantioPropriedadeProdutorId(id)) {
      throw new RegraNegocioException(MENSAGEM_COM_LANCAMENTOS);
    }
    if (propriedadeRepository.existsByProdutorId(id)
        || emissaoRelatorioRepository.existsByProdutorId(id)) {
      throw new RegraNegocioException(MENSAGEM_COM_VINCULOS);
    }
    produtorRepository.delete(produtor);
  }

  private Produtor buscar(Long id) {
    return produtorRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException(MENSAGEM_NAO_ENCONTRADO));
  }

  private static RegraNegocioException documentoDuplicado() {
    return new RegraNegocioException(
        MENSAGEM_DOCUMENTO_DUPLICADO,
        List.of(new CampoErro("documento", MENSAGEM_DOCUMENTO_DUPLICADO)));
  }

  private static void copiarCampos(ProdutorRequisicao dto, Produtor produtor) {
    produtor.setNome(dto.nome());
    produtor.setDocumento(dto.documento());
    produtor.setEndereco(dto.endereco());
    produtor.setProfissao(dto.profissao());
    produtor.setContato(dto.contato());
    produtor.setConsentimentoEm(dto.consentimentoEm());
    produtor.setAtivo(dto.ativo());
  }

  private static ProdutorResposta paraResposta(Produtor produtor) {
    return new ProdutorResposta(
        produtor.getId(),
        produtor.getNome(),
        produtor.getDocumento(),
        produtor.getEndereco(),
        produtor.getProfissao(),
        produtor.getContato(),
        produtor.getConsentimentoEm(),
        produtor.isAtivo(),
        produtor.getConsentimentoEm() == null);
  }
}
