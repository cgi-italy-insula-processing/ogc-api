package com.cgi.eoss.ogcapi.processes.config;

import com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper;
import com.cgi.eoss.ogcapi.processes.security.RequestContextForwardingInterceptor;
import com.cgi.eoss.ogcapi.processes.security.RequestContextInitializerFilter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource("classpath:test-application.properties")
public abstract class OgcApiProcessesAppConfigurationIT {

    @Autowired
    protected ApplicationContext applicationContext;

    public static class OcgApiProcessesAppConfigurationWithSecurityEnabledIT extends OgcApiProcessesAppConfigurationIT {

        @Test
        public void testOgcApiProcessesAppConfiguration_CreatesInsulaRestTemplateBean() {
            RestTemplate insulaRestTemplate = (RestTemplate) applicationContext.getBean("insulaRestTemplate");
            assertThat(insulaRestTemplate).isNotNull();
        }

        @Test
        public void testOgcApiProcessesAppConfiguration_CreatesInsulaJobMapperBean() {
            InsulaJobMapper insulaJobMapper = applicationContext.getBean("insulaJobMapper", InsulaJobMapper.class);
            assertThat(insulaJobMapper).isNotNull();
        }

        @Test
        public void testOgcApiProcessesAppConfiguration_CreatesCustomDeserializerModuleBean() {
            SimpleModule customDeserializerModule = (SimpleModule) applicationContext.getBean("customDeserializerModule");
            assertThat(customDeserializerModule).isNotNull();
        }

        @Test
        public void testOgcApiProcessesAppConfiguration_CreatesGuavaModuleBean() {
            GuavaModule guavaModule = (GuavaModule) applicationContext.getBean("guavaModule");
            assertThat(guavaModule).isNotNull();
        }

        @Test
        public void testOgcApiProcessesAppConfiguration_CreatesRequestContextForwardingInterceptorBean() {
            RequestContextForwardingInterceptor requestContextForwardingInterceptor = applicationContext
                    .getBean("requestContextForwardingInterceptor", RequestContextForwardingInterceptor.class);
            assertThat(requestContextForwardingInterceptor).isNotNull();
        }

        @Test
        public void testOgcApiProcessesAppConfiguration_CreatesRequestContextInitializerFilterBean() {
            RequestContextInitializerFilter requestContextInitializerFilter = applicationContext
                    .getBean("requestContextInitializerFilter", RequestContextInitializerFilter.class);
            assertThat(requestContextInitializerFilter).isNotNull();
        }
    }

    @TestPropertySource(properties = {"ogcapi.processes.security.enabled=false"})
    public static class OcgApiProcessesAppConfigurationWithoutSecurityDisabledIT extends OgcApiProcessesAppConfigurationIT {

        @Test
        public void testOgcApiProcessesAppConfiguration_DoesNotCreateRequestContextForwardingInterceptorBean() {
            assertThat(applicationContext.containsBean("requestContextForwardingInterceptor")).isFalse();
        }

        @Test
        public void testOgcApiProcessesAppConfiguration_DoesNotCreateRequestContextInitializerFilterBean() {
            assertThat(applicationContext.containsBean("requestContextInitializerFilter")).isFalse();
        }
    }

}
