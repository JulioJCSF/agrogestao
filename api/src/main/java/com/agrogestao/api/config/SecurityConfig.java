package com.agrogestao.api.config;

import com.agrogestao.api.exception.JsonAcessoNegadoHandler;
import com.agrogestao.api.exception.JsonPontoDeEntradaDeAutenticacao;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Matriz de permissões da F04b (RF02, RNF06), centralizada aqui por método HTTP. CONSULTA só lê;
 * LANCAMENTO lê e grava; ADMIN tudo, e só ele acessa {@code /usuarios}. A ordem importa: a regra de
 * {@code /usuarios} vem antes do GET geral.
 */
@Configuration
public class SecurityConfig {
  private static final String CONSULTA = "CONSULTA";
  private static final String LANCAMENTO = "LANCAMENTO";
  private static final String ADMIN = "ADMIN";

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JsonPontoDeEntradaDeAutenticacao authenticationEntryPoint,
      JsonAcessoNegadoHandler accessDeniedHandler,
      ConversorDeToken conversorDeToken)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                // ERROR é o encaminhamento interno do contêiner, não uma rota que o cliente chame.
                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                    .authenticated()
                    .requestMatchers("/api/v1/usuarios/**")
                    .hasRole(ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/v1/**")
                    .hasAnyRole(CONSULTA, LANCAMENTO, ADMIN)
                    .requestMatchers(HttpMethod.POST, "/api/v1/**")
                    .hasAnyRole(LANCAMENTO, ADMIN)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                    .hasAnyRole(LANCAMENTO, ADMIN)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                    .hasAnyRole(LANCAMENTO, ADMIN)
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/**")
                    .hasAnyRole(LANCAMENTO, ADMIN)
                    .anyRequest()
                    .denyAll())
        // Token ausente, adulterado ou expirado também responde no formato único de erro.
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeToken))
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .build();
  }
}
