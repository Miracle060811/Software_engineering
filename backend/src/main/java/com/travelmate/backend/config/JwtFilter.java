package com.travelmate.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import com.travelmate.common.AuthenticatedUser;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                AuthenticatedUser principal = jwtUtil.extractPrincipal(token);
                if (principal.userId() != null && principal.username() != null && !principal.username().isBlank()) {
                    User currentUser = userMapper.selectById(principal.userId());
                    int currentTokenVersion = currentUser == null || currentUser.getTokenVersion() == null
                            ? 0
                            : currentUser.getTokenVersion();
                    boolean active = currentUser != null
                            && Integer.valueOf(1).equals(currentUser.getStatus())
                            && !Integer.valueOf(1).equals(currentUser.getDeleted())
                            && principal.username().equals(currentUser.getUsername())
                            && currentTokenVersion == principal.tokenVersion();
                    if (!active) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    AuthenticatedUser currentPrincipal = new AuthenticatedUser(
                            currentUser.getId(),
                            currentUser.getUsername(),
                            currentUser.getRole(),
                            currentTokenVersion);
                    List<SimpleGrantedAuthority> authorities = currentPrincipal.isAdmin()
                            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : List.of();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(currentPrincipal, null,
                            authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // token无效，不设置认证信息
            }
        }

        filterChain.doFilter(request, response);
    }
}
