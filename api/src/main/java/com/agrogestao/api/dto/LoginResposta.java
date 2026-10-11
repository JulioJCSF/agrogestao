package com.agrogestao.api.dto;

import java.time.Instant;

/** Resposta do login: o token, o instante em que ele expira e o usuário autenticado. */
public record LoginResposta(String token, Instant expiraEm, UsuarioLogadoResposta usuario) {}
