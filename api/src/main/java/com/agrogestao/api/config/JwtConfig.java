package com.agrogestao.api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Emissão e validação de JWT com o suporte nativo do Spring Security (HS256). O mesmo segredo
 * assina o token no login e confere a assinatura em cada requisição.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  /** HS256 exige chave de pelo menos 256 bits. */
  static final int TAMANHO_MINIMO_SEGREDO = 32;

  @Bean
  public SecretKey chaveJwt(JwtProperties propriedades) {
    String segredo = propriedades.segredo();
    if (segredo == null
        || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
      throw new IllegalStateException(
          "JWT_SEGREDO ausente ou curto demais: defina no .env um valor com pelo menos "
              + TAMANHO_MINIMO_SEGREDO
              + " caracteres.");
    }
    return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
  }

  @Bean
  public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
    return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
