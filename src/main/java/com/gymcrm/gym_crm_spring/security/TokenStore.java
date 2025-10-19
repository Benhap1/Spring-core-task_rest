package com.gymcrm.gym_crm_spring.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, String> tokenStorage = new ConcurrentHashMap<>();

    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        tokenStorage.put(token, username);
        return token;
    }
    public Optional<String> validateToken(String token) {
        return Optional.ofNullable(tokenStorage.get(token));
    }

    public void invalidateToken(String token) {
        tokenStorage.remove(token);
    }
}
