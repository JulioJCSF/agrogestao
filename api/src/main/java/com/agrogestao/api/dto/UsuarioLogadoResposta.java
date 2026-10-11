package com.agrogestao.api.dto;

import com.agrogestao.api.model.Perfil;

/** Dados básicos do usuário autenticado. */
public record UsuarioLogadoResposta(Long id, String nome, Perfil perfil) {}
