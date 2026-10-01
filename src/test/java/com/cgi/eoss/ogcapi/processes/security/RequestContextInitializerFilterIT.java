package com.cgi.eoss.ogcapi.processes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SpringBootTest
@TestPropertySource(properties = {
        "ogcapi.processes.insula.headers.user=eosso-person",
        "ogcapi.processes.insula.headers.tenant=eosso-tenant",
})
public class RequestContextInitializerFilterIT {

    @Autowired
    private RequestContextInitializerFilter requestContextInitializerFilter;

    private final FilterChain filterChain = Mockito.mock(FilterChain.class);

    @AfterEach
    public void shutdown() {
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    public void testDoFilter_PopulatesCurrentRequestContextHolderWithRequestUserAndTenantInformationWithHeaderNamesFromProperties() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String userHeaderValue = "userValue";
        request.addHeader("eosso-person", userHeaderValue);
        String tenantHeaderValue = "tenantValue";
        request.addHeader("eosso-tenant", tenantHeaderValue);
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

}
