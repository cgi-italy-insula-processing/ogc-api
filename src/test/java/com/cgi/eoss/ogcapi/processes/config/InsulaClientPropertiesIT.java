package com.cgi.eoss.ogcapi.processes.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties(InsulaClientProperties.class)
public abstract class InsulaClientPropertiesIT {

    @Autowired
    protected InsulaClientProperties insulaClientProperties;

    public static class InsulaClientPropertiesDefaultValuesIT extends InsulaClientPropertiesIT {

        @Test
        public void testInsulaClientPropertiesDefaultValues() {
            assertThat(insulaClientProperties.getBaseUrl()).isEqualTo("http://localhost:8082");
            assertThat(insulaClientProperties.getConnectTimeout()).isEqualTo(600L);
            assertThat(insulaClientProperties.getReadTimeout()).isEqualTo(1200L);
        }

    }

    @TestPropertySource(properties = {
            "ogcapi.processes.insula.client.baseUrl=http://insula.url",
            "ogcapi.processes.insula.client.connectTimeout=300",
            "ogcapi.processes.insula.client.readTimeout=600"
    })
    public static class InsulaClientPropertiesCustomValuesIT extends InsulaClientPropertiesIT {

        @Test
        public void testInsulaClientPropertiesCustomValues() {
            assertThat(insulaClientProperties.getBaseUrl()).isEqualTo("http://insula.url");
            assertThat(insulaClientProperties.getConnectTimeout()).isEqualTo(300L);
            assertThat(insulaClientProperties.getReadTimeout()).isEqualTo(600L);
        }

    }
}
