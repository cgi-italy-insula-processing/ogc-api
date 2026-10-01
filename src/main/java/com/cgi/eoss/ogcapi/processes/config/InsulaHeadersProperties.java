package com.cgi.eoss.ogcapi.processes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties class storing all the configuration properties related to
 * the Insula headers names used when sending requests to Insula platform.
 */
@Data
@ConfigurationProperties("ogcapi.processes.insula.headers")
public class InsulaHeadersProperties {

    private String user = "user";

    private String tenant = "tenant";
}
