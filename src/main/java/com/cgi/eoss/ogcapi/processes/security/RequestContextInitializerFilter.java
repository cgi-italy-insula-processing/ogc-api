package com.cgi.eoss.ogcapi.processes.security;

import com.cgi.eoss.ogcapi.processes.config.InsulaHeadersProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

import java.io.IOException;

/**
 * Custom filter component that wraps the servlet request and response within the RequestContextHolder
 * populated with user and tenant information coming from the request.
 */
@AllArgsConstructor
public class RequestContextInitializerFilter implements Filter {

    private final InsulaHeadersProperties insulaHeadersProperties;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String tenant = ((HttpServletRequest) servletRequest).getHeader(insulaHeadersProperties.getTenant());
        String user = ((HttpServletRequest) servletRequest).getHeader(insulaHeadersProperties.getUser());

        try (RequestContextHolder rc = new RequestContextHolder(new RequestContext(user, tenant))) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
