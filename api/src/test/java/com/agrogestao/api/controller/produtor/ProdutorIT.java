package com.agrogestao.api.controller.produtor;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.config.UsuariosTeste;
import com.agrogestao.api.model.*;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.repository.*;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Integração da API de produtor com PostgreSQL efêmero. CPFs sintéticos, com DV válido. */
@Transactional
@WithMockUser(username = UsuariosTeste.LANCAMENTO, roles = "LANCAMENTO")
class ProdutorIT extends IntegracaoTest {

  private static final String CPF_A = "99900001079";
  private static final String CPF_B = "99900001150";
  private static final String CPF_C = "99900001230";
  private static final String MENSAGEM_COM_LANCAMENTOS =
      "Este produtor possui lançamentos e não pode ser excluído. Inative o cadastro.";
  private static final String MENSAGEM_COM_VINCULOS =
      "Este produtor possui propriedades ou relatórios vinculados e não pode ser excluído."
          + " Inative o cadastro.";

  @Autowired private MockMvc mockMvc;
  @Autowired private ProdutorRepository produtorRepository;
  @Autowired private PropriedadeRepository propriedadeRepository;
  @Autowired private CulturaRepository culturaRepository;
  @Autowired private PlantioRepository plantioRepository;
  @Autowired private ProducaoRepository producaoRepository;
  @Autowired private CategoriaDespesaRepository categoriaDespesaRepository;
  @Autowired private DespesaRepository despesaRepository;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void criarLancador() {
    new UsuariosTeste(jdbc, mockMvc).criar(Perfil.LANCAMENTO);
  }

  private static String corpo(
      String nome, String documento, String consentimentoEm, boolean ativo) {
    String consentimento = consentimentoEm == null ? "null" : "\"" + consentimentoEm + "\"";
    return "{\"nome\":\""
        + nome
        + "\",\"documento\":\""
        + documento
        + "\",\"consentimentoEm\":"
        + consentimento
        + ",\"ativo\":"
        + ativo
        + "}";
  }

