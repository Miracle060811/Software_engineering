package com.travelmate.backend.config;

import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAccessTokenAfterTokenVersionChanges() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserMapper userMapper = mock(UserMapper.class);
        JwtFilter filter = filter(jwtUtil, userMapper);
        when(jwtUtil.extractPrincipal("token")).thenReturn(new AuthenticatedUser(7L, "traveler", 0));
        when(jwtUtil.extractTokenVersion("token")).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(user(0, 2));

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void usesCurrentDatabaseRoleInsteadOfStaleJwtRole() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserMapper userMapper = mock(UserMapper.class);
        JwtFilter filter = filter(jwtUtil, userMapper);
        when(jwtUtil.extractPrincipal("token")).thenReturn(new AuthenticatedUser(7L, "traveler", 1));
        when(jwtUtil.extractTokenVersion("token")).thenReturn(3);
        when(userMapper.selectById(7L)).thenReturn(user(0, 3));

        filter.doFilterInternal(request(), new MockHttpServletResponse(), mock(FilterChain.class));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(0, authentication.getAuthorities().size());
        assertEquals(0, ((AuthenticatedUser) authentication.getPrincipal()).role());
    }

    private JwtFilter filter(JwtUtil jwtUtil, UserMapper userMapper) {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "userMapper", userMapper);
        return filter;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private User user(int role, int tokenVersion) {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        user.setRole(role);
        user.setTokenVersion(tokenVersion);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
