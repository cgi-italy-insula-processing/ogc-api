package com.cgi.eoss.ogcapi.processes.security;

import com.cgi.eoss.ogcapi.processes.config.InsulaHeadersProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RequestContextForwardingInterceptorTest {

    private final ClientHttpRequestExecution clientHttpRequestExecution = Mockito.mock(ClientHttpRequestExecution.class);

    private RequestContextForwardingInterceptor requestContextForwardingInterceptor;

    @BeforeEach
    public void init() {
        requestContextForwardingInterceptor = new RequestContextForwardingInterceptor(createInsulaHeadersProperties());
    }

    @AfterEach
    public void shutdown() {
        verifyNoMoreInteractions(clientHttpRequestExecution);
    }

    @Test
    public void testIntercept_AddsRequestContextDataToHttpRequest() throws Exception {
        String userHeaderValue = "user";
        String tenantHeaderValue = "tenant";
        MockClientHttpRequest httpRequest = new MockClientHttpRequest();
        byte[] body = null;

        {
            try (ClientHttpResponse r = doAnswer((i) -> {
                assertThat(httpRequest.getHeaders().get("insula-user")).contains(userHeaderValue);
                assertThat(httpRequest.getHeaders().get("insula-tenant")).contains(tenantHeaderValue);
                return null;
            }).when(clientHttpRequestExecution).execute(httpRequest, body)) {
                assertThat(r).isNull();
            }
        }

        try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("user", "tenant"))) {
            try (ClientHttpResponse r = requestContextForwardingInterceptor.intercept(httpRequest, body, clientHttpRequestExecution)) {
                assertThat(r).isNull();
            }
        }

        try (ClientHttpResponse r = verify(clientHttpRequestExecution, times(1)).execute(httpRequest, body)) {
            assertThat(r).isNull();
        }
    }

    private InsulaHeadersProperties createInsulaHeadersProperties() {
        InsulaHeadersProperties insulaHeadersProperties = new InsulaHeadersProperties();
        insulaHeadersProperties.setUser("insula-user");
        insulaHeadersProperties.setTenant("insula-tenant");
        return insulaHeadersProperties;
    }
}
