package com.agrogestao.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do JWT, lida do {@code .env}: {@code JWT_SEGREDO} (chave de assinatura, sem valor
 * padrão) e {@code JWT_EXPIRACAO_MINUTOS}.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String segredo, long expiracaoMinutos) {}
