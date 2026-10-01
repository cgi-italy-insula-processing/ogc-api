package com.cgi.eoss.ogcapi.processes.config;

import com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper;
import com.cgi.eoss.ogcapi.processes.model.Input;
import com.cgi.eoss.ogcapi.processes.model.OgcapppkgExecutionUnit;
import com.cgi.eoss.ogcapi.processes.security.RequestContextForwardingInterceptor;
import com.cgi.eoss.ogcapi.processes.security.RequestContextInitializerFilter;
import com.cgi.eoss.ogcapi.processes.serializers.InputDeserializer;
import com.cgi.eoss.ogcapi.processes.serializers.OgcapppkgExecutionUnitDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Duration;

/**
 * Application configuration class which initializes all the beans
 * required for the execution of the OGC API Processes application.
 */
@Configuration
@EnableConfigurationProperties({InsulaClientProperties.class, InsulaHeadersProperties.class})
public class OgcApiProcessesAppConfiguration {

    /**
     * Initializes a RestTemplate object that is leveraged to send HTTP requests to Insula platform.
     *
     * @param mappingJackson2HttpMessageConverter the default HTTP message converter used in this template
     * @param insulaClientProperties              Class storing Insula client related properties
     * @return the Insula RestTemplate object.
     */
    @Bean
    public RestTemplate insulaRestTemplate(MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter,
                                           InsulaClientProperties insulaClientProperties,
                                           @Autowired(required = false) RequestContextForwardingInterceptor requestContextForwardingInterceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(insulaClientProperties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(insulaClientProperties.getReadTimeout()))
                .uriTemplateHandler(new DefaultUriBuilderFactory(insulaClientProperties.getBaseUrl()))
                .messageConverters(mappingJackson2HttpMessageConverter)
                .build();
        if (requestContextForwardingInterceptor != null) {
            restTemplate.getInterceptors().add(requestContextForwardingInterceptor);
        }
        return restTemplate;
    }

    /**
     * Class used to create security related beans when security is enabled
     *
     */
    @Configuration
    @ConditionalOnProperty(
            name = "ogcapi.processes.security.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public static class SecurityConfiguration {

        /**
         * Initializes the interceptor that forwards the request context headers to outgoing HTTP requests.
         *
         * @param insulaHeadersProperties the configuration of the headers to forward.
         * @return the RequestContextForwardingInterceptor.
         */
        @Bean
        public RequestContextForwardingInterceptor requestContextForwardingInterceptor(InsulaHeadersProperties insulaHeadersProperties) {
            return new RequestContextForwardingInterceptor(insulaHeadersProperties);
        }

        /**
         * Initializes the filter that populates the request context from the incoming request headers.
         *
         * @param insulaHeadersProperties the configuration of the headers to read.
         * @return the RequestContextInitializerFilter.
         */
        @Bean
        @Order(1)
        public RequestContextInitializerFilter requestContextInitializerFilter(InsulaHeadersProperties insulaHeadersProperties) {
            return new RequestContextInitializerFilter(insulaHeadersProperties);
        }
    }

    /**
     * Initializes a custom Jackson Module object that is leveraged to deserialize OgcapppkgExecutionUnit objects.
     *
     * @return the custom Jackson Module.
     */
    @Bean
    public Module customDeserializerModule() {
        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addDeserializer(OgcapppkgExecutionUnit.class, new OgcapppkgExecutionUnitDeserializer());
        deserializationModule.addDeserializer(Input.class, new InputDeserializer());
        return deserializationModule;
    }

    /**
     * Initializes a {@link InsulaJobMapper} object that is leveraged to map Insula objects.
     *
     * @param isJobApiMappingEnabled enables mapping of parent and child jobs link.
     * @return {@code insulaJobMapper} bean.
     *
     */
    @Bean
    public InsulaJobMapper insulaJobMapper(
            @Value("${ogcapi.processes.insula.getSubJobsApi.enabled:true}") boolean isJobApiMappingEnabled) {
        return new InsulaJobMapper(isJobApiMappingEnabled);
    }

    /**
     * Initializes a GuavaModule that is used by the application to serialize/deserialize Guava objects.
     *
     * @return the Guava Module.
     */
    @Bean
    public GuavaModule guavaModule() {
        return new GuavaModule();
    }

}
