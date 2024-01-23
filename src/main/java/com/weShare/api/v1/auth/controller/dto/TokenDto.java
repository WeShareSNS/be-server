package com.weShare.api.v1.auth.controller.dto;


import java.util.Objects;

public record TokenDto(String accessToken, String refreshToken) {
    public TokenDto {
      Objects.requireNonNull(accessToken);
      Objects.requireNonNull(refreshToken);
    }
}