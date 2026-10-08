package com.agrogestao.api.exception;

import com.agrogestao.api.dto.ErroResposta;
import com.agrogestao.api.dto.ErroResposta.CampoErro;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Tratamento centralizado de erro (padrões técnicos, seção 2.4). Todo erro sai como {@link
 * ErroResposta}; controllers não usam {@code try/catch} para devolver erro.
 *
 * <p>Estende {@link ResponseEntityExceptionHandler} para que as exceções internas do Spring MVC
 * (rota inexistente, método não suportado etc.) também saiam no formato único.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  static final String MENSAGEM_VALIDACAO =
      "Há campos com valores inválidos. Verifique os dados informados.";
  static final String MENSAGEM_CORPO_INVALIDO =
      "O corpo da requisição não está em um formato válido.";
  static final String MENSAGEM_VALOR_INVALIDO = "Valor em formato inválido.";
  static final String MENSAGEM_REQUISICAO_INVALIDA =
      "A requisição não pôde ser processada. Verifique os dados informados.";
  static final String MENSAGEM_NAO_ENCONTRADO = "Recurso não encontrado.";
  static final String MENSAGEM_CONSTRAINT_BANCO =
      "Não foi possível salvar: os dados conflitam com registros já cadastrados.";
  static final String MENSAGEM_ERRO_INTERNO =
      "Ocorreu um erro inesperado. Tente novamente e, se persistir, contate o suporte.";

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ErroResposta> handleRegraNegocio(RegraNegocioException ex) {
    return responder(CodigoErro.REGRA_NEGOCIO, ex.getMessage(), ex.getCampos());
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ErroResposta> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
    return responder(CodigoErro.NAO_ENCONTRADO, ex.getMessage(), List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErroResposta> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    // Detalhe técnico (constraint, SQL) só no log; o operador recebe mensagem genérica.
    log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
    return responder(CodigoErro.REGRA_NEGOCIO, MENSAGEM_CONSTRAINT_BANCO, List.of());
  }

  /**
   * Erros de segurança lançados dentro do controller (ex.: {@code @PreAuthorize}) voltam para o
   * {@code ExceptionTranslationFilter}, que os entrega ao entry point e ao access denied handler.
   * Sem isso, cairiam no tratamento genérico como 500.
   */
  @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
  public void relancarErroDeSeguranca(RuntimeException ex) {
    throw ex;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErroResposta> handleErroNaoTratado(Exception ex) {
    log.error("Erro não tratado", ex);
    return responder(CodigoErro.ERRO_INTERNO, MENSAGEM_ERRO_INTERNO, List.of());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<CampoErro> campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
            .toList();
    String mensagem =
        ex.getBindingResult().getGlobalErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .findFirst()
            .orElse(MENSAGEM_VALIDACAO);
    return responderObjeto(CodigoErro.VALIDACAO, mensagem, campos, headers);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<CampoErro> campos =
        ex.getParameterValidationResults().stream()
            .flatMap(
                resultado ->
                    resultado.getResolvableErrors().stream()
                        .map(
                            erro ->
                                new CampoErro(
                                    erro instanceof FieldError fieldError
                                        ? fieldError.getField()
                                        : resultado.getMethodParameter().getParameterName(),
                                    erro.getDefaultMessage())))
            .toList();
    return responderObjeto(CodigoErro.VALIDACAO, MENSAGEM_VALIDACAO, campos, headers);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (ex.getCause() instanceof MismatchedInputException mismatch
        && !mismatch.getPath().isEmpty()) {
      CampoErro campo = new CampoErro(caminhoDoCampo(mismatch), MENSAGEM_VALOR_INVALIDO);
      return responderObjeto(CodigoErro.VALIDACAO, MENSAGEM_VALIDACAO, List.of(campo), headers);
    }
    return responderObjeto(CodigoErro.VALIDACAO, MENSAGEM_CORPO_INVALIDO, List.of(), headers);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    CampoErro campo = new CampoErro(ex.getPropertyName(), MENSAGEM_VALOR_INVALIDO);
    return responderObjeto(CodigoErro.VALIDACAO, MENSAGEM_VALIDACAO, List.of(campo), headers);
  }

  /**
   * Demais exceções do Spring MVC: mantém o status do Spring e troca o corpo pelo formato único.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    CodigoErro codigo;
    String mensagem;
    if (statusCode.value() == 404) {
      codigo = CodigoErro.NAO_ENCONTRADO;
      mensagem = MENSAGEM_NAO_ENCONTRADO;
    } else if (statusCode.is5xxServerError()) {
      log.error("Erro interno no processamento da requisição", ex);
      codigo = CodigoErro.ERRO_INTERNO;
      mensagem = MENSAGEM_ERRO_INTERNO;
    } else {
      codigo = CodigoErro.VALIDACAO;
      mensagem = MENSAGEM_REQUISICAO_INVALIDA;
    }
    // Status do Spring (ex.: 405) no corpo, para bater com o status HTTP da resposta.
    ErroResposta corpo =
        new ErroResposta(Instant.now(), statusCode.value(), codigo.name(), mensagem, List.of());
    return ResponseEntity.status(statusCode).headers(headers).body(corpo);
  }

  private static String caminhoDoCampo(MismatchedInputException ex) {
    StringBuilder caminho = new StringBuilder();
    for (var referencia : ex.getPath()) {
      if (referencia.getPropertyName() != null) {
        if (!caminho.isEmpty()) {
          caminho.append('.');
        }
        caminho.append(referencia.getPropertyName());
      } else if (referencia.getIndex() >= 0) {
        caminho.append('[').append(referencia.getIndex()).append(']');
      }
    }
    return caminho.toString();
  }

  private static ResponseEntity<ErroResposta> responder(
      CodigoErro codigo, String mensagem, List<CampoErro> campos) {
    return ResponseEntity.status(codigo.getStatus())
        .body(ErroResposta.de(codigo, mensagem, campos));
  }

  private static ResponseEntity<Object> responderObjeto(
      CodigoErro codigo, String mensagem, List<CampoErro> campos, HttpHeaders headers) {
    return ResponseEntity.status(codigo.getStatus())
        .headers(headers)
        .body(ErroResposta.de(codigo, mensagem, campos));
  }
}
