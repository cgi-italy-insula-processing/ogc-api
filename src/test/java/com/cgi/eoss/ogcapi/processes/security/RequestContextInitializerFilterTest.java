package com.cgi.eoss.ogcapi.processes.security;

import com.cgi.eoss.ogcapi.processes.config.InsulaHeadersProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class RequestContextInitializerFilterTest {

    private final FilterChain filterChain = Mockito.mock(FilterChain.class);

    private RequestContextInitializerFilter requestContextInitializerFilter;

    @BeforeEach
    public void init() {
        requestContextInitializerFilter = new RequestContextInitializerFilter(new InsulaHeadersProperties());
    }

    @AfterEach
    public void shutdown() {
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void testDoFilter_PopulatesCurrentRequestContextHolderWithRequestUserAndTenantInformation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String userHeaderValue = "userValue";
        request.addHeader("user", userHeaderValue);
        String tenantHeaderValue = "tenantValue";
        request.addHeader("tenant", tenantHeaderValue);
        HttpServletResponse response = null;

        {
            doAnswer((i) -> {
                assertThat(RequestContextHolder.getCurrent())
                        .isEqualTo(new RequestContext(userHeaderValue, tenantHeaderValue));
                return null;
            }).when(filterChain).doFilter(request, response);
        }

        assertThat(RequestContextHolder.getCurrent()).isNull();
        requestContextInitializerFilter.doFilter(request, response, filterChain);
        assertThat(RequestContextHolder.getCurrent()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilter_ThrowsIllegalStateException_WhenRequestContextIsNotValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = null;

        assertThat(RequestContextHolder.getCurrent()).isNull();
        assertThatThrownBy(() -> requestContextInitializerFilter.doFilter(request, response, filterChain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RequestContext is not valid");
        assertThat(RequestContextHolder.getCurrent()).isNull();
        verify(filterChain, times(0)).doFilter(request, response);
    }
}
