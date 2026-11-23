package com.example.proyectoweb.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class TokenJwtConfig {

    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "application/json";

    // Clave persistente por defecto para desarrollo y testing (HMAC-SHA256 requiere mínimo 256 bits / 32 bytes)
    private static final String DEFAULT_SECRET = "campus_recursos_jwt_secret_key_2026_super_secure_32bytes!";
    private static final long DEFAULT_EXPIRATION = 86400000L; // 24 horas

    public static SecretKey SECRET_KEY = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
    public static long EXPIRATION_TIME = DEFAULT_EXPIRATION;

    @Value("${security.jwt.secret:#{null}}")
    public void setSecretKey(String secret) {
        if (secret != null && !secret.isBlank()) {
            SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Value("${security.jwt.expiration:86400000}")
    public void setExpirationTime(long expiration) {
        EXPIRATION_TIME = expiration;
    }
}
