package com.cgi.eoss.ogcapi.processes.services;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestBaseUriResolverTest {

    private final RequestBaseUriResolver resolver = new RequestBaseUriResolver();

    @Test
    void testResolveFrom_ReturnsBaseUriWithoutPathAndQuery_WhenHttpServletRequestIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("int.insula.earth");
        request.setServerPort(8080);
        request.setRequestURI("/testogcapi/jobs/4421");
        request.setQueryString("projection=detailedJob&foo=bar");

        NativeWebRequest webRequest = new ServletWebRequest(request);

        URI baseUri = resolver.resolveFrom(webRequest);

        assertThat(baseUri).isEqualTo(URI.create("https://int.insula.earth:8080"));
    }

    @Test
    void testResolveFrom_ThrowsIllegalStateException_WhenHttpServletRequestIsMissing() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveFrom(webRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to resolve HttpServletRequest from NativeWebRequest");
    }
}