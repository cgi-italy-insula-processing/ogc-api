package com.cgi.eoss.ogcapi.processes.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class RequestContextHolderTest {

    @Test
    public void testGetCurrent_RequestContextIsNull_WhenRequestContextIsNotSet() {
        assertThat(RequestContextHolder.getCurrent()).isNull();
    }

    @Test
    public void testGetCurrent_ReturnsProvidedRequestContext_WhenRequestContextIsSet() {
        try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("user", "tenant"))) {
            assertThat(RequestContextHolder.getCurrent()).isEqualTo(new RequestContext("user", "tenant"));
        }
    }

    @Test
    public void testGetCurrent_RequestContextIsNull_WhenRequestContextIsClosed() {
        try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("user", "tenant"))) {
            assertThat(RequestContextHolder.getCurrent()).isEqualTo(new RequestContext("user", "tenant"));
        }
        assertThat(RequestContextHolder.getCurrent()).isNull();
    }

    @Test
    public void testRequestContextHolder_ThrowsIllegalStateException_WhenRequestContextIsNotValid() {
        try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("", ""))) {
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("RequestContext is not valid");
        }
        assertThat(RequestContextHolder.getCurrent()).isNull();
    }

    @Test
    public void testRequestContextHolder_ThrowsIllegalStateExceptionAndKeepsPreviousRequestContext_WhenPreviousRequestContextIsNotNull() {
        try (RequestContextHolder rc1 = new RequestContextHolder(new RequestContext("user", "tenant"))) {
            assertThat(RequestContextHolder.getCurrent()).isEqualTo(new RequestContext("user", "tenant"));
            try (RequestContextHolder rc2 = new RequestContextHolder(new RequestContext("otherUser", "otherTenant"))){
                fail();
            } catch (IllegalStateException e) {
                assertThat(e.getMessage()).contains("Previous RequestContext was not null");
            }
            assertThat(RequestContextHolder.getCurrent()).isEqualTo(new RequestContext("user", "tenant"));
        }
        assertThat(RequestContextHolder.getCurrent()).isNull();
    }

    @Test
    public void testClose_ThrowsIllegalStateException_WhenContextHasAlreadyBeenClosed() {
        try (RequestContextHolder rc1 = new RequestContextHolder(new RequestContext("user", "tenant"))) {
            rc1.close();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("RequestContext was null when closing RequestContextHolder");
        }
        assertThat(RequestContextHolder.getCurrent()).isNull();
    }
}
