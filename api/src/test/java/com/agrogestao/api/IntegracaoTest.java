package com.agrogestao.api;

import com.agrogestao.api.config.PostgresTesteConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Base compartilhada por todos os *IT; não é um teste unitário. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTesteConfig.class)
public abstract class IntegracaoTest {}
