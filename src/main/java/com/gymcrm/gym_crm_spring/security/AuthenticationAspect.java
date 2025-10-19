package com.gymcrm.gym_crm_spring.security;

import com.gymcrm.gym_crm_spring.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthenticationAspect {

    private final TokenStore tokenStore;

    @Before("@annotation(com.gymcrm.gym_crm_spring.security.RequireAuthentication)")
    public void authenticate() {

        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new InvalidCredentialsException("No request context found — cannot authenticate user.");
        }

        var request = attributes.getRequest();
        String token = request.getHeader("X-Auth-Token");

        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException("Missing authentication token in header: X-Auth-Token");
        }

        var username = tokenStore.validateToken(token);

        if (username.isEmpty()) {
            throw new InvalidCredentialsException("Invalid or expired authentication token");
        }
    }
}