  private ResultActions criar(String json) throws Exception {
    return mockMvc.perform(
        post("/api/v1/produtores").contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private long criarProdutor(String nome, String documento) throws Exception {
    String resposta =
        criar(corpo(nome, documento, null, true))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(resposta, "$.id")).longValue();
  }

  private Propriedade propriedadeDe(long produtorId) {
    Propriedade propriedade = new Propriedade();
    propriedade.setProdutor(produtorRepository.findById(produtorId).orElseThrow());
    propriedade.setNome("Propriedade Fictícia IT");
    return propriedadeRepository.save(propriedade);
  }

  @Test
  void deveCriarComLocationEConsentimentoPendenteQuandoDadosValidos() throws Exception {
    var resposta =
        criar(corpo("Produtor Fictício IT", CPF_A, null, true))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documento").value(CPF_A))
            .andExpect(jsonPath("$.ativo").value(true))
            .andExpect(jsonPath("$.consentimentoPendente").value(true))
            .andReturn()
            .getResponse();
    Number id = JsonPath.read(resposta.getContentAsString(), "$.id");
    org.hamcrest.MatcherAssert.assertThat(
        resposta.getHeader("Location"), endsWith("/api/v1/produtores/" + id));
  }

  @Test
  void deveDetalharEAlterarQuandoProdutorExistir() throws Exception {
    long id = criarProdutor("Produtor Fictício IT", CPF_A);

    mockMvc
        .perform(get("/api/v1/produtores/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Produtor Fictício IT"));

    mockMvc
        .perform(
            put("/api/v1/produtores/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Produtor Fictício IT alterado", CPF_A, "2026-01-10", false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Produtor Fictício IT alterado"))
        .andExpect(jsonPath("$.ativo").value(false))
        .andExpect(jsonPath("$.consentimentoEm").value("2026-01-10"))
        .andExpect(jsonPath("$.consentimentoPendente").value(false));
  }

  @Test
  void deveRetornar404QuandoProdutorInexistente() throws Exception {
    mockMvc
        .perform(get("/api/v1/produtores/{id}", Long.MAX_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.mensagem").value("Produtor não encontrado."));
    mockMvc
        .perform(delete("/api/v1/produtores/{id}", Long.MAX_VALUE))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveBuscarPorNomeOuCpfQuandoListar() throws Exception {
    long alfa = criarProdutor("Produtor Fictício Alfa IT", CPF_A);
    criarProdutor("Produtor Fictício Beta IT", CPF_B);

    mockMvc
        .perform(get("/api/v1/produtores").param("busca", "alfa it"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].id").value(alfa))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamanho").value(20));
    mockMvc
        .perform(get("/api/v1/produtores").param("busca", "999.000.010"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].documento").value(CPF_A));
    mockMvc
        .perform(get("/api/v1/produtores").param("busca", "Fictício").param("tamanho", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens", hasSize(1)))
        .andExpect(jsonPath("$.tamanho").value(1));
    mockMvc
        .perform(get("/api/v1/produtores").param("tamanho", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"));
  }

  @Test
  void deveRetornar400QuandoCpfInvalido() throws Exception {
    for (String documento : new String[] {"99900001078", "11111111111", "9990000107"}) {
      criar(corpo("Produtor Fictício IT", documento, null, true))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.erro").value("VALIDACAO"))
          .andExpect(jsonPath("$.campos[*].campo", everyItem(is("documento"))));
    }
  }

  @Test
  void deveRetornar400QuandoConsentimentoFuturo() throws Exception {
    criar(corpo("Produtor Fictício IT", CPF_A, LocalDate.now().plusDays(1).toString(), true))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos[0].campo").value("consentimentoEm"));
  }

  @Test
  void deveRetornar422QuandoCpfDuplicado() throws Exception {
    long id = criarProdutor("Produtor Fictício IT", CPF_A);
    criarProdutor("Outro Produtor Fictício IT", CPF_B);

    criar(corpo("Duplicado Fictício IT", CPF_A, null, true))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro").value("REGRA_NEGOCIO"))
        .andExpect(jsonPath("$.mensagem").value("Já existe produtor cadastrado com este CPF."))
        .andExpect(jsonPath("$.campos[*].campo", hasItem("documento")));
    mockMvc
        .perform(
            put("/api/v1/produtores/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Produtor Fictício IT", CPF_B, null, true)))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void deveExcluirQuandoProdutorNaoTiverVinculos() throws Exception {
    long id = criarProdutor("Produtor Fictício IT", CPF_A);

    mockMvc.perform(delete("/api/v1/produtores/{id}", id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/produtores/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar422QuandoExcluirProdutorComPropriedadeSemLancamento() throws Exception {
    long id = criarProdutor("Produtor Fictício IT", CPF_A);
    propriedadeDe(id);

    mockMvc
        .perform(delete("/api/v1/produtores/{id}", id))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.mensagem").value(MENSAGEM_COM_VINCULOS));
  }

  @Test
  void deveRetornar422QuandoExcluirProdutorComDespesa() throws Exception {
    long id = criarProdutor("Produtor Fictício IT", CPF_A);
    CategoriaDespesa categoria = new CategoriaDespesa();
    categoria.setNome("Categoria Fictícia Produtor IT");
    categoriaDespesaRepository.save(categoria);
    Despesa despesa = new Despesa();
    despesa.setProdutor(produtorRepository.findById(id).orElseThrow());
    despesa.setCategoriaDespesa(categoria);
    despesa.setDescricao("Despesa Fictícia IT");
    despesa.setValorTotal(new BigDecimal("10.00"));
    despesa.setDataFato(LocalDate.of(2026, 3, 1));
    despesa.setDataColeta(LocalDate.of(2026, 3, 2));
    despesaRepository.save(despesa);

    mockMvc
        .perform(delete("/api/v1/produtores/{id}", id))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.mensagem").value(MENSAGEM_COM_LANCAMENTOS));
  }

  @Test
  void deveRetornar422QuandoExcluirProdutorComProducaoNoPlantio() throws Exception {
    long id = criarProdutor("Produtor Fictício IT", CPF_C);
    Cultura cultura = new Cultura();
    cultura.setNome("Cultura Fictícia Produtor IT");
    cultura.setTipoCicloPadrao(TipoCiclo.CURTO);
    cultura.setUnidadeMedida("kg");
    culturaRepository.save(cultura);
    Plantio plantio = new Plantio();
    plantio.setPropriedade(propriedadeDe(id));
    plantio.setCultura(cultura);
    plantio.setArea(new BigDecimal("1.000"));
    plantio.setTipoCiclo(TipoCiclo.CURTO);
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    plantioRepository.save(plantio);
    Producao producao = new Producao();
    producao.setPlantio(plantio);
    producao.setQuantidade(new BigDecimal("5.000"));
    producao.setUnidadeMedida("kg");
    producao.setDataFato(LocalDate.of(2026, 3, 1));
    producao.setDataColeta(LocalDate.of(2026, 3, 2));
    producaoRepository.save(producao);

    mockMvc
        .perform(delete("/api/v1/produtores/{id}", id))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.mensagem").value(MENSAGEM_COM_LANCAMENTOS));
  }
}
