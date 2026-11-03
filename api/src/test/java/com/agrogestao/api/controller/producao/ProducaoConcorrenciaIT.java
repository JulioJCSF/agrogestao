package com.agrogestao.api.controller.producao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.dto.*;
import com.agrogestao.api.exception.RegraNegocioException;
import com.agrogestao.api.model.Plantio;
import com.agrogestao.api.service.lancamento.LancamentoService;
import com.agrogestao.api.service.plantio.PlantioService;
import com.agrogestao.api.service.producao.ProducaoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class ProducaoConcorrenciaIT extends IntegracaoTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired ProducaoService producoes;
  @Autowired PlantioService plantios;
  @MockitoSpyBean LancamentoService lancamentos;

  @Test
  void deveRejeitarEncerramentoAnteriorQuandoProducaoConcorrenteJaEstiverEmGravacao()
      throws Exception {
    Long autor =
        jdbc.queryForObject(
            "select id from usuario where perfil='ADMIN' order by id limit 1", Long.class);
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Concorrência','99900002554',?) returning id",
            Long.class,
            autor);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,criado_por) values (?,'Sítio Teste Concorrência',?) returning id",
            Long.class,
            produtor,
            autor);
    Long cultura = jdbc.queryForObject("select id from cultura where nome='Milho'", Long.class);
    Long plantio =
        jdbc.queryForObject(
            "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1,'LONGO','2024-02-01',?) returning id",
            Long.class,
            propriedade,
            cultura,
            autor);
    CountDownLatch lendo = new CountDownLatch(1), liberar = new CountDownLatch(1);
    doAnswer(
            inv -> {
              lendo.countDown();
              assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
              return inv.callRealMethod();
            })
        .when(lancamentos)
        .validarDataFato(any(LocalDate.class), any(Plantio.class));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<?> producao =
          executor.submit(
              () ->
                  producoes.criar(
                      plantio,
                      new ProducaoRequisicao(BigDecimal.ONE, "kg", LocalDate.of(2024, 7, 1))));
      assertThat(lendo.await(10, TimeUnit.SECONDS)).isTrue();
      Future<?> encerramento =
          executor.submit(
              () ->
                  plantios.encerrar(
                      plantio, new EncerramentoPlantioRequisicao(LocalDate.of(2024, 6, 30))));
      try {
        encerramento.get(1, TimeUnit.SECONDS);
      } catch (TimeoutException esperado) {
        /* Aguarda o lock da produção. */
      }
      liberar.countDown();
      producao.get(10, TimeUnit.SECONDS);
      assertThatThrownBy(() -> encerramento.get(10, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(RegraNegocioException.class);
      assertThat(
              jdbc.queryForObject(
                  "select data_encerramento from plantio where id=?", LocalDate.class, plantio))
          .isNull();
    } finally {
      liberar.countDown();
      jdbc.update("delete from producao where plantio_id=?", plantio);
      jdbc.update("delete from plantio where id=?", plantio);
      jdbc.update("delete from propriedade where id=?", propriedade);
      jdbc.update("delete from produtor where id=?", produtor);
    }
  }
}
