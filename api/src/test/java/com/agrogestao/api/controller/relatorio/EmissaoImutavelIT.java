package com.agrogestao.api.controller.relatorio;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.exception.ApiExceptionHandler;
import com.agrogestao.api.model.EmissaoRelatorio;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.repository.EmissaoRelatorioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * A03c: o banco recusa UPDATE, DELETE e TRUNCATE em {@code emissao_relatorio} (RN34), e o INSERT,
 * que é o que a aplicação faz, continua. Toda recusa confere que a linha ficou como estava.
 */
class EmissaoImutavelIT extends IntegracaoTest {
  static final String RN34 = "Emissão de relatório não pode ser alterada nem excluída (RN34).";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  @Autowired EmissaoRelatorioRepository repository;
  @Autowired PlatformTransactionManager transacoes;
  @Autowired EntityManager em;
  @Autowired ApiExceptionHandler handler;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, produtor;
  List<Long> produtores = new ArrayList<>();

  @BeforeEach
  void preparar() throws Exception {
    relogio.fixar(Instant.parse("2026-11-04T15:00:00Z"));
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    usuarios.criar(Perfil.ADMIN);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    produtor = produtor("Produtor Teste Imutável", "99900001907");
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    for (Long id : produtores) {
      LimpezaTeste.apagarEmissoes(jdbc, id);
      jdbc.update("delete from produtor where id = ?", id);
    }
    for (Perfil perfil : new Perfil[] {Perfil.LANCAMENTO, Perfil.ADMIN})
      jdbc.update("delete from usuario where login = ?", UsuariosTeste.login(perfil));
  }

