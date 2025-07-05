package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        System.out.println("[JwtAuthenticationFilter] ====== START doFilterInternal ======");
        System.out.println("[JwtAuthenticationFilter] Request URI: " + request.getRequestURI());
        System.out.println("[JwtAuthenticationFilter] Request method: " + request.getMethod());
        
        final String authHeader = request.getHeader("Authorization");
        System.out.println("[JwtAuthenticationFilter] Authorization header: " + authHeader);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JwtAuthenticationFilter] No Bearer token found, skipping authentication");
            System.out.println("[JwtAuthenticationFilter] ====== END doFilterInternal ======");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        System.out.println("[JwtAuthenticationFilter] JWT token: " + jwt);
        
        final String username = jwtTokenUtil.extractUsername(jwt);
        System.out.println("[JwtAuthenticationFilter] Extracted username: " + username);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("[JwtAuthenticationFilter] Loading user details for username: " + username);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            System.out.println("[JwtAuthenticationFilter] Loaded user details: " + userDetails);
            
            if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                System.out.println("[JwtAuthenticationFilter] Token is valid, setting authentication");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("[JwtAuthenticationFilter] Authentication set successfully");
            } else {
                System.out.println("[JwtAuthenticationFilter] Token validation failed");
            }
        }
        System.out.println("[JwtAuthenticationFilter] ====== END doFilterInternal ======");
        filterChain.doFilter(request, response);
    }
} 