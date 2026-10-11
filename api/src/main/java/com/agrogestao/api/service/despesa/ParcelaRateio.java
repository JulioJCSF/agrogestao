package com.agrogestao.api.service.despesa;

import java.math.BigDecimal;

/**
 * Parcela da despesa atribuída a um plantio. {@code valorRateado} (escala 2) é a fonte de verdade;
 * {@code percentual} (escala 6) é informativo e não deve ser usado para recalcular o valor.
 */
public record ParcelaRateio(Long plantioId, BigDecimal percentual, BigDecimal valorRateado) {}
