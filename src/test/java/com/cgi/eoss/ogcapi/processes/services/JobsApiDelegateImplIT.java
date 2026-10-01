package com.cgi.eoss.ogcapi.processes.services;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-application.properties")
public abstract class JobsApiDelegateImplIT {

    protected static final Path INSULA_BASE_TEST_PATH = Paths.get("src", "test", "resources", "insula");

    protected static final Path SERVICES_BASE_TEST_PATH = Paths.get("src", "test", "resources", "ogcapi");

    protected final static String CONTEXT_PATH = "/testogcapi";

    protected static final CustomComparator GET_JOB_RESPONSE_CUSTOM_COMPARATOR = new CustomComparator(
            JSONCompareMode.STRICT,
            regexMatcher("message"),
            regexMatcher("status"),
            regexMatcher("progress"),
            regexMatcher("links[0].title")
    );

    @Autowired
    protected MockMvc mockMvc;

    @Value("${insula.mockserver.port}")
    protected Integer mockWebServerPort;

    protected MockWebServer mockWebServer;

    @BeforeEach
    public void init() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(mockWebServerPort);
    }

    @AfterEach
    public void shutdown() throws Exception {
        mockWebServer.shutdown();
    }

    @TestPropertySource(properties = {
            "ogcapi.processes.insula.getSubJobsApi.enabled=false"
    })
    public static class JobsApiDelegateImplWithApiJobMappingDisabledIT extends JobsApiDelegateImplIT {

        @Test
        public void testGetStatus_ResponseStatusIsOkAndContainsStatusInfoRepresentation() throws Exception {
            String jobId = "4421";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response-with-parent-child.json"));
            String getJobConfigResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-config-response.json"));

            {
                // Get job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));
                // Get job config request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobConfigResponseBody));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs/" + jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-job-response-with-metadata-without-parent-child.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, GET_JOB_RESPONSE_CUSTOM_COMPARATOR);

            DocumentContext actualJsonResults = JsonPath.parse(actualResponse);
            assertThat(actualJsonResults.<String>read("message")).isEqualTo("Subjobs: RUNNING: 1 - SUCCESSFUL: 2 - FAILED: 1");
            assertThat(actualJsonResults.<String>read("status")).isEqualTo("accepted");
            assertThat(actualJsonResults.<Integer>read("progress")).isEqualTo(25);

            assertThat(actualJsonResults.<String>read("$.metadata[0].title")).isEqualTo("dockerTag");
            assertThat(actualJsonResults.<String>read("$.metadata[0].role")).isEqualTo("runtime");
            assertThat(actualJsonResults.<String>read("$.metadata[0].value")).isEqualTo("eopaas/sardem-sarsen-process:8f50d01c");

            assertThat(actualJsonResults.<List<Object>>read("$.links")).hasSize(1);
            assertThat(actualJsonResults.<String>read("links[0].title")).isEqualTo("Job Status");
            assertThat(actualJsonResults.<String>read("$.links[0].rel")).isEqualTo("self");

        }
    }

    public static class JobsApiDelegateImplWithApiJobMappingEnabledIT extends JobsApiDelegateImplIT {

        @Test
        public void testGetStatus_ResponseStatusIsOkAndContainsStatusInfoRepresentation() throws Exception {
            String jobId = "4421";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response-with-parent-child.json"));
            String getJobConfigResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-config-response.json"));

            {
                // Get job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));
                // Get job config request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobConfigResponseBody));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs/" + jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-job-response-with-metadata-and-parent-child.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, GET_JOB_RESPONSE_CUSTOM_COMPARATOR);

            DocumentContext actualJsonResults = JsonPath.parse(actualResponse);
            assertThat(actualJsonResults.<String>read("message")).isEqualTo("Subjobs: RUNNING: 1 - SUCCESSFUL: 2 - FAILED: 1");
            assertThat(actualJsonResults.<String>read("status")).isEqualTo("accepted");
            assertThat(actualJsonResults.<Integer>read("progress")).isEqualTo(25);

            assertThat(actualJsonResults.<String>read("$.metadata[0].title")).isEqualTo("dockerTag");
            assertThat(actualJsonResults.<String>read("$.metadata[0].role")).isEqualTo("runtime");
            assertThat(actualJsonResults.<String>read("$.metadata[0].value")).isEqualTo("eopaas/sardem-sarsen-process:8f50d01c");

            assertThat(actualJsonResults.<List<Object>>read("$.links")).hasSize(3);
            assertThat(actualJsonResults.<String>read("$.links[0].title")).isEqualTo("Job Status");
            assertThat(actualJsonResults.<String>read("$.links[1].rel")).isEqualTo("insula-parent-job");
            assertThat(actualJsonResults.<String>read("$.links[2].rel")).isEqualTo("insula-child-jobs");
        }

        @Test
        public void testGetJobs_ResponseStatusIsOkAndContainsJobListRepresentation() throws Exception {
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .param("limit", "1")
                            .param("offset", "0")
                            .param("processID", "process")
                            .param("status", "accepted")
                            .param("datetime", "2025-03-12T14:56:09")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob" +
                        "&size=1&sort=id&page=0&filter=process&status=CREATED&startTime=2025-03-12T14:56:09");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-jobs-response.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
        }

        @Test
        public void testGetJobs_ResponseStatusIsOk_WhenOnlyRequiredParamsPresent() throws Exception {
            String insulaResponseAsString = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(insulaResponseAsString));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .param("limit", "1")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob" +
                        "&size=1&sort=id&page=0");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-jobs-response-without-processId-and-status.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
        }


        @Test
        public void testGetJobs_ResponseStatusIsOkAndContainsEmptyJobArray_WhenInsulaSeverGetJobsRespondsEmptyResponse() throws Exception {
            String insulaResponseAsString = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-empty-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(insulaResponseAsString));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .param("limit", "1")
                            .param("offset", "0")
                            .param("processID", "process")
                            .param("status", "accepted")
                            .param("datetime", "2025-03-12T14:56:09")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob" +
                        "&size=1&sort=id&page=0&filter=process&status=CREATED&startTime=2025-03-12T14:56:09");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-jobs-response-with-no-records.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
        }

        @Test
        public void testGetJobs_ResponseStatusIsOkAndContainsJobListRepresentation_WhenInsulaGetJobsIsCalledTwice() throws Exception {
            String insulaFirstJobResponseAsString  = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-response-for-request-one.json"));
            String insulaSecondJobResponseAsString  = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-response-for-request-two.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(insulaFirstJobResponseAsString ));
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(insulaSecondJobResponseAsString));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .param("limit", "5")
                            .param("offset", "2")
                            .param("processID", "process")
                            .param("status", "accepted")
                            .param("datetime", "2025-03-12T14:56:09")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob" +
                        "&size=5&sort=id&page=0&filter=process&status=CREATED&startTime=2025-03-12T14:56:09");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob" +
                        "&size=5&sort=id&page=1&filter=process&status=CREATED&startTime=2025-03-12T14:56:09");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-jobs-response-with-offset.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
        }

        @Test
        public void testGetResult_ResponseStatusIsOkAndContainsOutputResultsRepresentation() throws Exception {
            String jobId = "4421";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-job-result-response.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
        }

        @Test
        public void testGetOutputResult_ResponseStatusIsOkAndContainsOutputResultsRepresentation() throws Exception {
            String jobId = "4421";
            String outputId = "outputOne";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results/"+outputId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-job-output-result-response.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
        }

        @Test
        public void testGetOutputResult_ResponseStatusIsOkAndContainsStacSearchResponseRepresentation_WhenAcceptHeaderContainsGeoJsonValue() throws Exception {
            String jobId = "4421";
            String outputId = "outputOne";
            String getJobResponseAsString = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response.json"));
            String stacSearchResponseAsString = Files.readString(INSULA_BASE_TEST_PATH.resolve("stac-search-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseAsString));
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(stacSearchResponseAsString));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results/"+outputId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept("application/geo+json"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath())
                        .isEqualTo("/search?catalogue=PLATFORM_PRODUCTS&collection=eopaasb469865a5b47409da16cd843f2123b6a" +
                                "&identifier=c7d6fedd-5e2d-4a31-b359-95c69e16ca92_outputOne");
            }

            JSONAssert.assertEquals(stacSearchResponseAsString, actualResponse, JSONCompareMode.STRICT);
        }

        @Test
        public void testDismiss_SendsCancelRequest_WhenJobHasNotStartedYet() throws Exception {
            String jobId = "4421";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response.json"));

            {
                // Get job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));

                // Cancel job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            }

            String actualResponse = mockMvc.perform(delete(CONTEXT_PATH + "/jobs/"+jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "/cancel");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-job-response.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, GET_JOB_RESPONSE_CUSTOM_COMPARATOR);

            DocumentContext actualJsonResults = JsonPath.parse(actualResponse);
            assertThat(actualJsonResults.<String>read("message")).isEqualTo("Job dismission requested");
            assertThat(actualJsonResults.<String>read("status")).isEqualTo("accepted");
            assertThat(actualJsonResults.<Integer>read("progress")).isEqualTo(25);
            assertThat(actualJsonResults.<String>read("links[0].title")).isEqualTo("Dismiss Job");
        }

        @Test
        public void testDismiss_SendsTerminateRequest_WhenJobHasStarted() throws Exception {
            String jobId = "4421";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-running-detailed-response.json"));

            {
                // Get job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));

                // Cancel job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            }

            String actualResponse = mockMvc.perform(delete(CONTEXT_PATH + "/jobs/"+jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "/terminate");
            }

            String expectedResponse = Files.readString(SERVICES_BASE_TEST_PATH.resolve("get-job-response.json"));
            JSONAssert.assertEquals(expectedResponse, actualResponse, GET_JOB_RESPONSE_CUSTOM_COMPARATOR);

            DocumentContext actualJsonResults = JsonPath.parse(actualResponse);
            assertThat(actualJsonResults.<String>read("message")).isEqualTo("Job dismission requested");
            assertThat(actualJsonResults.<String>read("status")).isEqualTo("running");
            assertThat(actualJsonResults.<Integer>read("progress")).isEqualTo(75);
            assertThat(actualJsonResults.<String>read("links[0].title")).isEqualTo("Dismiss Job");
        }

        @Test
        public void testDismiss_ResponseStatusIsInternalServerError_WhenJobIdIsNotANumber() throws Exception {
            String jobId = "notANumber";

            mockMvc.perform(delete(CONTEXT_PATH + "/jobs/"+jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.type").value("N/A"))
                    .andExpect(jsonPath("$.status").value("500"))
                    .andExpect(jsonPath("$.detail").value("ID " + jobId + " is not a valid number"));
        }

        @Test
        public void testGetStatus_ResponseStatusIsInternalServerError_WhenJobIdIsNotANumber() throws Exception {
            String jobId = "notANumber";

            mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.type").value("N/A"))
                    .andExpect(jsonPath("$.status").value("500"))
                    .andExpect(jsonPath("$.detail").value("ID " + jobId + " is not a valid number"));
        }

        @Test
        public void testGetResult_ResponseStatusIsInternalServerError_WhenJobIdIsNotANumber() throws Exception {
            String jobId = "notANumber";

            mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.type").value("N/A"))
                    .andExpect(jsonPath("$.status").value("500"))
                    .andExpect(jsonPath("$.detail").value("ID " + jobId + " is not a valid number"));
        }

        @Test
        public void testGetOutputResult_ResponseStatusIsInternalServerError_WhenJobIdIsNotANumber() throws Exception {
            String jobId = "notANumber";

            mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results/outputId")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.type").value("N/A"))
                    .andExpect(jsonPath("$.status").value("500"))
                    .andExpect(jsonPath("$.detail").value("ID " + jobId + " is not a valid number"));
        }

        @Test
        public void testDismiss_ResponseStatusIsPropagated_WhenJobIsNotFound() throws Exception {
            String jobId = "4421";

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            }

            mockMvc.perform(delete(CONTEXT_PATH + "/jobs/"+jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
        }

        @Test
        public void testGetStatus_ResponseStatusIsPropagated_WhenUserCannotRetrieveJob() throws Exception {
            String jobId = "4421";

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(403)
                        .setHeader("Content-Type", "application/json")
                        .setBody("error"));
            }

            mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
        }

        @Test
        public void testGetStatus_ResponseIsOkAndMetadataIsEmpty_WhenJobConfigCannotBeRetrieved() throws Exception {
            String jobId = "4421";
            String getJobResponseBody = Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response-with-parent-child.json"));

            {
                // Get job request
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));

                // Get job config request fails
                mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"));
            }

            String actualResponse = mockMvc.perform(get(CONTEXT_PATH + "/jobs/" + jobId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(2);

                RecordedRequest firstRequest = mockWebServer.takeRequest();
                assertThat(firstRequest.getMethod()).isEqualTo("GET");
                assertThat(firstRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");

                RecordedRequest secondRequest = mockWebServer.takeRequest();
                assertThat(secondRequest.getMethod()).isEqualTo("GET");
                assertThat(secondRequest.getPath()).isEqualTo("/jobs/" + jobId + "/config");
            }

            DocumentContext actualJsonResults = JsonPath.parse(actualResponse);

            assertThat(actualJsonResults.<java.util.List<Object>>read("$.metadata")).isEmpty();
            assertThat(actualJsonResults.<String>read("$.id")).isEqualTo(jobId);
        }

        @Test
        public void testGetJobs_ResponseStatusIsNotImplemented_WhenRequestContainsUnsupportedParameters() throws Exception {
            mockMvc.perform(get(CONTEXT_PATH + "/jobs")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .param("limit", "10")
                            .param("processID", "process")
                            .param("status", "accepted")
                            .param("minDuration", "10")
                            .param("maxDuration", "99")
                            .param("type", "type")
                            .param("datetime", "2025-03-12T14:56:09")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotImplemented());
        }

        @Test
        public void testGetJobs_ResponseStatus_WhenNoJobIsFound() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody("error"));
            }

            mockMvc.perform(get(CONTEXT_PATH + "/jobs")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .param("limit", "10")
                            .param("processID", "process")
                            .param("status", "accepted")
                            .param("datetime", "2025-03-12T14:56:09")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob" +
                        "&size=10&sort=id&page=0&filter=process&status=CREATED&startTime=2025-03-12T14:56:09");
            }
        }

        @Test
        public void testGetResult_ResponseStatusIsPropagated_WhenUserCannotRetrieveJobOutputs() throws Exception {
            String jobId = "4421";

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(403)
                        .setHeader("Content-Type", "application/json")
                        .setBody("error"));
            }

            mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results")
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
        }

        @Test
        public void testGetOutputResult_ResponseStatusIsPropagated_WhenUserCannotRetrieveJobOutput() throws Exception {
            String jobId = "4421";
            String outputId = "out";

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(403)
                        .setHeader("Content-Type", "application/json")
                        .setBody("error"));
            }

            mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results/"+outputId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse().getContentAsString();

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
        }
    }

    @TestPropertySource(properties = {
            "ogcapi.processes.insula.searchApi.enabled=false"
    })
    public static class JobsApiDelegateSearchDisabledIT extends JobsApiDelegateImplIT {

        @Test
        public void testGetOutputResult_ResponseStatusIsOkAndContainsStacSearchResponseRepresentation_WhenAcceptHeaderContainsGeoJsonValue() throws Exception {

            String jobId = "4421";
            String outputId = "outputOne";
            String stacSearchResponseAsString = Files.readString(INSULA_BASE_TEST_PATH.resolve("stac-search-response.json"));

            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response.json"))));
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(stacSearchResponseAsString));
            }

            JSONAssert.assertEquals(
                    stacSearchResponseAsString,
                    mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results/"+outputId)
                                    .header("user", "user")
                                    .header("tenant", "tenant")
                                    .contentType("application/json")
                                    .contextPath(CONTEXT_PATH)
                                    .accept("application/geo+json"))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString(),
                    JSONCompareMode.STRICT);

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/"+jobId+"/outputs/"+outputId);
            }
        }

        @Test
        public void testGetOutputResult_ResponseStatusIsPropagated_WhenAcceptHeaderContainsGeoJsonValueAndUserCannotRetrieveJobOutput() throws Exception {

            String jobId = "4421";
            String outputId = "outputOne";
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(Files.readString(INSULA_BASE_TEST_PATH.resolve("get-job-detailed-response.json"))));
                mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            }

            assertThat(mockMvc.perform(get(CONTEXT_PATH + "/jobs/"+jobId+"/results/"+outputId)
                            .header("user", "user")
                            .header("tenant", "tenant")
                            .contentType("application/json")
                            .contextPath(CONTEXT_PATH)
                            .accept("application/geo+json"))
                    .andExpect(status().isInternalServerError())
                    .andReturn().getResponse().getContentAsString()
            ).isEqualTo(
                    "{\"type\":\"N/A\",\"title\":\"500 INTERNAL_SERVER_ERROR\",\"status\":500,\"detail\":\"Insula API GET /jobs/{jobId}/outputs/{outputId} returned with status: 500 INTERNAL_SERVER_ERROR\",\"instance\":null}"
            );

            {
                assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/" + jobId + "?projection=detailedJob");
            }
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("user");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobs/"+jobId+"/outputs/"+outputId);
            }
        }

    }

    protected static Customization regexMatcher(String jsonPath) {
        return new Customization(jsonPath, new RegularExpressionValueMatcher<>());
    }

}
