package com.agrogestao.api.controller.despesa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.repository.PlantioRepository;
import com.agrogestao.api.service.despesa.DespesaService;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.plantio.PlantioService;
import com.agrogestao.api.service.venda.VendaService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Lock da despesa (decisão de 03/11, L04b): despesa em 01/07 e encerramento em 30/06 do mesmo
 * plantio não podem ter sucesso juntos (RN04); e despesa e venda nos mesmos dois plantios, em
 * paralelo, travam na mesma ordem crescente e não entram em deadlock.
 */
class DespesaConcorrenciaIT extends IntegracaoTest {
  static final LocalDate JULHO = LocalDate.of(2024, 7, 1);

  @Autowired JdbcTemplate jdbc;
  @Autowired DespesaService despesas;
  @Autowired VendaService vendas;
  @Autowired PlantioService plantios;
  @MockitoSpyBean LancamentoService lancamentos;
  @MockitoSpyBean PlantioRepository plantiosRepo;
  @Autowired EntityManager em;
  Long autor, produtor, propriedade, cliente, adubo, p1, p2;

  @BeforeEach
  void preparar() {
    autor =
        jdbc.queryForObject(
            "select id from usuario where perfil='ADMIN' order by id limit 1", Long.class);
    produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Despesa','99900001745',?) returning id",
            Long.class,
            autor);
    propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,criado_por) values (?,'Sítio Teste',?) returning id",
            Long.class,
            produtor,
            autor);
    cliente =
        jdbc.queryForObject(
            "insert into cliente(nome,tipo_canal,criado_por) values ('Cliente Teste Despesa','FEIRA',?) returning id",
            Long.class,
            autor);
    adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    p1 = plantio();
    p2 = plantio();
  }

  @AfterEach
  void limpar() {
    jdbc.update("delete from despesa where produtor_id=?", produtor);
    jdbc.update("delete from venda where produtor_id=?", produtor);
    jdbc.update("delete from plantio where propriedade_id=?", propriedade);
    jdbc.update("delete from propriedade where id=?", propriedade);
    jdbc.update("delete from produtor where id=?", produtor);
    jdbc.update("delete from cliente where id=?", cliente);
  }

  Long plantio() {
    Long cultura = jdbc.queryForObject("select id from cultura where nome='Milho'", Long.class);
    return jdbc.queryForObject(
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1,'LONGO','2024-02-01',?) returning id",
        Long.class,
        propriedade,
        cultura,
        autor);
  }

  DespesaRequisicao despesa(List<Long> plantioIds) {
    return new DespesaRequisicao(
        produtor, adubo, "Adubo de teste", new BigDecimal("80.00"), JULHO, plantioIds);
  }

  @Test
  void deveRejeitarEncerramentoAnteriorQuandoDespesaConcorrenteJaEstiverEmGravacao()
      throws Exception {
    CountDownLatch lendo = new CountDownLatch(1), liberar = new CountDownLatch(1);
    doAnswer(
            inv -> {
              lendo.countDown();
              assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
              return inv.callRealMethod();
            })
        .when(lancamentos)
        .validarDataFato(any(LocalDate.class), anyCollection());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<?> despesa = executor.submit(() -> despesas.criar(despesa(List.of(p1))));
      assertThat(lendo.await(10, TimeUnit.SECONDS)).isTrue();
      Future<?> encerramento =
          executor.submit(
              () ->
                  plantios.encerrar(
                      p1, new EncerramentoPlantioRequisicao(LocalDate.of(2024, 6, 30))));
      try {
        encerramento.get(1, TimeUnit.SECONDS);
      } catch (TimeoutException esperado) {
        /* Aguarda o lock da despesa. */
      }
      liberar.countDown();
      despesa.get(10, TimeUnit.SECONDS);
      assertThatThrownBy(() -> encerramento.get(10, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RegraNegocioException.class);
      assertThat(
              jdbc.queryForObject(
                  "select data_encerramento from plantio where id=?", LocalDate.class, p1))
          .isNull();
    } finally {
      liberar.countDown();
    }
  }

  @Test
  void deveConcluirAsDuasSemDeadlockQuandoDespesaEVendaTravaremOsMesmosPlantios() throws Exception {
    // Ordens de entrada opostas: despesa [P2, P1] e venda [P1, P2]. Depois do primeiro lock, cada
    // uma espera a outra pegar o seu (até 2 s). Travando na ordem informada, cada uma seguraria um
    // plantio e esperaria o outro: deadlock, que o PostgreSQL aborta numa delas. Em ordem
    // crescente, a segunda fica parada no lock de P1 e a primeira segue sozinha.
    // O spy do repositório é proxy de interface: o lock de verdade vem de outra instância dele.
    PlantioRepository real = new JpaRepositoryFactory(em).getRepository(PlantioRepository.class);
    CyclicBarrier primeiroLock = new CyclicBarrier(2);
    ThreadLocal<Boolean> jaTravou = ThreadLocal.withInitial(() -> false);
    doAnswer(
            inv -> {
              Object plantio = real.buscarParaAtualizar(inv.getArgument(0));
              if (!jaTravou.get()) {
                jaTravou.set(true);
                try {
                  primeiroLock.await(2, TimeUnit.SECONDS);
                } catch (TimeoutException | BrokenBarrierException sozinha) {
                  /* A outra transação está parada no lock. */
                }
              }
              return plantio;
            })
        .when(plantiosRepo)
        .buscarParaAtualizar(anyLong());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<?> despesa = executor.submit(() -> despesas.criar(despesa(List.of(p2, p1))));
      Future<?> venda =
          executor.submit(
              () ->
                  vendas.criar(
                      new VendaRequisicao(
                          produtor,
                          cliente,
                          JULHO,
                          List.of(
                              new ItemVendaRequisicao(
                                  p1, BigDecimal.ONE, "kg", new BigDecimal("2.00")),
                              new ItemVendaRequisicao(
                                  p2, BigDecimal.ONE, "kg", new BigDecimal("2.00"))))));
      despesa.get(20, TimeUnit.SECONDS);
      venda.get(20, TimeUnit.SECONDS);
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from despesa where produtor_id=?", Integer.class, produtor))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from venda where produtor_id=?", Integer.class, produtor))
        .isEqualTo(1);
  }
}
