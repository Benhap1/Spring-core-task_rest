package com.gymcrm.gym_crm_spring.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Component
public class TokenRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = httpRequest.getHeader("X-Auth-Token");

        if (token != null) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));
            RequestContextHolder.getRequestAttributes()
                    .setAttribute("X-Auth-Token", token, RequestAttributes.SCOPE_REQUEST);
        }

        chain.doFilter(request, response);
    }
}