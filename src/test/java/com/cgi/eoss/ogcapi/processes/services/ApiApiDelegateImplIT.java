package com.cgi.eoss.ogcapi.processes.services;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-application.properties")
public abstract class ApiApiDelegateImplIT {

    protected final static String CONTEXT_PATH = "/testogcapi";

    @Autowired
    protected MockMvc mockMvc;

    public static class ApiApiDelegateImplSecurityEnabledIT extends ApiApiDelegateImplIT {
        @Test
        public void testGetApi_ResponseStatusIsFoundAndRedirectsToSwagger() throws Exception {
            String redirectUrl = mockMvc.perform(get(CONTEXT_PATH + "/api")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contextPath(CONTEXT_PATH))
                    .andExpect(status().isFound())
                    .andReturn().getResponse().getHeader("Location");

            String swaggerExpectedPath = CONTEXT_PATH + "/api/swagger-ui/index.html";
            String expectedRedirectUrl = "http://localhost" + swaggerExpectedPath;
            assertThat(redirectUrl).isEqualTo(expectedRedirectUrl);

            String swaggerIndexContent = mockMvc.perform(get(swaggerExpectedPath)
                            .header("user", "user")
                            .header("tenant", "tenant"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String expectedSwaggerIndexContent = Files.readString(Paths.get("src", "test", "resources", "swagger", "index.html"));

            assertThat(swaggerIndexContent).isEqualToIgnoringWhitespace(expectedSwaggerIndexContent);
        }

        @Test
        public void testGetApiDocs_ResponseStatusIsOkAndBodyContainsSwaggerContent() throws Exception {
            String apiDocsPath = CONTEXT_PATH + "/api/v3/api-docs";
            String actualApiDocsContent = mockMvc.perform(get(apiDocsPath)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contextPath(CONTEXT_PATH))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String expectedApiDocsContent = Files.readString(Paths.get("src", "test", "resources", "swagger", "api-docs.json"));

            JSONAssert.assertEquals(expectedApiDocsContent, actualApiDocsContent, JSONCompareMode.STRICT);
        }

    }

    @TestPropertySource(properties = {
            "ogcapi.processes.security.enabled=false"
    })
    public static class ApiApiDelegateImplSecurityDisabledIT extends ApiApiDelegateImplIT {
        @Test
        public void testGetApi_ResponseStatusIsFoundAndRedirectsToSwagger_WhenSecurityHeadersFiltersAreDisabled() throws Exception {
            String redirectUrl = mockMvc.perform(get(CONTEXT_PATH + "/api")
                            .contextPath(CONTEXT_PATH))
                    .andExpect(status().isFound())
                    .andExpect(header().doesNotExist("user"))
                    .andExpect(header().doesNotExist("tenant"))
                    .andReturn().getResponse().getHeader("Location");

            String swaggerExpectedPath = CONTEXT_PATH + "/api/swagger-ui/index.html";
            String expectedRedirectUrl = "http://localhost" + swaggerExpectedPath;
            assertThat(redirectUrl).isEqualTo(expectedRedirectUrl);

            String swaggerIndexContent = mockMvc.perform(get(swaggerExpectedPath))
                    .andExpect(header().doesNotExist("user"))
                    .andExpect(header().doesNotExist("tenant"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String expectedSwaggerIndexContent = Files.readString(Paths.get("src", "test", "resources", "swagger", "index.html"));

            assertThat(swaggerIndexContent).isEqualToIgnoringWhitespace(expectedSwaggerIndexContent);
        }
    }

}
