package com.agrogestao.api.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Clock dos *IT: segue o de produção até fixar(); restaurar() volta ao normal. */
public class RelogioAjustavel extends Clock {
  private final Clock producao;
  private volatile Clock atual;

  public RelogioAjustavel(Clock producao) {
    this.producao = producao;
    this.atual = producao;
  }

  public void fixar(Instant instante) {
    atual = Clock.fixed(instante, producao.getZone());
  }

  public void restaurar() {
    atual = producao;
  }

  @Override
  public ZoneId getZone() {
    return atual.getZone();
  }

  @Override
  public Clock withZone(ZoneId zona) {
    return atual.withZone(zona);
  }

  @Override
  public Instant instant() {
    return atual.instant();
  }
}