  Long produtor(String nome, String cpf) {
    Long id =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,consentimento_em,criado_por) values (?,?,'2026-01-15',?) returning id",
            Long.class,
            nome,
            cpf,
            autor);
    produtores.add(id);
    return id;
  }

  /** Emite 2026-01-01 a 2026-06-30 pela API e devolve o id. */
  long emitir(Long doProdutor) throws Exception {
    String json =
        mvc.perform(
                post("/api/v1/relatorios")
                    .header(HttpHeaders.AUTHORIZATION, lancamento)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"produtorId\":%d,\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\"}"
                            .formatted(doProdutor)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }

  Map<String, Object> linha(long emissao) {
    return jdbc.queryForMap(
        "select produtor_id, usuario_id, data_emissao, periodo_inicio, periodo_fim, conteudo::text as conteudo, atualizado_em from emissao_relatorio where id = ?",
        emissao);
  }

  int emissoes() {
    return jdbc.queryForObject("select count(*) from emissao_relatorio", Integer.class);
  }

  void recusado(Runnable comando) {
    assertThatThrownBy(comando::run)
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining(RN34);
  }

  // --- SQL direto --------------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveRecusarUpdateSemMudarALinhaQuandoForPorSql() throws Exception {
    long id = emitir(produtor);
    Map<String, Object> antes = linha(id);

    recusado(() -> jdbc.update("update emissao_relatorio set conteudo = '{}' where id = ?", id));
    recusado(
        () ->
            jdbc.update(
                "update emissao_relatorio set periodo_fim = '2026-12-31' where id = ?", id));
    recusado(
        () -> jdbc.update("update emissao_relatorio set data_emissao = now() where id = ?", id));
    recusado(
        () -> jdbc.update("update emissao_relatorio set usuario_id = criado_por where id = ?", id));

    assertThat(linha(id)).isEqualTo(antes);
  }

  @Test
  @Tag("P1")
  void deveRecusarDeleteETruncateSemApagarNadaQuandoForPorSql() throws Exception {
    long id = emitir(produtor);
    Map<String, Object> antes = linha(id);
    int total = emissoes();

    recusado(() -> jdbc.update("delete from emissao_relatorio where id = ?", id));
    recusado(() -> jdbc.execute("truncate emissao_relatorio"));

    assertThat(linha(id)).isEqualTo(antes);
    assertThat(emissoes()).isEqualTo(total);
  }

  @Test
  void deveAceitarInsertQuandoForPorSql() {
    int total = emissoes();
    jdbc.update(
        "insert into emissao_relatorio(produtor_id,usuario_id,periodo_inicio,periodo_fim,conteudo,criado_por) values (?,?,'2026-01-01','2026-06-30','{}'::jsonb,?)",
        produtor,
        autor,
        autor);
    assertThat(emissoes()).isEqualTo(total + 1);
  }

  // --- API e Hibernate ---------------------------------------------------------------------------

  @Test
  void deveEmitirEConsultarPelaApiQuandoGatilhoEstiverLigado() throws Exception {
    long id = emitir(produtor);
    mvc.perform(get("/api/v1/relatorios/" + id).header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
    mvc.perform(
            get("/api/v1/produtores/" + produtor + "/relatorios")
                .header(HttpHeaders.AUTHORIZATION, lancamento))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[0].id").value(id));
  }

  /**
   * O {@code jsonb} devolve o texto normalizado, diferente do gravado. Se o Hibernate achasse a
   * entidade suja por isso, o flush mandaria um UPDATE e o gatilho derrubaria a transação.
   */
  @Test
  @Tag("P1")
  void deveNaoEmitirUpdateQuandoCarregarEmissaoNumaTransacaoDeEscrita() throws Exception {
    long id = emitir(produtor);
    Map<String, Object> antes = linha(id);
    new TransactionTemplate(transacoes)
        .executeWithoutResult(
            status -> {
              EmissaoRelatorio emissao = repository.findById(id).orElseThrow();
              assertThat(emissao.getConteudo()).isNotBlank();
              em.flush();
            });
    assertThat(linha(id)).isEqualTo(antes);
  }

  @Test
  void deveManterOProdutorComEmissaoQuandoTentarExcluirPelaApi() throws Exception {
    emitir(produtor);
    mvc.perform(
            delete("/api/v1/produtores/" + produtor)
                .header(HttpHeaders.AUTHORIZATION, usuarios.bearer(Perfil.ADMIN)))
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "Este produtor possui propriedades ou relatórios vinculados e não pode ser"
                        + " excluído. Inative o cadastro."));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from produtor where id = ?", Integer.class, produtor))
        .isOne();
  }

  // --- erro na API -------------------------------------------------------------------------------

  /** Rota só deste teste: altera a emissão pelo repositório, como faria um bug na aplicação. */
  @RestController
  static class AlteracaoIndevidaController {
    final EmissaoRelatorioRepository repository;
    final TransactionTemplate transacao;

    AlteracaoIndevidaController(
        EmissaoRelatorioRepository repository, PlatformTransactionManager transacoes) {
      this.repository = repository;
      this.transacao = new TransactionTemplate(transacoes);
    }

    @PutMapping("/teste/emissoes/{id}")
    void alterar(@PathVariable Long id) {
      transacao.executeWithoutResult(
          status -> {
            EmissaoRelatorio emissao = repository.findById(id).orElseThrow();
            emissao.setPeriodoFim(LocalDate.of(2026, 12, 31));
            repository.saveAndFlush(emissao);
          });
    }
  }

  @Test
  @Tag("P1")
  void deveResponder422SemMudarAEmissaoQuandoAplicacaoTentarAlterar() throws Exception {
    long id = emitir(produtor);
    Map<String, Object> antes = linha(id);
    AlteracaoIndevidaController rota = new AlteracaoIndevidaController(repository, transacoes);

    assertThatThrownBy(() -> rota.alterar(id)).isInstanceOf(DataIntegrityViolationException.class);

    MockMvc comRota = MockMvcBuilders.standaloneSetup(rota).setControllerAdvice(handler).build();
    comRota
        .perform(put("/teste/emissoes/" + id))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(
            jsonPath("$.mensagem")
                .value(
                    "Não foi possível salvar: os dados conflitam com registros já cadastrados."));
    assertThat(linha(id)).isEqualTo(antes);
  }

  // --- limpeza dos testes ------------------------------------------------------------------------

  @Test
  @Tag("P1")
  void deveReligarOsGatilhosQuandoLimpezaDeTesteTerminar() throws Exception {
    Long outro = produtor("Produtor Teste Imutável Outro", "99900001988");
    emitir(produtor);
    long doOutro = emitir(outro);

    LimpezaTeste.apagarEmissoes(jdbc, produtor);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from emissao_relatorio where produtor_id = ?",
                Integer.class,
                produtor))
        .isZero();

    // Várias vezes, para pegar outras conexões do pool.
    for (int vez = 0; vez < 5; vez++) {
      assertThat(jdbc.queryForObject("show session_replication_role", String.class))
          .isEqualTo("origin");
      assertThatThrownBy(() -> jdbc.update("delete from emissao_relatorio where id = ?", doOutro))
          .isInstanceOf(DataAccessException.class)
          .hasMessageContaining(RN34);
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from emissao_relatorio where id = ?", Integer.class, doOutro))
        .isOne();
  }
}
