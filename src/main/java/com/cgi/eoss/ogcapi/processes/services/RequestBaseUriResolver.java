package com.cgi.eoss.ogcapi.processes.services;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Utility component for deriving the request base URI from the current web request.
 */
@Component
public class RequestBaseUriResolver {

    /**
     * Resolves the base URI (scheme + host + optional port)
     *
     * @param nativeWebRequest the request to resolve the base URI from
     * @throws IllegalStateException if the {@link HttpServletRequest} cannot be resolved
     */
    public URI resolveFrom(NativeWebRequest nativeWebRequest) {
        HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("Unable to resolve HttpServletRequest from NativeWebRequest");
        }

        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUri();
    }
}
