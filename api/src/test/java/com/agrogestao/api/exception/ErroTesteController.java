package com.agrogestao.api.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller só de teste: cada endpoint provoca um tipo de erro tratado pelo advice. */
@RestController
@RequestMapping("/teste-erro")
class ErroTesteController {

  record DespesaTesteDTO(@NotBlank String descricao, @NotNull LocalDate dataDespesa) {}

  @GetMapping("/regra-negocio")
  void regraNegocio() {
    throw new RegraNegocioException("Não é possível lançar despesa em plantio encerrado.");
  }

  @GetMapping("/nao-encontrado")
  void naoEncontrado() {
    throw new RecursoNaoEncontradoException("Plantio não encontrado.");
  }

  @PostMapping("/validacao")
  ResponseEntity<Void> validacao(@Valid @RequestBody DespesaTesteDTO dto) {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/plantios/{id}")
  ResponseEntity<Void> porId(@PathVariable Long id) {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/constraint-banco")
  void constraintBanco() {
    throw new DataIntegrityViolationException(
        "ERROR: duplicate key value violates unique constraint \"uk_plantio_codigo\"");
  }

  @GetMapping("/erro-interno")
  void erroInterno() {
    throw new IllegalStateException("falha interna com.agrogestao.segredo");
  }

  @GetMapping("/acesso-negado")
  void acessoNegado() {
    throw new AccessDeniedException("sem permissão");
  }
}
