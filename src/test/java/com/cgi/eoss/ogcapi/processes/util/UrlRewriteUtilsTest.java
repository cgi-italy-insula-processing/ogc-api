package com.cgi.eoss.ogcapi.processes.util;

import org.junit.jupiter.api.Test;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class UrlRewriteUtilsTest {

    @Test
    void testRewriteBaseUrl_ReturnsOriginal_WhenTargetBaseIsNull() {
        assertThat(UrlRewriteUtils.rewriteBaseUrl("http://eoepca-server-service:8090/", null))
                .isEqualTo("http://eoepca-server-service:8090/");
    }

    @Test
    void testRewriteBaseUrl_ReturnsNull_WhenOriginalHrefIsNull() {
        assertThat(UrlRewriteUtils.rewriteBaseUrl(null, URI.create("http://x"))).isNull();
    }

    @Test
    void testRewriteBaseUrl_TrimsTrailingSlashFromTargetBase() {
        String original = "http://eoepca-server-service:8090/secure/api/v2.0/files/1?token=abc#frag";
        URI target = URI.create("https://processing.example.com/");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("https://processing.example.com/secure/api/v2.0/files/1?token=abc#frag");
    }

    @Test
    void testRewriteBaseUrl_rewritesAuthorityOnlyUrl() {
        String original = "http://eoepca-server-service:8090";
        URI target = URI.create("https://processing.example.com");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("https://processing.example.com");
    }

    @Test
    void testRewriteBaseUrl_preservesQueryWhenNoPath() {
        String original = "http://eoepca-server-service:8090?x=1";
        URI target = URI.create("https://processing.example.com");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("https://processing.example.com?x=1");
    }

    @Test
    void testRewriteBaseUrl_preservesFragmentWhenNoPath() {
        String original = "http://eoepca-server-service:8090#frag";
        URI target = URI.create("https://processing.example.com");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("https://processing.example.com#frag");
    }

    @Test
    void testRewriteBaseUrl_doesNotRewriteRelativeHref() {
        String original = "/secure/api/v2.0/files/1";
        URI target = URI.create("https://processing.example.com");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("/secure/api/v2.0/files/1");
    }

    @Test
    void testRewriteBaseUrl_UsesStringReplacement_WhenHrefContainsUriTemplate() {
        String original = "https://eoepca-server-service:8090/secure/api/v2.0/platformFiles/4864{?projection}/dl#frag";
        URI target = URI.create("http://processing.example.com");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("http://processing.example.com/secure/api/v2.0/platformFiles/4864{?projection}/dl#frag");
    }

    @Test
    void testRewriteBaseUrl_FallsBackToStringReplacement_WhenOriginalHrefIsNotParseable() {
        String original = "http://eoepca-server-service:8090/secure/api/v2.0/files/1?token=ab c#frag";
        URI target = URI.create("https://processing.example.com");
        assertThat(UrlRewriteUtils.rewriteBaseUrl(original, target))
                .isEqualTo("https://processing.example.com/secure/api/v2.0/files/1?token=ab c#frag");
    }
}
