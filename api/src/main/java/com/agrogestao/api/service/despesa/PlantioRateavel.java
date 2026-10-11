package com.agrogestao.api.service.despesa;

import java.math.BigDecimal;

/** Plantio vinculado a uma despesa, com a área (em hectares) usada como peso no rateio. */
public record PlantioRateavel(Long plantioId, BigDecimal area) {}
