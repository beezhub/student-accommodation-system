package com.beezhub.student_accommodation.security;

import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MustChangePasswordFilterTest {

    private static final String TOKEN = "a.b.c";

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private MustChangePasswordFilter filter;

    private MockHttpServletRequest request(String method, String path, boolean withToken) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        if (withToken) {
            request.addHeader("Authorization", "Bearer " + TOKEN);
        }
        return request;
    }

    @Test
    void flagSet_blocksOrdinaryRequestWithForbidden() throws Exception {
        when(jwtUtil.extractMustChangePassword(TOKEN)).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/admin/users", true), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("must change your password");
        verify(chain, never()).doFilter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/change-password", "/api/auth/logout", "/api/auth/me", "/api/auth/login"})
    void flagSet_allowsThePathsNeededToClearIt(String path) throws Exception {
        when(jwtUtil.extractMustChangePassword(TOKEN)).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", path, true), response, chain);

        verify(chain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void flagNotSet_requestPassesThrough() throws Exception {
        when(jwtUtil.extractMustChangePassword(TOKEN)).thenReturn(false);

        filter.doFilter(request("GET", "/api/admin/users", true), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void noToken_requestPassesThroughToTheAuthFilters() throws Exception {
        filter.doFilter(request("POST", "/api/auth/login", false), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verify(jwtUtil, never()).extractMustChangePassword(any());
    }

    @Test
    void malformedToken_leftToTheJwtFilter() throws Exception {
        when(jwtUtil.extractMustChangePassword(TOKEN)).thenThrow(new IllegalArgumentException("bad token"));

        filter.doFilter(request("GET", "/api/admin/users", true), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void preflightRequest_isNeverBlocked() throws Exception {
        when(jwtUtil.extractMustChangePassword(TOKEN)).thenReturn(true);

        filter.doFilter(request("OPTIONS", "/api/admin/users", true), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }
}
