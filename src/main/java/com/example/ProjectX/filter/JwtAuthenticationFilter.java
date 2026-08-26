package com.example.ProjectX.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.ProjectX.exception.filter.InvalidTokenException;
import com.example.ProjectX.model.User;
import com.example.ProjectX.repository.UserRepository;
import com.example.ProjectX.token.JwtTokenProvider;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final HandlerExceptionResolver resolver;
    private final UserRepository userRepository;

    

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, 
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.resolver = resolver;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/register") || path.startsWith("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            String email = jwtTokenProvider.getEmailFromToken(jwt);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User activeUser = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid email"));
                List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_" + activeUser.getRole()));
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(activeUser, null, roles);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }  
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            resolver.resolveException(request, response, null, new InvalidTokenException("Token has expired"));
            return;
        } catch (JwtException ex) {
            resolver.resolveException(request, response, null, new InvalidTokenException("Invalid token"));
            return;
        }
    }
}
