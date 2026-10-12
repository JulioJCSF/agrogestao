package com.agrogestao.api.controller.auth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrogestao.api.IntegracaoTest;
import com.agrogestao.api.model.Perfil;
import com.agrogestao.api.model.Usuario;
import com.agrogestao.api.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integração do login com PostgreSQL efêmero. Cria os próprios usuários e desfaz tudo ao fim de
 * cada teste.
 */
@Transactional
class AuthIT extends IntegracaoTest {

  private static final String SENHA = "senha-de-teste";
  private static final String MENSAGEM_LOGIN_INVALIDO = "Login ou senha inválidos.";
  private static final String MENSAGEM_NAO_AUTENTICADO =
      "Você não está autenticado. Faça login para continuar.";

  @Autowired private MockMvc mockMvc;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtEncoder jwtEncoder;

  private Usuario ativo;

  @BeforeEach
  void criarUsuarios() {
    String hash = passwordEncoder.encode(SENHA);
    ativo =
        usuarioRepository.saveAndFlush(
            new Usuario("Operadora de Teste", "it-ativo", hash, Perfil.LANCAMENTO, true));
    usuarioRepository.saveAndFlush(
        new Usuario("Inativo de Teste", "it-inativo", hash, Perfil.CONSULTA, false));
  }

  private ResultActions logar(String login, String senha) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\": \"" + login + "\", \"senha\": \"" + senha + "\"}"));
  }

  private String tokenValido() throws Exception {
    String corpo =
        logar("it-ativo", SENHA)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(corpo, "$.token");
  }

  private ResultActions me(String token) throws Exception {
    return mockMvc.perform(
        get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
  }

  private static void assertErro401(ResultActions resultado, String mensagem) throws Exception {
    resultado
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.timestamp").value(notNullValue()))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"))
        .andExpect(jsonPath("$.mensagem").value(mensagem))
        .andExpect(jsonPath("$.campos", hasSize(0)));
  }

  @Test
  void deveRetornarTokenEUsuarioQuandoLoginValido() throws Exception {
    logar("it-ativo", SENHA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.expiraEm").isNotEmpty())
        .andExpect(jsonPath("$.usuario.id").value(ativo.getId()))
        .andExpect(jsonPath("$.usuario.nome").value("Operadora de Teste"))
        .andExpect(jsonPath("$.usuario.perfil").value("LANCAMENTO"))
        .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
  }

  @Test
  void deveRetornar401QuandoSenhaErrada() throws Exception {
    assertErro401(logar("it-ativo", "senha-errada"), MENSAGEM_LOGIN_INVALIDO);
  }

  @Test
  void deveRetornar401QuandoLoginInexistente() throws Exception {
    assertErro401(logar("it-nao-existe", SENHA), MENSAGEM_LOGIN_INVALIDO);
  }

  @Test
  void deveRetornar401QuandoUsuarioInativo() throws Exception {
    assertErro401(logar("it-inativo", SENHA), MENSAGEM_LOGIN_INVALIDO);
  }

  @Test
  void deveRetornar400QuandoLoginOuSenhaEmBranco() throws Exception {
    logar("", "")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("VALIDACAO"))
        .andExpect(jsonPath("$.campos", hasSize(2)));
  }

  @Test
  void deveRetornarUsuarioAtualQuandoTokenValido() throws Exception {
    me(tokenValido())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ativo.getId()))
        .andExpect(jsonPath("$.nome").value("Operadora de Teste"))
        .andExpect(jsonPath("$.perfil").value("LANCAMENTO"));
  }

  @Test
  void deveRetornar401QuandoSemToken() throws Exception {
    assertErro401(mockMvc.perform(get("/api/v1/auth/me")), MENSAGEM_NAO_AUTENTICADO);
  }

  @Test
  void deveRetornar401QuandoTokenAdulterado() throws Exception {
    String[] partes = tokenValido().split("\\.");
    // Troca o primeiro caractere da assinatura: o conteúdo continua legível, mas não confere mais.
    char trocado = partes[2].charAt(0) == 'A' ? 'B' : 'A';
    String adulterado = partes[0] + "." + partes[1] + "." + trocado + partes[2].substring(1);

    assertErro401(me(adulterado), MENSAGEM_NAO_AUTENTICADO);
  }

  @Test
  void deveRetornar401QuandoTokenExpirado() throws Exception {
    Instant emissao = Instant.now().minus(2, ChronoUnit.HOURS);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(String.valueOf(ativo.getId()))
            .issuedAt(emissao)
            .expiresAt(emissao.plus(1, ChronoUnit.HOURS))
            .build();
    String expirado =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    assertErro401(me(expirado), MENSAGEM_NAO_AUTENTICADO);
  }

  @Test
  void deveExporEsquemaBearerNoSwaggerSemToken() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        // Lista vazia sobrescreve a exigência global: o login aparece sem cadeado.
        .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security", hasSize(0)))
        .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security").doesNotExist())
        .andExpect(jsonPath("$.security[0].bearerAuth").exists());
  }
}
