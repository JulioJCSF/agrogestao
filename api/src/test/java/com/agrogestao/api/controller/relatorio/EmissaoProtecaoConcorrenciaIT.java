package com.agrogestao.api.controller.relatorio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.repository.*;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A03b: exclusão de despesa × emissão do mesmo produtor, em paralelo. Sem o lock do produtor, as
 * duas transações se encontram no pior ponto: a exclusão já conferiu que o período não foi emitido
 * e a emissão já leu a despesa. O teste segura cada uma nesse ponto por até 2 s, esperando a outra;
 * com o lock, a segunda nem chega lá antes de a primeira terminar.
 *
 * <p>Aceitável: (a) a exclusão ganhou e a emissão não contém a despesa, ou (b) a emissão ganhou e a
 * exclusão tomou 422. Nunca a emissão com a despesa e a despesa excluída. Um produtor novo por
 * rodada, para nenhuma emissão anterior proteger a despesa da rodada seguinte.
 */
class EmissaoProtecaoConcorrenciaIT extends IntegracaoTest {
  static final int RODADAS = 3;

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  @Autowired EntityManager em;
  @MockitoSpyBean EmissaoRelatorioRepository emissoes;
  @MockitoSpyBean DespesaRepository despesas;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, milho, adubo;
  List<Long> produtores = new ArrayList<>();
  AtomicReference<CyclicBarrier> pontoCritico = new AtomicReference<>();

  @BeforeEach
  void preparar() throws Exception {
    relogio.fixar(Instant.parse("2026-11-03T15:00:00Z"));
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);

    // Os spies são proxies de interface: a leitura real vem de outra instância do repositório.
    EmissaoRelatorioRepository emissoesReais =
        new JpaRepositoryFactory(em).getRepository(EmissaoRelatorioRepository.class);
    DespesaRepository despesasReais =
        new JpaRepositoryFactory(em).getRepository(DespesaRepository.class);
    doAnswer(
            inv -> {
              boolean emitido =
                  emissoesReais.existeEmissaoNoPeriodo(inv.getArgument(0), inv.getArgument(1));
              esperarAOutra();
              return emitido;
            })
        .when(emissoes)
        .existeEmissaoNoPeriodo(anyLong(), any());
    doAnswer(
            inv -> {
              var lidas =
                  despesasReais.buscarDoRelatorio(
                      inv.getArgument(0), inv.<LocalDate>getArgument(1), inv.getArgument(2));
              esperarAOutra();
              return lidas;
            })
        .when(despesas)
        .buscarDoRelatorio(anyLong(), any(), any());
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    for (Long id : produtores) {
      jdbc.update("delete from emissao_relatorio where produtor_id = ?", id);
      jdbc.update("delete from despesa where produtor_id = ?", id);
      jdbc.update(
          "delete from plantio where propriedade_id in (select id from propriedade where produtor_id = ?)",
          id);
      jdbc.update("delete from propriedade where produtor_id = ?", id);
      jdbc.update("delete from produtor where id = ?", id);
    }
    jdbc.update("delete from usuario where login = ?", UsuariosTeste.LANCAMENTO);
  }

  /** Espera a outra transação no ponto crítico; sem ela em 2 s, segue sozinha. */
  void esperarAOutra() throws InterruptedException {
    CyclicBarrier barreira = pontoCritico.get();
    if (barreira == null) return;
    try {
      barreira.await(2, TimeUnit.SECONDS);
    } catch (TimeoutException | BrokenBarrierException sozinha) {
      // Com o lock do produtor, a outra está esperando a linha travada: é o esperado.
    }
  }

  MockHttpServletResponse executar(MockHttpServletRequestBuilder requisicao, String corpo)
      throws Exception {
    return mvc.perform(
            requisicao
                .header(HttpHeaders.AUTHORIZATION, lancamento)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
        .andReturn()
        .getResponse();
  }

  /** Produtor com consentimento, plantio aberto e uma despesa de 2026-03-10; devolve a despesa. */
  long massa(int rodada) throws Exception {
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,consentimento_em,criado_por) values (?,?,'2026-01-15',?) returning id",
            Long.class,
            "Produtor Teste Concorrência " + rodada,
            "9990000170" + rodada,
            autor);
    produtores.add(produtor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            autor);
    Long plantio =
        jdbc.queryForObject(
            "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1.000,'LONGO','2026-02-01',?) returning id",
            Long.class,
            propriedade,
            milho,
            autor);
    MockHttpServletResponse criada =
        executar(
            post("/api/v1/despesas"),
            "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Concorrência\",\"valorTotal\":80.00,\"dataFato\":\"2026-03-10\",\"plantioIds\":[%d]}"
                .formatted(produtor, adubo, plantio));
    assertThat(criada.getStatus()).isEqualTo(201);
    return ((Number) JsonPath.read(criada.getContentAsString(), "$.id")).longValue();
  }

  @Test
  @Tag("P1")
  void deveNuncaEmitirDespesaExcluidaQuandoExclusaoEEmissaoCorreremEmParalelo() throws Exception {
    Map<String, Integer> desfechos = new TreeMap<>();
    for (int rodada = 0; rodada < RODADAS; rodada++) {
      long despesa = massa(rodada);
      Long produtor = produtores.get(rodada);
      pontoCritico.set(new CyclicBarrier(2));
      CountDownLatch largada = new CountDownLatch(1);
      MockHttpServletResponse exclusao, emissao;
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        Future<MockHttpServletResponse> excluir =
            executor.submit(
                () -> {
                  largada.await();
                  return mvc.perform(
                          delete("/api/v1/despesas/" + despesa)
                              .header(HttpHeaders.AUTHORIZATION, lancamento))
                      .andReturn()
                      .getResponse();
                });
        Future<MockHttpServletResponse> emitir =
            executor.submit(
                () -> {
                  largada.await();
                  return executar(
                      post("/api/v1/relatorios"),
                      "{\"produtorId\":%d,\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-06-30\"}"
                          .formatted(produtor));
                });
        largada.countDown();
        exclusao = excluir.get(30, TimeUnit.SECONDS);
        emissao = emitir.get(30, TimeUnit.SECONDS);
      }
      pontoCritico.set(null);

      assertThat(emissao.getStatus()).as("emissão, rodada %d", rodada).isEqualTo(201);
      List<Number> listadas =
          JsonPath.read(emissao.getContentAsString(), "$.conteudo.despesas[*].id");
      boolean emitidaComADespesa = listadas.stream().anyMatch(id -> id.longValue() == despesa);
      boolean excluida =
          jdbc.queryForObject("select count(*) from despesa where id = ?", Integer.class, despesa)
              == 0;
      assertThat(emitidaComADespesa && excluida)
          .as("rodada %d: emissão com a despesa e despesa excluída", rodada)
          .isFalse();
      if (exclusao.getStatus() == 204) {
        assertThat(emitidaComADespesa).as("(a) rodada %d", rodada).isFalse();
        desfechos.merge("a: exclusão ganhou", 1, Integer::sum);
      } else {
        assertThat(exclusao.getStatus()).as("(b) rodada %d", rodada).isEqualTo(422);
        assertThat(emitidaComADespesa).as("(b) rodada %d", rodada).isTrue();
        assertThat(excluida).as("(b) rodada %d", rodada).isFalse();
        desfechos.merge("b: emissão ganhou", 1, Integer::sum);
      }
    }
    assertThat(desfechos.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(RODADAS);
  }
}
