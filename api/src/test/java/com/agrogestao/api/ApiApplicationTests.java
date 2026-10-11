package com.agrogestao.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.segredo=segredo-de-teste-com-mais-de-32-caracteres")
class ApiApplicationTests {

  @Test
  void contextLoads() {}
}
