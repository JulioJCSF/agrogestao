package com.agrogestao.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequisicao(@NotBlank String login, @NotBlank String senha) {}
