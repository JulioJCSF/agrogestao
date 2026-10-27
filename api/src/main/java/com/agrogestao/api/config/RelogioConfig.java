package com.agrogestao.api.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {
  @Bean
  public Clock relogio() {
    return Clock.system(ZoneId.of("America/Fortaleza"));
  }

  // @PastOrPresent e afins usam o mesmo "hoje" de America/Fortaleza, não o fuso da JVM.
  @Bean
  public static ValidationConfigurationCustomizer relogioDaValidacao(Clock relogio) {
    return configuracao -> configuracao.clockProvider(() -> relogio);
  }
}
