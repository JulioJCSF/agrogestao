package com.agrogestao.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiApplicationIT extends IntegracaoTest {
  @Autowired EntityManager entityManager;

  @Test
  void deveValidarTodoODerQuandoContextoIniciar() {
    assertThat(entityManager.getMetamodel().getEntities()).hasSize(15);
  }
}
