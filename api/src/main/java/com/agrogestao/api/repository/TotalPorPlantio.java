package com.agrogestao.api.repository;

import java.math.BigDecimal;

/** Soma de quantidade agrupada por plantio, sem recorte de período (avisos da RN29, L09b). */
public interface TotalPorPlantio {
  Long getPlantioId();

  BigDecimal getTotal();
}
