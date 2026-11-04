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
import java.math.BigDecimal;
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
 * A03b e A03d: exclusão de despesa ou de produção × emissão do mesmo produtor, em paralelo. Sem o
 * lock do produtor, as duas transações se encontram no pior ponto: a exclusão já conferiu que o
 * período não foi emitido e a emissão já leu a despesa. O teste segura cada uma nesse ponto por até
 * 2 s, esperando a outra; com o lock, a segunda nem chega lá antes de a primeira terminar. Na
 * produção, a emissão a lê na apuração, antes das despesas: o ponto crítico da emissão é o mesmo.
 *
 * <p>Aceitável: (a) a exclusão ganhou e a emissão não contém o lançamento, ou (b) a emissão ganhou
 * e a exclusão tomou 422. Nunca a emissão com o lançamento e o lançamento excluído. Um produtor
 * novo por rodada, para nenhuma emissão anterior proteger a despesa da rodada seguinte.
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
          "delete from producao where plantio_id in (select p.id from plantio p join propriedade r on r.id = p.propriedade_id where r.produtor_id = ?)",
          id);
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

  /** Produtor com consentimento e um plantio aberto desde 2026-02-01; devolve o plantio. */
  long plantio(String nome, String cpf) {
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,consentimento_em,criado_por) values (?,?,'2026-01-15',?) returning id",
            Long.class,
            nome,
            cpf,
            autor);
    produtores.add(produtor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            autor);
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1.000,'LONGO','2026-02-01',?) returning id",
        Long.class,
        propriedade,
        milho,
        autor);
  }

  /** Despesa de 2026-03-10 num produtor novo; devolve a despesa. */
  long massa(int rodada) throws Exception {
    long plantio = plantio("Produtor Teste Concorrência " + rodada, "9990000170" + rodada);
    Long produtor = produtores.getLast();
    MockHttpServletResponse criada =
        executar(
            post("/api/v1/despesas"),
            "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Concorrência\",\"valorTotal\":80.00,\"dataFato\":\"2026-03-10\",\"plantioIds\":[%d]}"
                .formatted(produtor, adubo, plantio));
    assertThat(criada.getStatus()).isEqualTo(201);
    return ((Number) JsonPath.read(criada.getContentAsString(), "$.id")).longValue();
  }

  /** DELETE do caminho × emissão de 01/01 a 30/06 do produtor; devolve [exclusão, emissão]. */
  MockHttpServletResponse[] correr(String caminho, Long produtor) throws Exception {
    pontoCritico.set(new CyclicBarrier(2));
    CountDownLatch largada = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<MockHttpServletResponse> excluir =
          executor.submit(
              () -> {
                largada.await();
                return mvc.perform(delete(caminho).header(HttpHeaders.AUTHORIZATION, lancamento))
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
      return new MockHttpServletResponse[] {
        excluir.get(30, TimeUnit.SECONDS), emitir.get(30, TimeUnit.SECONDS)
      };
    } finally {
      pontoCritico.set(null);
    }
  }

  @Test
  @Tag("P1")
  void deveNuncaEmitirDespesaExcluidaQuandoExclusaoEEmissaoCorreremEmParalelo() throws Exception {
    Map<String, Integer> desfechos = new TreeMap<>();
    for (int rodada = 0; rodada < RODADAS; rodada++) {
      long despesa = massa(rodada);
      Long produtor = produtores.get(rodada);
      MockHttpServletResponse[] respostas = correr("/api/v1/despesas/" + despesa, produtor);
      MockHttpServletResponse exclusao = respostas[0], emissao = respostas[1];

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

  /** A03d: a produção de 2026-03-10 é a prova de atividade; a emissão a conta na apuração. */
  @Test
  @Tag("P1")
  void deveNuncaEmitirProducaoExcluidaQuandoExclusaoEEmissaoCorreremEmParalelo() throws Exception {
    Map<String, Integer> desfechos = new TreeMap<>();
    for (int rodada = 0; rodada < RODADAS; rodada++) {
      long plantio =
          plantio("Produtor Teste Concorrência Produção " + rodada, "9990000171" + rodada);
      Long produtor = produtores.getLast();
      MockHttpServletResponse criada =
          executar(
              post("/api/v1/plantios/" + plantio + "/producoes"),
              "{\"quantidade\":10.000,\"unidadeMedida\":\"kg\",\"dataFato\":\"2026-03-10\"}");
      assertThat(criada.getStatus()).isEqualTo(201);
      long producao = ((Number) JsonPath.read(criada.getContentAsString(), "$.id")).longValue();

      MockHttpServletResponse[] respostas = correr("/api/v1/producoes/" + producao, produtor);
      MockHttpServletResponse exclusao = respostas[0], emissao = respostas[1];

      assertThat(emissao.getStatus()).as("emissão, rodada %d", rodada).isEqualTo(201);
      List<Number> produzido =
          JsonPath.read(emissao.getContentAsString(), "$.conteudo.plantios[*].quantidadeProduzida");
      boolean emitidaComAProducao =
          produzido.stream().anyMatch(q -> new BigDecimal(q.toString()).signum() > 0);
      boolean excluida =
          jdbc.queryForObject("select count(*) from producao where id = ?", Integer.class, producao)
              == 0;
      assertThat(emitidaComAProducao && excluida)
          .as("rodada %d: emissão com a produção e produção excluída", rodada)
          .isFalse();
      if (exclusao.getStatus() == 204) {
        assertThat(emitidaComAProducao).as("(a) rodada %d", rodada).isFalse();
        desfechos.merge("a: exclusão ganhou", 1, Integer::sum);
      } else {
        assertThat(exclusao.getStatus()).as("(b) rodada %d", rodada).isEqualTo(422);
        assertThat(emitidaComAProducao).as("(b) rodada %d", rodada).isTrue();
        assertThat(excluida).as("(b) rodada %d", rodada).isFalse();
        desfechos.merge("b: emissão ganhou", 1, Integer::sum);
      }
    }
    assertThat(desfechos.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(RODADAS);
  }
}
