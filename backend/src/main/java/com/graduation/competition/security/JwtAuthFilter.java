package com.graduation.competition.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * MVP 单用户默认放行。预认证头存在时解析用户标识，为后续 JWT 接入保留扩展点。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String DEMO_USER_HEADER = "X-Demo-User";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String demoUser = request.getHeader(DEMO_USER_HEADER);
        request.setAttribute("currentUser", demoUser == null || demoUser.isBlank() ? "demo" : demoUser);
        filterChain.doFilter(request, response);
    }
}

