package com.agrogestao.api.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.model.Cultura;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.TipoCiclo;
import com.agrogestao.api.repository.CulturaRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Autor dos registros (RF03, RNF07) pela API, com token real do login. */
@Transactional
class AuditoriaIT extends IntegracaoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired CulturaRepository culturas;
  @Autowired AuditorAware<Long> autorDoRegistro;

  @Value("${ADMIN_LOGIN}")
  String adminLogin;

  UsuariosTeste usuarios;
  Long lancador, admin;

  @BeforeEach
  void preparar() {
    usuarios = new UsuariosTeste(jdbc, mvc);
    lancador = usuarios.criar(Perfil.LANCAMENTO);
    usuarios.criar(Perfil.ADMIN);
    admin = jdbc.queryForObject("select id from usuario where login = ?", Long.class, adminLogin);
  }

  static String cultura(String nome) {
    return "{\"nome\":\"" + nome + "\",\"tipoCicloPadrao\":\"CURTO\",\"unidadeMedida\":\"kg\"}";
  }

  ResultActions criarCultura(String bearer, String nome) throws Exception {
    return mvc.perform(
        post("/api/v1/culturas")
            .header(HttpHeaders.AUTHORIZATION, bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cultura(nome)));
  }

  Number id(ResultActions resultado) throws Exception {
    return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
  }

  Long autorDe(String tabela, Number id) {
    return jdbc.queryForObject("select criado_por from " + tabela + " where id=?", Long.class, id);
  }

  @Test
  void deveGravarUsuarioDoTokenQuandoLancamentoCriarCultura() throws Exception {
    Number id =
        id(
            criarCultura(usuarios.bearer(Perfil.LANCAMENTO), "Cultura Teste F04b Autor")
                .andExpect(status().isCreated()));
    assertThat(autorDe("cultura", id)).isEqualTo(lancador).isNotEqualTo(admin);
  }

  @Test
  void deveGravarLancadorNaDespesaENosRateiosQuandoLancarEmDoisPlantios() throws Exception {
    Long produtor =
        jdbc.queryForObject(
            "insert into produtor(nome,documento,criado_por) values ('Produtor Teste Auditoria','99900020294',?) returning id",
            Long.class,
            admin);
    Long propriedade =
        jdbc.queryForObject(
            "insert into propriedade(produtor_id,nome,area_total,criado_por) values (?,'Sítio Teste',10.000,?) returning id",
            Long.class,
            produtor,
            admin);
    Long milho = jdbc.queryForObject("select id from cultura where nome = 'Milho'", Long.class);
    String sql =
        "insert into plantio(propriedade_id,cultura_id,area,tipo_ciclo,data_inicio,criado_por) values (?,?,1.000,'LONGO','2026-02-01',?) returning id";
    Long p1 = jdbc.queryForObject(sql, Long.class, propriedade, milho, admin);
    Long p2 = jdbc.queryForObject(sql, Long.class, propriedade, milho, admin);
    Long adubo =
        jdbc.queryForObject(
            "select id from categoria_despesa where nome = 'Adubo e fertilizantes'", Long.class);
    Number despesa =
        id(
            mvc.perform(
                    post("/api/v1/despesas")
                        .header(HttpHeaders.AUTHORIZATION, usuarios.bearer(Perfil.LANCAMENTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"produtorId\":%d,\"categoriaDespesaId\":%d,\"descricao\":\"Despesa Teste Auditoria\",\"valorTotal\":90.00,\"dataFato\":\"2026-03-10\",\"plantioIds\":[%d,%d]}"
                                .formatted(produtor, adubo, p1, p2)))
                .andExpect(status().isCreated()));
    assertThat(autorDe("despesa", despesa)).isEqualTo(lancador);
    assertThat(
            jdbc.queryForList(
                "select criado_por from rateio_despesa where despesa_id=?", Long.class, despesa))
        .hasSize(2)
        .containsOnly(lancador);
  }

  @Test
  void deveManterAutorQuandoOutroUsuarioAlterar() throws Exception {
    Number id =
        id(
            criarCultura(usuarios.bearer(Perfil.LANCAMENTO), "Cultura Teste F04b Original")
                .andExpect(status().isCreated()));
    Map<String, Object> antes =
        jdbc.queryForMap("select criado_em,atualizado_em from cultura where id=?", id);
    mvc.perform(
            put("/api/v1/culturas/" + id)
                .header(HttpHeaders.AUTHORIZATION, usuarios.bearer(Perfil.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cultura("Cultura Teste F04b Alterada")))
        .andExpect(status().isOk());
    assertThat(autorDe("cultura", id)).isEqualTo(lancador);
    Map<String, Object> depois =
        jdbc.queryForMap("select criado_em,atualizado_em from cultura where id=?", id);
    assertThat(depois.get("criado_em")).isEqualTo(antes.get("criado_em"));
    assertThat(depois.get("atualizado_em")).isNotEqualTo(antes.get("atualizado_em"));
  }

  @Test
  void deveGravarAdminQuandoSalvarForaDeRequisicao() {
    SecurityContextHolder.clearContext();
    Cultura cultura = new Cultura();
    cultura.setNome("Cultura Teste F04b Sem Requisição");
    cultura.setTipoCicloPadrao(TipoCiclo.CURTO);
    cultura.setUnidadeMedida("kg");
    culturas.saveAndFlush(cultura);
    assertThat(autorDe("cultura", cultura.getId())).isEqualTo(admin);
  }

  @Test
  @WithMockUser(username = "nao-existe", roles = "LANCAMENTO")
  void deveFalharSemGravarComoAdminQuandoLoginSimuladoNaoExistir() throws Exception {
    assertThatThrownBy(autorDoRegistro::getCurrentAuditor)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("'nao-existe'");
    mvc.perform(
            post("/api/v1/culturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cultura("Cultura Teste F04b Sem Autor")))
        .andExpect(status().isInternalServerError());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cultura where nome = 'Cultura Teste F04b Sem Autor'",
                Long.class))
        .isZero();
  }
}
