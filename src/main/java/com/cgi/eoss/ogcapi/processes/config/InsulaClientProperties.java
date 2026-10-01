package com.cgi.eoss.ogcapi.processes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties class storing all the configuration properties related to
 * the Insula client connection.
 */
@Data
@ConfigurationProperties("ogcapi.processes.insula.client")
public class InsulaClientProperties {

    private String baseUrl = "http://localhost:8082";

    private Long connectTimeout = 600L;

    private Long readTimeout = 1200L;
}
