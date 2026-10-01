package com.cgi.eoss.ogcapi.processes.services;

import com.cgi.eoss.ogcapi.processes.controllers.ProcessesApiDelegate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-application.properties")
public class ProcessesApiDelegateImplIT {
    private static final Path INSULA_BASE_TEST_PATH = Paths.get("src", "test", "resources", "insula");

    private static final Path SERVICES_BASE_TEST_PATH = Paths.get("src", "test", "resources", "ogcapi");

    private final static String CONTEXT_PATH = "/testogcapi";

    @Autowired
    private ProcessesApiDelegate processesApiDelegate;

    @Autowired
    private MockMvc mockMvc;

    @Value("${insula.mockserver.port}")
    private Integer mockWebServerPort;

    private MockWebServer mockWebServer;

    @BeforeEach
    public void init() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(mockWebServerPort);
    }

    @AfterEach
    public void shutdown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testPOSTProcesses_ResponseStatusIsCreatedAndContainsProcessSummaryRepresentationAndContainsLocationHeader() throws Exception {
        String serviceCreationResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("service-creation-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(serviceCreationResponseBody));
        }

        String deployBody = "{\"executionUnit\": {\"href\": \"https://some.external.reference/cwl\", \"type\": \"application/cwl\", \"title\": \"cwl\"}}";
        String actualResponse = mockMvc.perform(post(CONTEXT_PATH + "/processes")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/ogcapppkg+json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(deployBody))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues("Location", "http://localhost/testogcapi/processes/92"))
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/services");

        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("deploy-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testGETPackages_ResponseStatusIsOKAndContainsOgcApppkgRepresentation() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-service-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
        }

        String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/processes/10/package")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/10");

        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-package-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsOKAndContainsStatusInfoRepresentation() throws Exception {
        String createJobConfigResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-config-creation-response.json"));
        String launchJobResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-launch-response.json"));

        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(createJobConfigResponse));

            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json"));

            mockWebServer.enqueue(new MockResponse().setResponseCode(202)
                    .setHeader("Content-Type", "application/json")
                    .setBody(launchJobResponse));
        }

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        String actualResponse = mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039/launch");
        }


        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("execute-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsOKAndContainsStatusInfoRepresentation_WhenInputContainsArray() throws Exception {
        String createJobConfigResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-config-creation-response.json"));
        String launchJobResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-launch-response.json"));

        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(createJobConfigResponse));

            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json"));

            mockWebServer.enqueue(new MockResponse().setResponseCode(202)
                    .setHeader("Content-Type", "application/json")
                    .setBody(launchJobResponse));
        }

        String executeBody = "{\"inputs\":{\"arrayInput\":[\"firstItem\", \"secondItem\"]}}";
        String actualResponse = mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"arrayInput\":[\"firstItem\",\"secondItem\"]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039/launch");
        }


        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("execute-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testDELETEProcess_ResponseStatusIsNoContent_WhenProcessIsDeleted() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-service-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
            mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        }

        mockMvc.perform(delete(CONTEXT_PATH + "/processes/10")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/10");
        }
        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/10/disable");
        }
    }

    @Test
    public void testDELETEProcess_ResponseStatusIsNotFound_WhenInsulaServiceIsDisabled() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-service-response-status-disabled.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
        }

        mockMvc.perform(delete(CONTEXT_PATH + "/processes/10")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("404 NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("https://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process"))
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.detail").value("Insula API GET /services/10 returned with status: 404 NOT_FOUND"));


        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/10");
    }

    @Test
    public void testPUTProcess_ResponseStatusIsNoContent_WhenProcessIsReplaced() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(204));
        }

        String replaceBody = "{\"executionUnit\": {\"href\": \"https://some.external.reference/cwl\", \"type\": \"application/cwl\", \"title\": \"cwl\"}}";
        mockMvc.perform(put(CONTEXT_PATH + "/processes/100")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/ogcapppkg+json")
                        .contextPath(CONTEXT_PATH)
                        .content(replaceBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/100");
    }

    @Test
    public void testPOSTProcesses_ResponseStatusIsUnsupportedMediaTypeAndContentIsOgcException_WhenRequestContentTypeIsNotOgcapppkgJson() throws Exception {
        String deployBody = "{\"executionUnit\": {\"href\": \"https://some.external.reference/cwl\", \"type\": \"application/cwl\", \"title\": \"cwl\"}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/text")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(deployBody))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.type").value("http://www.opengis.net/def/exceptions/ogcapi-processes-2/1.0/unsupported-media-type"))
                .andExpect(jsonPath("$.status").value("415"))
                .andExpect(jsonPath("$.detail").value("Content-Type 'application/text' is not supported."));
    }

    @Test
    public void testPOSTProcesses_ResponseStatusIsBadRequest_WhenRequestBodyIsMalformed() throws Exception {
        String deployBody = "{\"unknownKey\": \"unknownValue\"}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/ogcapppkg+json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(deployBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPOSTProcesses_ResponseStatusIsPropagatedFromInsulaResponse_WhenInsulaResponseContainsForbiddenStatusCode() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(403)
                    .setHeader("Content-Type", "application/json")
                    .setBody("error"));
        }

        String deployBody = "{\"executionUnit\": {\"href\": \"https://some.external.reference/cwl\", \"type\": \"application/cwl\", \"title\": \"cwl\"}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/ogcapppkg+json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(deployBody))
                .andExpect(status().isForbidden());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/services");
    }

    @Test
    public void testPOSTProcesses_ResponseStatusIsPropagatedFromInsulaResponseAndContentIsOgcException_WhenInsulaResponseContainsConflictStatusCode() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(409)
                    .setHeader("Content-Type", "application/json")
                    .setBody("Service is already deployed."));
        }

        String deployBody = "{\"executionUnit\": {\"href\": \"https://some.external.reference/cwl\", \"type\": \"application/cwl\", \"title\": \"cwl\"}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/ogcapppkg+json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(deployBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("409 CONFLICT"))
                .andExpect(jsonPath("$.type").value("http://www.opengis.net/def/exceptions/ogcapi-processes-2/1.0/duplicated-process"))
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.detail").value("Insula API POST /services returned with status: 409 CONFLICT"));

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/services");
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsNotFound_WhenCreateJobConfigResponseStatusIsNotFound() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                    .setHeader("Content-Type", "application/json"));
        }

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isNotFound());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsUnauthorized_WhenCreateJobConfigResponseStatusIsUnauthorized() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(401)
                    .setHeader("Content-Type", "application/json"));
        }

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isUnauthorized());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsPaymentRequired_WhenUserCannotLaunchJobConfig() throws Exception {
        String createJobConfigResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-config-creation-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(createJobConfigResponse));

            mockWebServer.enqueue(new MockResponse().setResponseCode(402)
                    .setHeader("Content-Type", "application/json"));

            mockWebServer.enqueue(new MockResponse().setResponseCode(204)
                    .setHeader("Content-Type", "application/json"));
        }

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isPaymentRequired());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039");
        }
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsPropagatedFromInsulaResponse_WhenUserDoesNotHaveEnoughCreditAndCannotDeleteJobConfig() throws Exception {
        String createJobConfigResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-config-creation-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(createJobConfigResponse));

            mockWebServer.enqueue(new MockResponse().setResponseCode(402)
                    .setHeader("Content-Type", "application/json"));

            mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                    .setHeader("Content-Type", "application/json"));
        }

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isNotFound());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039");
        }
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsUnauthorized_WhenUserIsNotAllowedToLaunchJob() throws Exception {
        String createJobConfigResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-config-creation-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(createJobConfigResponse));

            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json"));

            mockWebServer.enqueue(new MockResponse().setResponseCode(401)
                    .setHeader("Content-Type", "application/json"));
        }

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isUnauthorized());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039/launch");
        }
    }

    @Test
    public void testPOSTExecution_ResponseStatusIsCreatedAndDoesNotIncludeInsulaJobLink_WhenJobLaunchResponseSelfLinkIsMissing() throws Exception {
        String createJobConfigResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-config-creation-response.json"));
        String launchJobResponse = Files.readString(INSULA_BASE_TEST_PATH.resolve("job-launch-response-without-self-link.json"));

        mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(createJobConfigResponse));

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse().setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody(launchJobResponse));

        String executeBody = "{\"inputs\":{\"inputOne\":\"valueString\",\"inputTwo\":20.5}}";
        String actualResponse = mockMvc.perform(post(CONTEXT_PATH + "/processes/10/execution")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(executeBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8())
                    .isEqualTo("{\"service\":\"http://localhost:10011/services/10\",\"inputs\":{\"inputOne\":[\"valueString\"],\"inputTwo\":[20.5]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
        }

        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039/launch");
        }

        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("execute-response-without-self-link.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testPUTProcess_ResponseStatusIsForbidden_WhenServiceCannotBeReplaced() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(403));
        }

        String replaceBody = "{\"executionUnit\": {\"href\": \"https://some.external.reference/cwl\", \"type\": \"application/cwl\", \"title\": \"cwl\"}}";
        mockMvc.perform(put(CONTEXT_PATH + "/processes/100")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/ogcapppkg+json")
                        .contextPath(CONTEXT_PATH)
                        .content(replaceBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/100");
    }

    @Test
    public void testPUTProcess_ResponseStatusIsUnsupportedMediaTypeAndContentIsOgcException_WhenRequestContentTypeIsNotOgcapppkgJson() throws Exception {
        mockMvc.perform(put(CONTEXT_PATH + "/processes/100")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void testDELETEProcess_ResponseStatusIsUnauthorized_WhenUserIsNotAllowedToDeleteService() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-service-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        }

        mockMvc.perform(delete(CONTEXT_PATH + "/processes/10")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/10");
        }
        {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/10/disable");
        }
    }

    @Test
    public void testGETPackage_ResponseStatusIsUnauthorized_WhenUserIsNotAllowedToRetrieveService() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        }

        mockMvc.perform(get(CONTEXT_PATH + "/processes/100/package")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/100");
    }

    @Test
    public void testGETProcessDescription_ResponseStatusIsOkAndContainsProcessRepresentation() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-service-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
        }

        String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/processes/10")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/10");

        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-process-description-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    public void testGETProcessDescription_ResponseStatusIsNotFound_WhenServiceStatusIsDisabled() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-service-response-status-disabled.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
        }

        mockMvc.perform(get(CONTEXT_PATH + "/processes/10")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("404 NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("https://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process"))
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.detail").value("Insula API GET /services/10 returned with status: 404 NOT_FOUND"));


        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/10");

    }

    @Test
    public void testGETProcessDescription_ResponseStatusIsUnauthorized_WhenUserIsNotAllowedToRetrieveService() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        }

        mockMvc.perform(get(CONTEXT_PATH + "/processes/10")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/10");
    }

    @Test
    public void testGETProcesses_ResponseStatusIsOkAndContainsProcessListRepresentation() throws Exception {
        String getServiceResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-services-response.json"));
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getServiceResponseBody));
        }

        String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/processes?limit=2")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/search/parametricFind?size=2&status=AVAILABLE,IN_DEVELOPMENT&projection=detailedPlatformService");

        String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-processes-response.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testGETProcesses_ResponseStatusIsUnauthorized_WhenUserIsNotAllowedToRetrieveServices() throws Exception {
        {
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        }

        mockMvc.perform(get(CONTEXT_PATH + "/processes?limit=2")
                        .header("user", "user")
                        .header("tenant", "tenant")
                        .contentType("application/json")
                        .contextPath(CONTEXT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
        assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
        assertThat(recordedRequest.getPath()).isEqualTo("/services/search/parametricFind?size=2&status=AVAILABLE,IN_DEVELOPMENT&projection=detailedPlatformService");
    }
}
