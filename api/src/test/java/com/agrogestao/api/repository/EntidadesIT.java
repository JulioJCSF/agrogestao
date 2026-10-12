package com.agrogestao.api.repository;

import static org.assertj.core.api.Assertions.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.model.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class EntidadesIT extends IntegracaoTest {
  @Autowired EntityManager em;
  @Autowired UsuarioRepository usuarioRepository;
  @Autowired ProdutorRepository produtorRepository;
  @Autowired PropriedadeRepository propriedadeRepository;
  @Autowired CulturaRepository culturaRepository;
  @Autowired PlantioRepository plantioRepository;
  @Autowired ProducaoRepository producaoRepository;
  @Autowired CategoriaDespesaRepository categoriaDespesaRepository;
  @Autowired ValorReferenciaDiariaRepository valorReferenciaDiariaRepository;
  @Autowired DiariaRepository diariaRepository;
  @Autowired DespesaRepository despesaRepository;
  @Autowired RateioDespesaRepository rateioDespesaRepository;
  @Autowired ClienteRepository clienteRepository;
  @Autowired VendaRepository vendaRepository;
  @Autowired ItemVendaRepository itemVendaRepository;
  @Autowired EmissaoRelatorioRepository emissaoRelatorioRepository;

  @Test
  @Transactional
  void deveGravarELerTodasAsEntidadesQuandoDerForValido() {
    Produtor produtor = new Produtor();
    produtor.setNome("Produtor Fictício IT");
    produtor.setDocumento("99900000039");
    produtor.setAtivo(true);
    produtorRepository.save(produtor);
    Propriedade propriedade = new Propriedade();
    propriedade.setProdutor(produtor);
    propriedade.setNome("Propriedade Fictícia IT");
    propriedadeRepository.save(propriedade);
    Cultura cultura = new Cultura();
    cultura.setNome("Cultura Fictícia IT");
    cultura.setTipoCicloPadrao(TipoCiclo.CURTO);
    cultura.setUnidadeMedida("kg");
    culturaRepository.save(cultura);
    Plantio plantio = new Plantio();
    plantio.setPropriedade(propriedade);
    plantio.setCultura(cultura);
    plantio.setArea(new BigDecimal("1.250"));
    plantio.setTipoCiclo(TipoCiclo.LONGO);
    plantio.setDataInicio(LocalDate.of(2026, 2, 1));
    plantioRepository.save(plantio);
    Producao producao = new Producao();
    producao.setPlantio(plantio);
    producao.setQuantidade(new BigDecimal("12.345"));
    producao.setUnidadeMedida("kg");
    producao.setDataFato(LocalDate.of(2026, 3, 1));
    producao.setDataColeta(LocalDate.of(2026, 3, 2));
    producaoRepository.save(producao);
    CategoriaDespesa categoria = new CategoriaDespesa();
    categoria.setNome("Categoria Fictícia IT");
    categoriaDespesaRepository.save(categoria);
    ValorReferenciaDiaria referencia = new ValorReferenciaDiaria();
    referencia.setValor(new BigDecimal("81.23"));
    referencia.setVigenciaInicio(LocalDate.of(2024, 1, 1));
    referencia.setVigenciaFim(LocalDate.of(2024, 1, 31));
    valorReferenciaDiariaRepository.save(referencia);
    Diaria diaria = new Diaria();
    diaria.setPlantio(plantio);
    diaria.setNatureza(NaturezaDiaria.FAMILIAR);
    diaria.setValorReferenciaDiaria(referencia);
    diaria.setQuantidadeDias(new BigDecimal("1.50"));
    diaria.setValorUnitario(new BigDecimal("81.23"));
    diaria.setDataFato(LocalDate.of(2024, 1, 5));
    diaria.setDataColeta(LocalDate.of(2024, 1, 6));
    diariaRepository.save(diaria);
    Despesa despesa = new Despesa();
    despesa.setProdutor(produtor);
    despesa.setCategoriaDespesa(categoria);
    despesa.setDescricao("Despesa Fictícia IT");
    despesa.setValorTotal(new BigDecimal("123.45"));
    despesa.setDataFato(LocalDate.of(2026, 3, 1));
    despesa.setDataColeta(LocalDate.of(2026, 3, 2));
    despesaRepository.save(despesa);
    RateioDespesa rateio = new RateioDespesa();
    rateio.setDespesa(despesa);
    rateio.setPlantio(plantio);
    rateio.setPercentual(new BigDecimal("1.000000"));
    rateio.setValorRateado(new BigDecimal("123.45"));
    rateioDespesaRepository.save(rateio);
    Cliente cliente = new Cliente();
    cliente.setNome("Cliente Fictício IT");
    cliente.setTipoCanal(TipoCanal.CEASA);
    clienteRepository.save(cliente);
    Venda venda = new Venda();
    venda.setProdutor(produtor);
    venda.setCliente(cliente);
    venda.setValorTotal(new BigDecimal("41.23"));
    venda.setDataFato(LocalDate.of(2026, 3, 1));
    venda.setDataColeta(LocalDate.of(2026, 3, 2));
    vendaRepository.save(venda);
    ItemVenda item = new ItemVenda();
    item.setVenda(venda);
    item.setPlantio(plantio);
    item.setQuantidade(new BigDecimal("1.000"));
    item.setUnidadeMedida("kg");
    item.setValorUnitario(new BigDecimal("41.23"));
    item.setValorTotal(new BigDecimal("41.23"));
    itemVendaRepository.save(item);
    Usuario admin = usuarioRepository.findByLogin("admin-teste-sintetico").orElseThrow();
    EmissaoRelatorio emissao = new EmissaoRelatorio();
    emissao.setProdutor(produtor);
    emissao.setUsuario(admin);
    emissao.setDataEmissao(Instant.now());
    emissao.setPeriodoInicio(LocalDate.of(2026, 1, 1));
    emissao.setPeriodoFim(LocalDate.of(2026, 3, 31));
    emissao.setConteudo("{\"resultado\": -123.45, \"origem\": \"fixture\"}");
    emissaoRelatorioRepository.save(emissao);
    em.flush();
    em.clear();
    assertThat(produtorRepository.findById(produtor.getId()).orElseThrow().getDocumento())
        .isEqualTo("99900000039");
    assertThat(propriedadeRepository.findById(propriedade.getId()).orElseThrow().getAreaTotal())
        .isNull();
    assertThat(culturaRepository.findById(cultura.getId()).orElseThrow().getTipoCicloPadrao())
        .isEqualTo(TipoCiclo.CURTO);
    assertThat(plantioRepository.findById(plantio.getId()).orElseThrow().getArea())
        .isEqualByComparingTo("1.250");
    assertThat(producaoRepository.findById(producao.getId()).orElseThrow().getQuantidade())
        .isEqualByComparingTo("12.345");
    assertThat(categoriaDespesaRepository.findById(categoria.getId()).orElseThrow().isSistema())
        .isFalse();
    assertThat(
            valorReferenciaDiariaRepository.findById(referencia.getId()).orElseThrow().getValor())
        .isEqualByComparingTo("81.23");
    assertThat(
            diariaRepository
                .findById(diaria.getId())
                .orElseThrow()
                .getValorReferenciaDiaria()
                .getId())
        .isEqualTo(referencia.getId());
    assertThat(despesaRepository.findById(despesa.getId()).orElseThrow().getRateios()).hasSize(1);
    assertThat(rateioDespesaRepository.findById(rateio.getId()).orElseThrow().getPercentual())
        .isEqualByComparingTo("1.000000");
    assertThat(clienteRepository.findById(cliente.getId()).orElseThrow().getTipoCanal())
        .isEqualTo(TipoCanal.CEASA);
    assertThat(vendaRepository.findById(venda.getId()).orElseThrow().getItens()).hasSize(1);
    assertThat(itemVendaRepository.findById(item.getId()).orElseThrow().getValorTotal())
        .isEqualByComparingTo("41.23");
    assertThat(emissaoRelatorioRepository.findById(emissao.getId()).orElseThrow().getConteudo())
        .contains("fixture", "-123.45");
    for (EntidadeAuditada entidade :
        List.of(
            produtor,
            propriedade,
            cultura,
            plantio,
            producao,
            categoria,
            referencia,
            diaria,
            despesa,
            rateio,
            cliente,
            venda,
            item,
            emissao)) {
      assertThat(entidade.getCriadoPor()).isEqualTo(admin.getId());
      assertThat(entidade.getCriadoEm()).isNotNull();
      assertThat(entidade.getAtualizadoEm()).isNotNull();
    }
    Instant criado = produtorRepository.findById(produtor.getId()).orElseThrow().getCriadoEm();
    Produtor relido = produtorRepository.findById(produtor.getId()).orElseThrow();
    relido.setNome("Produtor Fictício IT atualizado");
    produtorRepository.saveAndFlush(relido);
    assertThat(relido.getCriadoPor()).isEqualTo(admin.getId());
    assertThat(relido.getCriadoEm()).isEqualTo(criado);
    assertThat(relido.getAtualizadoEm()).isAfterOrEqualTo(criado);
  }
}
