package com.agrogestao.api.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;

@Getter
@Setter
@ParameterObject
public class PaginacaoConsulta {
  @Min(0)
  private int pagina = 0;

  @Min(1)
  @Max(100)
  private int tamanho = 20;

  public Pageable pageable() {
    return PageRequest.of(pagina, tamanho, Sort.by("id"));
  }
}
