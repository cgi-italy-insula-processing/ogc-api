package com.cgi.eoss.ogcapi.processes.security;

import com.cgi.eoss.ogcapi.processes.config.InsulaHeadersProperties;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Custom HTTP interceptor used in Insula HTTP Client.
 * It adds authorization-related headers to the request.
 */
@AllArgsConstructor
public class RequestContextForwardingInterceptor implements ClientHttpRequestInterceptor {

    private final InsulaHeadersProperties insulaHeadersProperties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        RequestContext requestContext = RequestContextHolder.getCurrent();
        request.getHeaders().add(insulaHeadersProperties.getUser(), requestContext.user());
        request.getHeaders().add(insulaHeadersProperties.getTenant(), requestContext.tenant());

        return execution.execute(request, body);
    }
}
