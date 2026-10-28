package com.agrogestao.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrogestao.api.model.Perfil;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Um usuário sintético por perfil (F04b). O token sai do login real, para o conversor de token
 * rodar; o {@code jwt()} do spring-security-test pularia o conversor.
 *
 * <p>Os testes que só precisam gravar usam {@code @WithMockUser(username = LANCAMENTO, roles =
 * "LANCAMENTO")} e criam o usuário com {@link #criar(Perfil)} no {@code @BeforeEach}: o autor do
 * registro é resolvido pelo login.
 */
public class UsuariosTeste {
  public static final String CONSULTA = "consulta-teste-f04b";
  public static final String LANCAMENTO = "lancamento-teste-f04b";
  public static final String ADMIN = "admin-teste-f04b";
  static final String SENHA = "senha-sintetica-exclusiva-f04b";
  private static final String HASH = new BCryptPasswordEncoder().encode(SENHA);

  private final JdbcTemplate jdbc;
  private final MockMvc mvc;

  public UsuariosTeste(JdbcTemplate jdbc, MockMvc mvc) {
    this.jdbc = jdbc;
    this.mvc = mvc;
  }

  public static String login(Perfil perfil) {
    return switch (perfil) {
      case CONSULTA -> CONSULTA;
      case LANCAMENTO -> LANCAMENTO;
      case ADMIN -> ADMIN;
    };
  }

  /** Cria o usuário do perfil, ativo, e devolve o id. */
  public Long criar(Perfil perfil) {
    return jdbc.queryForObject(
        "insert into usuario(nome,login,senha_hash,perfil) values (?,?,?,?) returning id",
        Long.class,
        "Usuário Teste F04b " + perfil.name(),
        login(perfil),
        HASH,
        perfil.name());
  }

  public Long id(Perfil perfil) {
    return jdbc.queryForObject("select id from usuario where login = ?", Long.class, login(perfil));
  }

  /** Cabeçalho {@code Authorization} com o token do login real do usuário do perfil. */
  public String bearer(Perfil perfil) throws Exception {
    String resposta =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"login\":\"" + login(perfil) + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(resposta, "$.token");
  }
}
