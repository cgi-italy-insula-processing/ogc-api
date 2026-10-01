package com.cgi.eoss.ogcapi.processes.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties(InsulaHeadersProperties.class)
public abstract class InsulaHeadersPropertiesIT {

    @Autowired
    protected InsulaHeadersProperties insulaHeadersProperties;

    public static class InsulaHeadersPropertiesDefaultValuesIT extends InsulaHeadersPropertiesIT {

        @Test
        public void testInsulaClientPropertiesDefaultValues() {
            assertThat(insulaHeadersProperties.getUser()).isEqualTo("user");
            assertThat(insulaHeadersProperties.getTenant()).isEqualTo("tenant");
        }

    }

    @TestPropertySource(properties = {
            "ogcapi.processes.insula.headers.user=eosso-person",
            "ogcapi.processes.insula.headers.tenant=eosso-tenant",
    })
    public static class InsulaHeadersPropertiesCustomValuesIT extends InsulaHeadersPropertiesIT {

        @Test
        public void testInsulaClientPropertiesCustomValues() {
            assertThat(insulaHeadersProperties.getUser()).isEqualTo("eosso-person");
            assertThat(insulaHeadersProperties.getTenant()).isEqualTo("eosso-tenant");
        }

    }
}
