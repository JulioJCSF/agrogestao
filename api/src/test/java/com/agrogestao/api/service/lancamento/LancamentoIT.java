package com.agrogestao.api.service.lancamento;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.RelogioAjustavel;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.*;
import com.agrogestao.api.repository.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parte da L00 no CT057: a data de coleta é atribuída ao persistir, pelo listener, com o relógio da
 * aplicação. 2026-10-12T01:30Z é 22h30 de 11/10 em Fortaleza. O rollback limpa a massa.
 */
@Transactional
class LancamentoIT extends IntegracaoTest {
  static final LocalDate HOJE_EM_FORTALEZA = LocalDate.of(2026, 10, 11);

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager em;
  @Autowired RelogioAjustavel relogio;
  @Autowired ProdutorRepository produtores;
  @Autowired PropriedadeRepository propriedades;
  @Autowired CulturaRepository culturas;
  @Autowired PlantioRepository plantios;
  @Autowired CategoriaDespesaRepository categorias;
  @Autowired ClienteRepository clientes;
  @Autowired ProducaoRepository producoes;
  @Autowired DespesaRepository despesas;
  @Autowired DiariaRepository diarias;
  @Autowired VendaRepository vendas;

  Produtor produtor;
  Plantio plantio;

  @BeforeEach
  void prepararMassa() {
    // Um dos testes roda com @WithMockUser: a massa gravada aqui precisa do autor cadastrado.
    new UsuariosTeste(jdbc, mvc).criar(Perfil.LANCAMENTO);
    relogio.fixar(Instant.parse("2026-10-12T01:30:00Z"));
    produtor = new Produtor();
    produtor.setNome("Produtor Teste Relatório");
    produtor.setDocumento("99900001311");
    produtor.setConsentimentoEm(LocalDate.of(2026, 1, 15));
    produtor.setAtivo(true);
    produtores.save(produtor);
    Propriedade propriedade = new Propriedade();
    propriedade.setProdutor(produtor);
    propriedade.setNome("Sítio Teste");
    propriedade.setAreaTotal(new BigDecimal("10.000"));
    propriedades.save(propriedade);
    Long milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    plantio = new Plantio();
    plantio.setPropriedade(propriedade);
    plantio.setCultura(culturas.findById(milho).orElseThrow());
    plantio.setArea(new BigDecimal("1.000"));
    plantio.setTipoCiclo(TipoCiclo.LONGO);
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    plantios.save(plantio);
  }

  @AfterEach
  void restaurarRelogio() {
    relogio.restaurar();
  }

  LocalDate coletaGravada(String tabela, Long id) {
    return jdbc.queryForObject(
        "select data_coleta from " + tabela + " where id = ?", LocalDate.class, id);
  }

  @Test
  void deveGravarHojeEmFortalezaEManterNaAlteracaoQuandoProducaoVierComColetaDoCliente() {
    Producao producao = new Producao();
    producao.setPlantio(plantio);
    producao.setQuantidade(new BigDecimal("10.000"));
    producao.setUnidadeMedida("kg");
    producao.setDataFato(LocalDate.of(2026, 3, 10));
    producao.setDataColeta(LocalDate.of(2020, 1, 1));
    producoes.saveAndFlush(producao);
    assertThat(coletaGravada("producao", producao.getId())).isEqualTo(HOJE_EM_FORTALEZA);

    em.clear();
    relogio.fixar(OffsetDateTime.parse("2026-10-20T12:00:00-03:00").toInstant());
    Producao alterada = producoes.findById(producao.getId()).orElseThrow();
    alterada.setDataFato(LocalDate.of(2026, 3, 12));
    alterada.setQuantidade(new BigDecimal("12.000"));
    alterada.setDataColeta(LocalDate.of(2026, 10, 1));
    producoes.saveAndFlush(alterada);
    assertThat(
            jdbc.queryForObject(
                "select data_fato from producao where id = ?", LocalDate.class, producao.getId()))
        .isEqualTo(LocalDate.of(2026, 3, 12));
    assertThat(coletaGravada("producao", producao.getId())).isEqualTo(HOJE_EM_FORTALEZA);
  }

  @Test
  void deveGravarHojeEmFortalezaQuandoDespesaDiariaEVendaForemGravadas() {
    Long adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    Despesa despesa = new Despesa();
    despesa.setProdutor(produtor);
    despesa.setCategoriaDespesa(categorias.findById(adubo).orElseThrow());
    despesa.setDescricao("Adubo de teste");
    despesa.setValorTotal(new BigDecimal("50.00"));
    despesa.setDataFato(LocalDate.of(2026, 3, 10));
    despesa.setDataColeta(LocalDate.of(2020, 1, 1));
    despesas.saveAndFlush(despesa);

    Diaria diaria = new Diaria();
    diaria.setPlantio(plantio);
    diaria.setNatureza(NaturezaDiaria.PAGA);
    diaria.setQuantidadeDias(new BigDecimal("1.00"));
    diaria.setValorUnitario(new BigDecimal("90.00"));
    diaria.setDataFato(LocalDate.of(2026, 3, 10));
    diarias.saveAndFlush(diaria);

    Cliente cliente = new Cliente();
    cliente.setNome("Cliente Teste Relatório");
    cliente.setTipoCanal(TipoCanal.FEIRA);
    clientes.save(cliente);
    Venda venda = new Venda();
    venda.setProdutor(produtor);
    venda.setCliente(cliente);
    venda.setValorTotal(new BigDecimal("22.50"));
    venda.setDataFato(LocalDate.of(2026, 3, 10));
    vendas.saveAndFlush(venda);

    assertThat(coletaGravada("despesa", despesa.getId())).isEqualTo(HOJE_EM_FORTALEZA);
    assertThat(coletaGravada("diaria", diaria.getId())).isEqualTo(HOJE_EM_FORTALEZA);
    assertThat(coletaGravada("venda", venda.getId())).isEqualTo(HOJE_EM_FORTALEZA);
  }

  /** CT057: a data de coleta enviada pelo operador é ignorada. */
  @Test
  @WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
  void deveIgnorarDataColetaQuandoVierNoCorpoDaRequisicao() throws Exception {
    mvc.perform(
            post("/api/v1/plantios/" + plantio.getId() + "/producoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"quantidade\":10.000,\"unidadeMedida\":\"kg\",\"dataFato\":\"2026-03-10\",\"dataColeta\":\"2020-01-01\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dataColeta").value("2026-10-11"));
  }
}
