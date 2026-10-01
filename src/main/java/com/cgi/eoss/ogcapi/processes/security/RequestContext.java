package com.cgi.eoss.ogcapi.processes.security;
import org.springframework.util.StringUtils;

/**
 * Immutable record holding the user and tenant information for a specific HTTP request
 * targeting the Insula platform.
 * @param user the Insula User.
 * @param tenant the Insula tenant.
 */
public record RequestContext(String user, String tenant) {

    /**
     * Checks that both user and tenant parameters are actually populated.
     * @return true if both parameters are neither empty nor null, false otherwise.
     */
    public boolean isValid() {
        return StringUtils.hasText(user) && StringUtils.hasText(tenant);
    }
}
