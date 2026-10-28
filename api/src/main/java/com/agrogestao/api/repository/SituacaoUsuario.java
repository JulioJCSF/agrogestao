package com.agrogestao.api.repository;

import com.agrogestao.api.model.Perfil;

/** Perfil e situação do usuário, lidos a cada requisição sem carregar o hash da senha (F04b). */
public record SituacaoUsuario(Perfil perfil, boolean ativo) {}
