package com.cgi.eoss.ogcapi.processes.services;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-application.properties")
public class DefaultApiDelegateImplIT {

    private static final Path SERVICES_BASE_TEST_PATH = Paths.get("src", "test", "resources", "ogcapi");

    private final static String CONTEXT_PATH = "/testogcapi";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetLandingPage_ResponseStatusIsOkAndBodyContainsLandingPageJsonRepresentation_WhenFormatRequiresJsonResponse() throws Exception {
        String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/?f=json")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .accept(MediaType.APPLICATION_JSON)
                        .contextPath(CONTEXT_PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-landing-page-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testGetLandingPage_ResponseStatusIsNotAcceptable_WhenFormatIsNotJson() throws Exception {
        mockMvc.perform(get(CONTEXT_PATH + "/?f=notJson")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .accept(MediaType.APPLICATION_JSON)
                        .contextPath(CONTEXT_PATH))
                .andExpect(status().isNotAcceptable())
                .andReturn();
    }
}
