package com.agrogestao.api.controller.relatorio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.*;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.repository.DespesaRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
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
 * A03e: o conteúdo de cada emissão é coerente consigo mesmo com lançamentos sendo criados em
 * paralelo (RN19, RN20, RN31, RN34). A emissão lê o banco em várias consultas: a apuração de cada
 * plantio (custo pelos rateios), depois a lista de despesas e por fim a de diárias. Uma diária paga
 * ou despesa confirmada entre essas consultas aparece numa parte do documento e falta em outra.
 *
 * <p>Para tornar a janela visível, a lista de despesas da emissão espera um pouco antes e depois de
 * ler. Com o lock do produtor na criação, nada do produtor é gravado enquanto a emissão lê, e a
 * espera não muda nada; sem ele, a corrida aparece já nas primeiras emissões.
 */
class EmissaoConsistenciaConcorrenciaIT extends IntegracaoTest {
  static final Duration DURACAO = Duration.ofSeconds(4);
  static final long PAUSA_MS = 25;

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelogioAjustavel relogio;
  @Autowired EntityManager em;
  @MockitoSpyBean DespesaRepository despesas;
  UsuariosTeste usuarios;
  String lancamento;
  Long autor, milho, adubo, produtor, propriedade;

  @BeforeEach
  void preparar() throws Exception {
    relogio.fixar(Instant.parse("2026-11-04T15:00:00Z"));
    usuarios = new UsuariosTeste(jdbc, mvc);
    usuarios.criar(Perfil.LANCAMENTO);
    lancamento = usuarios.bearer(Perfil.LANCAMENTO);
    autor = usuarios.id(Perfil.LANCAMENTO);
    milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,consentimento_em,criado_por) values ('Produtor Teste Consistência','99900002301','2026-01-15',?) returning id",
            Long.class,
            autor);
    propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            autor);

    // O spy é proxy de interface: a leitura real vem de outra instância do repositório.
    DespesaRepository despesasReais =
        new JpaRepositoryFactory(em).getRepository(DespesaRepository.class);
    doAnswer(
            inv -> {
              Thread.sleep(PAUSA_MS);
              var lidas =
                  despesasReais.buscarDoRelatorio(
                      inv.getArgument(0), inv.<LocalDate>getArgument(1), inv.getArgument(2));
              Thread.sleep(PAUSA_MS);
              return lidas;
            })
        .when(despesas)
        .buscarDoRelatorio(anyLong(), any(), any());
  }

  @AfterEach
  void limpar() {
    relogio.restaurar();
    LimpezaTeste.apagarEmissoes(jdbc, produtor);
    jdbc.update("delete from despesa where produtor_id = ?", produtor);
    jdbc.update(
        "delete from diaria where plantio_id in (select id from plantio where propriedade_id = ?)",
        propriedade);
    jdbc.update("delete from plantio where propriedade_id = ?", propriedade);
    jdbc.update("delete from propriedade where id = ?", propriedade);
    jdbc.update("delete from produtor where id = ?", produtor);
    jdbc.update("delete from usuario where login = ?", UsuariosTeste.LANCAMENTO);
  }

  Long plantio(String area) {
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,?,'LONGO','2026-02-01',?) returning id",
        Long.class,
        propriedade,
        milho,
        new BigDecimal(area),
        autor);
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

  MockHttpServletResponse emitir() throws Exception {
    return executar(
        post("/api/v1/relatorios"),
        "{\"produtorId\":%d,\"periodoInicio\":\"2026-01-01\",\"periodoFim\":\"2026-10-31\"}"
            .formatted(produtor));
  }

  MockHttpServletResponse diariaPaga(Long plantio) throws Exception {
    return executar(
        post("/api/v1/plantios/" + plantio + "/diarias"),
        "{\"natureza\":\"PAGA\",\"quantidadeDias\":1.50,\"valorUnitario\":90.01,\"dataFato\":\"2026-03-10\"}");
  }

  MockHttpServletResponse despesa(Long... plantios) throws Exception {
    return executar(
        post("/api/v1/despesas"),
        "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Consistência\",\"valorTotal\":100.01,\"dataFato\":\"2026-04-15\",\"plantioIds\":%s}"
            .formatted(produtor, adubo, Arrays.toString(plantios)));
  }

  /** Repete a ação até o prazo; devolve quantas vezes deu o status esperado ou a primeira falha. */
  static Callable<Integer> emLaco(
      long prazo, int esperado, String nome, Callable<MockHttpServletResponse> acao) {
    return () -> {
      int feitas = 0;
      while (System.nanoTime() < prazo) {
        MockHttpServletResponse resposta = acao.call();
        assertThat(resposta.getStatus())
            .as("%s: %s", nome, resposta.getContentAsString())
            .isEqualTo(esperado);
        feitas++;
      }
      return feitas;
    };
  }

  static BigDecimal decimal(Object numero) {
    return new BigDecimal(numero.toString());
  }

  static BigDecimal somar(List<?> numeros) {
    return numeros.stream()
        .map(EmissaoConsistenciaConcorrenciaIT::decimal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Os três invariantes no conteúdo gravado: toda diária paga listada tem a despesa listada; a soma
   * dos rateios listados para os plantios do relatório é o custo; e o custo é a soma dos plantios.
   */
  static void conferir(Long emissao, String conteudo) {
    List<Number> diariasPagas = JsonPath.read(conteudo, "$.diarias[?(@.natureza == 'PAGA')].id");
    List<Number> geradoras = JsonPath.read(conteudo, "$.despesas[*].diariaId");
    Set<Long> comDespesa = new HashSet<>();
    geradoras.stream().filter(Objects::nonNull).forEach(id -> comDespesa.add(id.longValue()));
    assertThat(diariasPagas)
        .as("emissão %d: diária paga sem a despesa gerada", emissao)
        .allSatisfy(id -> assertThat(comDespesa).contains(id.longValue()));

    BigDecimal custo = decimal(JsonPath.read(conteudo, "$.custoDesembolso"));
    List<Number> plantios = JsonPath.read(conteudo, "$.plantios[*].plantioId");
    List<Map<String, Object>> rateios = JsonPath.read(conteudo, "$.despesas[*].rateios[*]");
    BigDecimal listado =
        somar(
            rateios.stream()
                .filter(
                    r ->
                        plantios.stream()
                            .anyMatch(
                                p -> p.longValue() == ((Number) r.get("plantioId")).longValue()))
                .map(r -> r.get("valorRateado"))
                .toList());
    assertThat(listado)
        .as("emissão %d: despesas listadas × custo de desembolso", emissao)
        .isEqualByComparingTo(custo);
    assertThat(somar(JsonPath.read(conteudo, "$.plantios[*].custoDesembolso")))
        .as("emissão %d: soma dos plantios × custo de desembolso", emissao)
        .isEqualByComparingTo(custo);
  }

  @Test
  @Tag("P1")
  void deveGravarConteudoCoerenteQuandoEmissoesCorreremComCriacaoDeDiariaPagaEDespesa()
      throws Exception {
    Long p1 = plantio("1.000"), p2 = plantio("3.000");
    long prazo = System.nanoTime() + DURACAO.toNanos();
    int emissoes, diarias, lancadas;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<Integer> emitindo = executor.submit(emLaco(prazo, 201, "emissão", this::emitir));
      Future<Integer> pagando =
          executor.submit(
              emLaco(
                  prazo,
                  201,
                  "diária paga",
                  new Callable<>() {
                    int vez;

                    public MockHttpServletResponse call() throws Exception {
                      return diariaPaga(vez++ % 2 == 0 ? p1 : p2);
                    }
                  }));
      Future<Integer> lancando =
          executor.submit(emLaco(prazo, 201, "despesa", () -> despesa(p2, p1)));
      emissoes = emitindo.get(60, TimeUnit.SECONDS);
      diarias = pagando.get(60, TimeUnit.SECONDS);
      lancadas = lancando.get(60, TimeUnit.SECONDS);
    }
    // Sem carga dos dois lados, o teste não prova nada.
    assertThat(emissoes).as("emissões").isGreaterThanOrEqualTo(5);
    assertThat(diarias).as("diárias pagas").isGreaterThanOrEqualTo(5);
    assertThat(lancadas).as("despesas").isGreaterThanOrEqualTo(5);

    List<Map<String, Object>> gravadas =
        jdbc.queryForList(
            "select id, conteudo from emissao_relatorio where produtor_id = ? order by id",
            produtor);
    assertThat(gravadas).hasSize(emissoes);
    for (Map<String, Object> gravada : gravadas)
      conferir(((Number) gravada.get("id")).longValue(), gravada.get("conteudo").toString());
  }

  /**
   * Ordem única de locks (produtor → plantio → cultura): alteração de plantio, despesa nos mesmos
   * plantios e encerramento, em paralelo, sempre terminam. Um deadlock no PostgreSQL vira erro
   * depois de {@code deadlock_timeout} (1 s); uma espera circular presa estoura o prazo curto.
   */
  @Test
  void deveTerminarSemDeadlockQuandoAlterarPlantioLancarDespesaEEncerrarEmParalelo()
      throws Exception {
    for (int rodada = 0; rodada < 5; rodada++) {
      Long p1 = plantio("1.000"), p2 = plantio("3.000");
      String alteracao =
          "{\"propriedadeId\":%d,\"culturaId\":%d,\"area\":2.000,\"dataInicio\":\"2026-02-01\"}"
              .formatted(propriedade, milho);
      String encerramento = "{\"dataEncerramento\":\"2026-10-31\"}";
      List<Callable<MockHttpServletResponse>> acoes =
          List.of(
              () -> executar(put("/api/v1/plantios/" + p1), alteracao),
              () -> executar(put("/api/v1/plantios/" + p2), alteracao),
              () -> despesa(p2, p1),
              () -> executar(post("/api/v1/plantios/" + p1 + "/encerramento"), encerramento),
              () -> executar(post("/api/v1/plantios/" + p2 + "/encerramento"), encerramento));
      CountDownLatch largada = new CountDownLatch(1);
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<MockHttpServletResponse>> futuros = new ArrayList<>();
        for (var acao : acoes)
          futuros.add(
              executor.submit(
                  () -> {
                    largada.await();
                    return acao.call();
                  }));
        largada.countDown();
        for (int n = 0; n < futuros.size(); n++) {
          MockHttpServletResponse resposta = futuros.get(n).get(10, TimeUnit.SECONDS);
          assertThat(resposta.getStatus())
              .as("rodada %d, ação %d: %s", rodada, n, resposta.getContentAsString())
              .isBetween(200, 201);
        }
      }
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from plantio where id in (?,?) and area = 2.000 and data_encerramento = '2026-10-31'",
                  Integer.class,
                  p1,
                  p2))
          .as("rodada %d", rodada)
          .isEqualTo(2);
    }
  }
}
