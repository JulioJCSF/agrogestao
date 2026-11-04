package com.agrogestao.api.service.saldo;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agrogestao.api.dto.SaldoPlantioResposta;
import com.agrogestao.api.exception.RecursoNaoEncontradoException;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * M05: saldo = produzido − vendido acumulados (RN29). As somas são do banco ({@code sum} nas
 * consultas agrupadas); aqui os repositórios devolvem o total como o banco devolveria.
 */
class SaldoPlantioServiceTest {
  ProducaoRepository producoes = mock(ProducaoRepository.class);
  ItemVendaRepository itensVenda = mock(ItemVendaRepository.class);
  PlantioRepository plantios = mock(PlantioRepository.class);
  SaldoPlantioService service = new SaldoPlantioService(producoes, itensVenda, plantios);

  record Total(Long getPlantioId, BigDecimal getTotal) implements TotalPorPlantio {}

  @BeforeEach
  void preparar() {
    Cultura milho = new Cultura();
    milho.setUnidadeMedida(" kg ");
    Plantio plantio = new Plantio();
    ReflectionTestUtils.setField(plantio, "id", 1L);
    plantio.setCultura(milho);
    when(plantios.findById(1L)).thenReturn(Optional.of(plantio));
  }

  void registrado(String produzido, String vendido) {
    when(producoes.somarQuantidadePorPlantio(Set.of(1L)))
        .thenReturn(
            produzido == null ? List.of() : List.of(new Total(1L, new BigDecimal(produzido))));
    when(itensVenda.somarQuantidadePorPlantio(Set.of(1L)))
        .thenReturn(vendido == null ? List.of() : List.of(new Total(1L, new BigDecimal(vendido))));
  }

  static void confere(
      SaldoPlantioResposta resposta, String produzido, String vendido, String saldo) {
    assertThat(resposta.plantioId()).isEqualTo(1L);
    assertThat(resposta.quantidadeProduzida()).isEqualTo(new BigDecimal(produzido));
    assertThat(resposta.quantidadeVendida()).isEqualTo(new BigDecimal(vendido));
    assertThat(resposta.saldo()).isEqualTo(new BigDecimal(saldo));
    assertThat(resposta.unidadeMedida()).isEqualTo("kg");
  }

  @Test
  void deveSubtrairOVendidoDoProduzidoQuandoHouverSaldoCt047() {
    registrado("100.000", "60.000");
    confere(service.saldo(1L), "100.000", "60.000", "40.000");
  }

  @Test
  void deveDevolverSaldoNegativoQuandoVendidoPassarDoProduzidoCt047() {
    registrado("100.000", "150.000");
    confere(service.saldo(1L), "100.000", "150.000", "-50.000");
  }

  @Test
  void deveDevolverZeroComTresCasasQuandoNaoHouverProducaoNemVenda() {
    registrado(null, null);
    confere(service.saldo(1L), "0.000", "0.000", "0.000");
  }

  @Test
  void deveManterAsTresCasasSemArredondarQuandoTotaisVieremDoBanco() {
    // 33,333 + 66,667 = 100,000 e 10,001 + 20,002 + 30,003 = 60,006 (somados pelo banco).
    registrado("100.000", "60.006");
    confere(service.saldo(1L), "100.000", "60.006", "39.994");
  }

  @Test
  void deveResponder404SemConsultarTotaisQuandoPlantioNaoExistir() {
    assertThatThrownBy(() -> service.saldo(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Plantio não encontrado.");
    verifyNoInteractions(producoes, itensVenda);
  }

  @Test
  void deveConsultarCadaFonteUmaVezQuandoPedirTotaisDeVariosPlantios() {
    when(producoes.somarQuantidadePorPlantio(any()))
        .thenReturn(List.of(new Total(1L, new BigDecimal("5.000"))));
    when(itensVenda.somarQuantidadePorPlantio(any()))
        .thenReturn(List.of(new Total(2L, new BigDecimal("7.000"))));
    var totais = service.totais(Set.of(1L, 2L, 3L));
    assertThat(totais.produzido(1L)).isEqualByComparingTo("5");
    assertThat(totais.vendido(2L)).isEqualByComparingTo("7");
    assertThat(totais.produzido(3L)).isZero();
    verify(producoes).somarQuantidadePorPlantio(Set.of(1L, 2L, 3L));
    verify(itensVenda).somarQuantidadePorPlantio(Set.of(1L, 2L, 3L));
    service.totais(Set.of());
    verifyNoMoreInteractions(producoes, itensVenda);
  }
}
