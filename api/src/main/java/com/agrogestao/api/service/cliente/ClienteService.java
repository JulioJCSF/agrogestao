package com.agrogestao.api.service.cliente;

import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.*;
import com.agrogestao.api.model.Cliente;
import com.agrogestao.api.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de clientes (RF29). Nome sem unicidade: o DER não tem restrição de nome. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {
  private final ClienteRepository clienteRepository;

  public PaginaResposta<ClienteResposta> listar(String busca, PaginacaoConsulta consulta) {
    return PaginaResposta.de(
        clienteRepository
            .findByNomeContainingIgnoreCase(busca.trim(), consulta.pageable())
            .map(ClienteService::resposta));
  }

  public ClienteResposta detalhar(Long id) {
    return resposta(encontrar(id));
  }

  @Transactional
  public ClienteResposta criar(ClienteRequisicao dto) {
    Cliente entidade = new Cliente();
    preencher(entidade, dto);
    return resposta(clienteRepository.saveAndFlush(entidade));
  }

  @Transactional
  public ClienteResposta alterar(Long id, ClienteRequisicao dto) {
    Cliente entidade = encontrar(id);
    preencher(entidade, dto);
    return resposta(clienteRepository.saveAndFlush(entidade));
  }

  private Cliente encontrar(Long id) {
    return clienteRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
  }

  private static void preencher(Cliente entidade, ClienteRequisicao dto) {
    entidade.setNome(dto.nome().trim());
    entidade.setTipoCanal(dto.tipoCanal());
  }

  private static ClienteResposta resposta(Cliente entidade) {
    return new ClienteResposta(entidade.getId(), entidade.getNome(), entidade.getTipoCanal());
  }
}
