package com.cgi.eoss.ogcapi.processes.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestContextTest {

    @Test
    public void testIsValid_ReturnsTrue_WhenBothUserAndTenantArePopulated() {
        RequestContext requestContext = new RequestContext("user", "tenant");
        assertThat(requestContext.isValid()).isTrue();
    }

    @Test
    public void testIsValid_ReturnsFalse_WhenUserIsPopulatedAndTenantIsEmpty() {
        RequestContext requestContext = new RequestContext("user", "");
        assertThat(requestContext.isValid()).isFalse();
    }

    @Test
    public void testIsValid_ReturnsFalse_WhenUserIsPopulatedAndTenantIsNull() {
        RequestContext requestContext = new RequestContext("user", null);
        assertThat(requestContext.isValid()).isFalse();
    }

    @Test
    public void testIsValid_ReturnsFalse_WhenTenantIsPopulatedAndUserIsEmpty() {
        RequestContext requestContext = new RequestContext("", "tenant");
        assertThat(requestContext.isValid()).isFalse();
    }

    @Test
    public void testIsValid_ReturnsFalse_WhenTenantIsPopulatedAndUserIsNull() {
        RequestContext requestContext = new RequestContext(null, "tenant");
        assertThat(requestContext.isValid()).isFalse();
    }


    @Test
    public void testIsValid_ReturnsFalse_WhenBothUserAndTenantAreEmpty() {
        RequestContext requestContext = new RequestContext("", "");
        assertThat(requestContext.isValid()).isFalse();
    }

    @Test
    public void testIsValid_ReturnsFalse_WhenBothUserAndTenantAreNull() {
        RequestContext requestContext = new RequestContext(null, null);
        assertThat(requestContext.isValid()).isFalse();
    }
}
