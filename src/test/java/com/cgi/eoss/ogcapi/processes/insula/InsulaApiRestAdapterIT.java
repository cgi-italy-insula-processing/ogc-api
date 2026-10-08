package com.cgi.eoss.ogcapi.processes.insula;

import com.cgi.eoss.ogcapi.processes.config.InsulaClientProperties;
import com.cgi.eoss.ogcapi.processes.insula.exception.InsulaApiException;
import com.cgi.eoss.ogcapi.processes.insula.exception.ServiceNotFoundException;
import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.cgi.eoss.ogcapi.processes.insula.model.OutputFile;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobConfigCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobLaunchRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.request.ServiceCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.*;
import com.cgi.eoss.ogcapi.processes.insula.model.ServiceDescriptor;
import com.cgi.eoss.ogcapi.processes.security.RequestContext;
import com.cgi.eoss.ogcapi.processes.security.RequestContextHolder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.Link;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@TestPropertySource(value = "classpath:test-application.properties")
public abstract class InsulaApiRestAdapterIT {

    private static final Path BASE_TEST_PATH = Paths.get("src", "test", "resources", "insula");

    @Autowired
    protected InsulaApiRestAdapter insulaApiRestAdapter;

    @Autowired
    protected InsulaClientProperties insulaClientProperties;

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

    public static class InsulaApiRestAdapterReachableServiceIT extends InsulaApiRestAdapterIT {

        @Test
        public void testCreate_ReturnsServiceResponseRepresentation() throws Exception {
            String serviceCreationResponseBody = Files.readString(BASE_TEST_PATH.resolve("service-creation-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(serviceCreationResponseBody));
            }

            ServiceResponse serviceResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                serviceResponse = insulaApiRestAdapter.create(ServiceCreationRequest.builder()
                        .cwl(Cwl.builder().url("https://some.external.reference/cwl").build()).build());
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/services");

            // Assert response is as expected
            assertThat(serviceResponse.getName()).isEqualTo("serviceByCwl");
            assertThat(serviceResponse.getDescription()).isEqualTo("A Service created from a CWL.");
            assertThat(serviceResponse.getCwl()).isEqualTo(Cwl.builder().url("https://some.external.reference/cwl")
                    .document("document").build());
            assertThat(serviceResponse.getServiceDescriptor()).isEqualTo(ServiceDescriptor.builder()
                    .title("titleOfTheServiceByCwl")
                    .version("1.0")
                    .dataInputs(Collections.emptyList())
                    .dataOutputs(Collections.emptyList())
                    .build());
            assertThat(serviceResponse.getLinks()).isEqualTo(Map.of(
                    "self", Link.of("https://processing.example.com/secure/api/v2.0/services/92", "self")
            ));
        }

        @Test
        public void testGetService_ReturnsServiceResponseRepresentation() throws Exception {
            String getServiceResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-service-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getServiceResponseBody));
            }

            ServiceResponse serviceResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                serviceResponse = insulaApiRestAdapter.getService(100L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/100");

            // Assert response is as expected
            assertThat(serviceResponse.getName()).isEqualTo("sardem-sarsen");
            assertThat(serviceResponse.getDescription())
                    .isEqualTo("This application is designed to process Synthetic Aperture Radar (SAR) data from " +
                            "Sentinel-1 GRD (Ground Range Detected) products using a Digital Elevation Model (DEM) " +
                            "obtained from Copernicus.");
            assertThat(serviceResponse.getCwl()).isEqualTo(Cwl.builder()
                    .url("https://raw.githubusercontent.com/MAAP-Project/sardem-sarsen/refs/heads/esa-ogc/nasa/ogc/workflows/process_sardem-sarsen_mlucas_nasa_ogc.cwl")
                    .document("document")
                    .build());
            ServiceDescriptor serviceDescriptor = serviceResponse.getServiceDescriptor();
            assertThat(serviceDescriptor.getTitle()).isEqualTo("sardem-sarsen");
            assertThat(serviceDescriptor.getVersion()).isEqualTo("N/A");
            assertThat(serviceDescriptor.getDataInputs()).containsExactlyInAnyOrder(
                    ServiceDescriptor.InputOutputParameter.builder()
                            .id("bbox")
                            .defaultAttrs(ImmutableMap.of("dataType", "string"))
                            .platformMetadata(ImmutableMap.of("preventUrlDownload", "true", "format", "OTHER"))
                            .minOccurs(1)
                            .maxOccurs(1)
                            .build(),
                    ServiceDescriptor.InputOutputParameter.builder()
                            .id("stac_catalog_folder")
                            .defaultAttrs(ImmutableMap.of("dataType", "string"))
                            .platformMetadata(ImmutableMap.of("preventUrlDownload", "false", "format", "CATALOGUE", "type", "STAC"))
                            .title("catalog folder")
                            .description("stac catalog folder")
                            .minOccurs(1)
                            .maxOccurs(1)
                            .build(),
                    ServiceDescriptor.InputOutputParameter.builder()
                            .id("stac_asset_name")
                            .defaultAttrs(ImmutableMap.of("dataType", "string", "value", "PRODUCT"))
                            .platformMetadata(ImmutableMap.of("preventUrlDownload", "true", "format", "OTHER"))
                            .title("asset name")
                            .description("stac asset name")
                            .minOccurs(0)
                            .maxOccurs(1)
                            .build()
            );
            assertThat(serviceDescriptor.getDataOutputs()).containsExactlyInAnyOrder(
                    ServiceDescriptor.InputOutputParameter.builder()
                            .id("outputs_result")
                            .defaultAttrs(ImmutableMap.of("dataType", "string"))
                            .platformMetadata(ImmutableMap.of("preventUrlDownload", "false", "format", "CATALOGUE", "type", "STAC"))
                            .title("output")
                            .description("outputs result")
                            .minOccurs(1)
                            .maxOccurs(1)
                            .build()
            );
            assertThat(serviceResponse.getLinks()).isEqualTo(
                    Map.of(
                    "self", Link.of("https://processing.example.com/secure/api/v2.0/services/147", "self")
                    )
            );
        }

        @Test
        public void testGetService_ThrowsServiceNotFoundException_WhenGetServiceReturnsServiceWithStatusDisabled() throws Exception {
            String getServiceResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-service-response-status-disabled.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getServiceResponseBody));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.getService(100L);
                fail();
            } catch (ServiceNotFoundException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /services/100 returned with status: 404 NOT_FOUND");
            }
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/100");

        }

        @Test
        public void testGetServices_ReturnsGetServicesResponseRepresentation() throws Exception {
            String getServicesResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-services-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getServicesResponseBody));
            }

            GetServicesResponse getServicesResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                getServicesResponse = insulaApiRestAdapter.getServices(2);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/search/parametricFind?size=2&status=AVAILABLE,IN_DEVELOPMENT&projection=detailedPlatformService");

            // Assert response is as expected
            List<ServiceResponse> servicesList = getServicesResponse.getEmbeddedServices().getServices();
            assertThat(servicesList).hasSize(2);

            ServiceResponse firstService = servicesList.get(0);
            assertThat(firstService.getId()).isEqualTo(70);
            assertThat(firstService.getName()).isEqualTo("service-name-1");
            assertThat(firstService.getDescription()).isNull();
            assertThat(firstService.getCwl()).isNull();
            assertThat(firstService.getServiceDescriptor()).isEqualTo(ServiceDescriptor.builder()
                    .title("service-title-1")
                    .dataInputs(List.of(
                            ServiceDescriptor.InputOutputParameter.builder()
                                    .id("in-service-1")
                                    .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                    .platformMetadata(ImmutableMap.of("format", "CATALOGUE"))
                                    .title("in-service-title-1")
                                    .description("in-service-1 description")
                                    .minOccurs(1)
                                    .maxOccurs(1)
                                    .build()
                    ))
                    .dataOutputs(List.of(
                            ServiceDescriptor.InputOutputParameter.builder()
                                    .id("out-service-1")
                                    .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                    .platformMetadata(ImmutableMap.of("format", "OTHER", "type", "OTHER"))
                                    .title("out-service-title-1")
                                    .description("out-service-1 description")
                                    .minOccurs(1)
                                    .maxOccurs(1)
                                    .build()
                    ))
                    .build());

            ServiceResponse secondService = servicesList.get(1);
            assertThat(secondService.getId()).isEqualTo(7);
            assertThat(secondService.getName()).isEqualTo("service-name-2");
            assertThat(secondService.getDescription()).isEqualTo("");
            assertThat(secondService.getCwl()).isEqualTo(
                    Cwl.builder()
                            .url("https://url.to/app.cwl")
                            .document("document content")
                            .build()
            );
            assertThat(secondService.getServiceDescriptor()).isEqualTo(ServiceDescriptor.builder()
                    .title("service-title-2")
                    .version("0.1")
                    .dataInputs(List.of(
                            ServiceDescriptor.InputOutputParameter.builder()
                                    .id("in-service-2")
                                    .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                    .title("in-service-title-2")
                                    .description("in-service-2 description")
                                    .minOccurs(0)
                                    .maxOccurs(0)
                                    .build()
                    ))
                    .dataOutputs(List.of(
                            ServiceDescriptor.InputOutputParameter.builder()
                                    .id("out-service-2")
                                    .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                    .platformMetadata(ImmutableMap.of("format", "GEOTIFF"))
                                    .title("out-service-title-2")
                                    .description("out-service-2 description")
                                    .minOccurs(0)
                                    .maxOccurs(0)
                                    .build()
                    ))
                    .build());
        }

        @Test
        public void testCreate_ReturnsJobConfigCreationResponseRepresentation_WhenUserCanLaunchJobConfig() throws Exception {
            String jobConfigCreationResponseBody = Files.readString(BASE_TEST_PATH.resolve("job-config-creation-response.json"));
            {
                // POST Job Config Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(jobConfigCreationResponseBody));

                // EstimateCost Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"));
            }

            JobConfigCreationResponse jobConfigCreationResponse;
            Multimap<String, Object> inputs = ArrayListMultimap.create();
            inputs.put("inputOne", "eopaas://in1");
            inputs.put("inputTwo", "eopaas://in2");
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobConfigCreationResponse = insulaApiRestAdapter.create(JobConfigCreationRequest.builder()
                                .service(URI.create("https://insula/services/1"))
                                .inputs(inputs)
                        .build());
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"https://insula/services/1\"," +
                        "\"inputs\":{" +
                        "\"inputOne\":[\"eopaas://in1\"]," +
                        "\"inputTwo\":[\"eopaas://in2\"]}}");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
            }

            // Assert response is as expected
            assertThat(jobConfigCreationResponse.getId()).isEqualTo(3039);
        }

        @Test
        public void testLaunch_ReturnsJobLaunchResponseRepresentation() throws Exception {
            String jobLaunchResponseBody = Files.readString(BASE_TEST_PATH.resolve("job-launch-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(202)
                        .setHeader("Content-Type", "application/json")
                        .setBody(jobLaunchResponseBody));
            }

            JobLaunchResponse jobLaunchResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobLaunchResponse = insulaApiRestAdapter.launch(JobLaunchRequest.builder().jobConfigId(3030L).build());
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3030/launch");

            // Assert response is as expected
            assertThat(jobLaunchResponse.getId()).isEqualTo(4108);
            assertThat(jobLaunchResponse.getCreated()).isEqualTo(OffsetDateTime.parse("2025-02-06T14:51:15Z"));
            assertThat(jobLaunchResponse.getLastUpdated()).isEqualTo(OffsetDateTime.parse("2025-02-06T14:51:15Z"));
            assertThat(jobLaunchResponse.getStatus()).isEqualTo(JobLaunchResponse.Status.PENDING);
            assertThat(jobLaunchResponse.getPhase()).isEqualTo(JobLaunchResponse.Phase.CREATED);
            assertThat(jobLaunchResponse.getStartTime()).isNull();
            assertThat(jobLaunchResponse.getEndTime()).isNull();
        }

        @Test
        public void testDisableService_ReturnsOk_WhenServiceIsDisabled() throws Exception {
            String insulaResponseAsString = Files.readString(BASE_TEST_PATH.resolve("get-service-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(insulaResponseAsString));
                mockWebServer.enqueue(new MockResponse().setResponseCode(200));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.disableService(4421L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/services/4421");
            }
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/services/4421/disable");
            }
        }

        @Test
        public void testUpdate_RequestsServiceUpdate() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.update(4421L, ServiceCreationRequest.builder()
                        .cwl(Cwl.builder().url("https://some.external.reference/cwl").build()).build());
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/4421");
        }

        @Test
        public void testGetJob_ReturnsJobGetResponseRepresentation() throws Exception {
            String getJobResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-job-detailed-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobResponseBody));
            }

            JobGetResponse jobGetResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobGetResponse = insulaApiRestAdapter.getJob(4421L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/4421?projection=detailedJob");

            // Assert response is as expected
            assertThat(jobGetResponse.getId()).isEqualTo(4421);
            assertThat(jobGetResponse.getServiceName()).isEqualTo("serviceName");
            assertThat(jobGetResponse.getServiceId()).isEqualTo(70L);
            assertThat(jobGetResponse.getCreated()).isEqualTo(OffsetDateTime.parse("2025-02-06T14:51:15Z"));
            assertThat(jobGetResponse.getLastUpdated()).isEqualTo(OffsetDateTime.parse("2025-02-06T14:51:15Z"));
            assertThat(jobGetResponse.getStatus()).isEqualTo(JobLaunchResponse.Status.PENDING);
            assertThat(jobGetResponse.getPhase()).isEqualTo(JobLaunchResponse.Phase.CREATED);
            assertThat(jobGetResponse.getStartTime()).isNull();
            assertThat(jobGetResponse.getEndTime()).isNull();
            assertThat(jobGetResponse.getOutputs()).isEqualTo(Map.of(
                    "outputOne", List.of("eopaas://outputProduct/c7d6fedd-5e2d-4a31-b359-95c69e16ca92/outputOne/SampleGeotiff-1.tif"),
                    "outputTwo", List.of("eopaas://outputProduct/c7d6fedd-5e2d-4a31-b359-95c69e16ca92/outputTwo/SampleGeotiff-2.tif"))
            );
            assertThat(jobGetResponse.getOutputFiles()).isEqualTo(List.of(
                    OutputFile.builder()
                            .filename("c7d6fedd-5e2d-4a31-b359-95c69e16ca92/outputOne/SampleGeotiff-1.tif")
                            .links(Map.of("download", Link.of("https://processing.example.com/secure/api/v2.0/platformFiles/4864{?projection}/dl")))
                            .build(),
                    OutputFile.builder()
                            .filename("c7d6fedd-5e2d-4a31-b359-95c69e16ca92/outputTwo/SampleGeotiff-2.tif")
                            .links(Map.of("download", Link.of("https://processing.example.com/secure/api/v2.0/platformFiles/4865{?projection}/dl")))
                            .build()
            ));
        }

        @Test
        public void testGetJob_MapsParentAndSubJobsLinks_WhenPresentInInsulaResponse() throws Exception {
            String getJobResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-job-detailed-response-with-parent-child.json"));

            mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(getJobResponseBody));

            JobGetResponse jobGetResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobGetResponse = insulaApiRestAdapter.getJob(4421L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/4421?projection=detailedJob");

            assertThat(jobGetResponse.getLinks()).isNotNull();
            assertThat(jobGetResponse.getLinks()).containsKeys("parentJob", "subJobs");

            assertThat(jobGetResponse.getLinks().get("parentJob").get(0).getHref())
                    .isEqualTo("https://processing.example.com/secure/api/v2.0/jobs/4421/parentJob{?projection}");
            assertThat(jobGetResponse.getLinks().get("subJobs").get(0).getHref())
                    .isEqualTo("https://processing.example.com/secure/api/v2.0/jobs/4421/subJobs{?projection}");
        }

        @Test
        public void testFindJob_ReturnsJobFindResponseRepresentation() throws Exception {
            String getJobsParametricFindResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobsParametricFindResponseBody));
            }

            JobFindResponse jobFindResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobFindResponse = insulaApiRestAdapter.findJobs(1, 0,"serviceName",
                        List.of(JobResponse.Status.COMPLETED, JobResponse.Status.ERROR),
                        "2025-03-10T11:58:44.136/2025-03-10T11:59:58.973");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind" +
                    "?projection=detailedJob&size=1&sort=id&page=0&filter=serviceName&status=COMPLETED,ERROR" +
                    "&startTime=2025-03-10T11:58:44.136&endTime=2025-03-10T11:59:58.973");

            // Assert response is as expected
            List<JobGetResponse> jobsList = jobFindResponse.getEmbeddedJobs().getJobs();
            assertThat(jobsList).containsExactlyInAnyOrder(
                    JobGetResponse.builder()
                            .id(4322L)
                            .serviceName("serviceName")
                            .serviceId(70L)
                            .created(OffsetDateTime.parse("2025-03-09T11:58:44.136Z"))
                            .lastUpdated(OffsetDateTime.parse("2025-03-10T11:58:44.138Z"))
                            .startTime(LocalDateTime.parse("2025-03-10T11:58:44.136"))
                            .endTime(LocalDateTime.parse("2025-03-10T11:59:58.973"))
                            .phase(JobResponse.Phase.OUTPUT_LIST)
                            .status(JobResponse.Status.COMPLETED)
                            .outputs(Map.of("out",
                                    List.of("eopaas://outputProduct/c7d6fedd-5e2d-4a31-b359-95c69e16ca92/out/SampleGeotiff-1.tif")))
                            .outputFiles(List.of(OutputFile.builder()
                                    .filename("c7d6fedd-5e2d-4a31-b359-95c69e16ca92/out/SampleGeotiff-1.tif")
                                    .links(Map.of("download",
                                            Link.of("https://processing.example.com/secure/api/v2.0/platformFiles/4865{?projection}/dl")))
                                    .build()))
                            .extId("c7d6fedd-5e2d-4a31-b359-95c69e16ca92")
                            .links(Map.of("self", List.of(Link.of("href", "rel"))))
                            .config(JobGetResponse.JobConfig.builder()
                                    .inputs(Map.of(
                                            "in", List.of("eopaas://refData/11/SampleGeotiff-1.tif"),
                                            "collection",
                                            List.of(Map.of("out", "eopaas23d15d17d5954239951a927de8a936c5"))
                                    ))
                                    .build())
                            .build()
            );
        }

        @Test
        public void testFindJob_ReturnsJobFindResponseRepresentation_WhenOptionalQueryParametersAreMissing() throws Exception {
            String getJobsParametricFindResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-jobs-parametric-find-detailed-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobsParametricFindResponseBody));
            }

            JobFindResponse jobFindResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobFindResponse = insulaApiRestAdapter.findJobs(null, null, null, null, null);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob");

            // Assert response is as expected
            List<JobGetResponse> jobsList = jobFindResponse.getEmbeddedJobs().getJobs();
            assertThat(jobsList).containsExactlyInAnyOrder(
                    JobGetResponse.builder()
                            .id(4322L)
                            .serviceName("serviceName")
                            .serviceId(70L)
                            .created(OffsetDateTime.parse("2025-03-09T11:58:44.136Z"))
                            .lastUpdated(OffsetDateTime.parse("2025-03-10T11:58:44.138Z"))
                            .startTime(LocalDateTime.parse("2025-03-10T11:58:44.136"))
                            .endTime(LocalDateTime.parse("2025-03-10T11:59:58.973"))
                            .phase(JobResponse.Phase.OUTPUT_LIST)
                            .status(JobResponse.Status.COMPLETED)
                            .outputs(Map.of("out",
                                    List.of("eopaas://outputProduct/c7d6fedd-5e2d-4a31-b359-95c69e16ca92/out/SampleGeotiff-1.tif")))
                            .outputFiles(List.of(OutputFile.builder()
                                    .filename("c7d6fedd-5e2d-4a31-b359-95c69e16ca92/out/SampleGeotiff-1.tif")
                                    .links(Map.of("download",
                                            Link.of("https://processing.example.com/secure/api/v2.0/platformFiles/4865{?projection}/dl")))
                                    .build()))
                            .extId("c7d6fedd-5e2d-4a31-b359-95c69e16ca92")
                            .links(Map.of("self", List.of(Link.of("href", "rel"))))
                            .config(JobGetResponse.JobConfig.builder()
                                    .inputs(Map.of(
                                            "in", List.of("eopaas://refData/11/SampleGeotiff-1.tif"),
                                            "collection",
                                            List.of(Map.of("out", "eopaas23d15d17d5954239951a927de8a936c5"))
                                    ))
                                    .build())
                            .build()
            );
        }

        @Test
        public void testStacSearch_ReturnsStacSearchResponseRepresentation() throws Exception {
            String statcSearchResponseBody = Files.readString(BASE_TEST_PATH.resolve("stac-search-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(statcSearchResponseBody));
            }

            StacSearchResponse stacSearchResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                stacSearchResponse = insulaApiRestAdapter
                        .stacSearch("catalogue", "collection", "identifier");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/search?catalogue=catalogue&collection=collection&identifier=identifier");

            // Assert response is as expected
            assertThat(stacSearchResponse.getType()).isEqualTo("FeatureCollection");
            assertThat(stacSearchResponse.getNumberReturned()).isEqualTo(1);
            assertThat(stacSearchResponse.getNumberMatched()).isEqualTo(1);
            assertThat(stacSearchResponse.getLinks()).containsExactlyInAnyOrder(
                    Link.of("href", "first"),
                    Link.of("href", "last")
            );
            assertThat(stacSearchResponse.getFeatures()).hasSize(1);
            Map<String, Object> feature = (Map<String, Object>) stacSearchResponse.getFeatures().get(0);
            assertThat(feature.get("id")).isEqualTo("597094e4-eaf5-5c57-8f5a-cfd61048a3c3");
            assertThat(feature.get("type")).isEqualTo("Feature");
            assertThat(feature.get("stac_version")).isEqualTo("1.0.0");
            assertThat(feature).containsKey("properties");
            assertThat(feature).containsKey("geometry");
            assertThat(feature).containsKey("stac_extensions");
        }

        @Test
        public void testCancelJob_RequestsJobCancellation() throws Exception{
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.cancelJob(4421L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/4421/cancel");
        }

        @Test
        public void testTerminateJob_RequestsJobTermination() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.terminateJob(4421L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/4421/terminate");
        }

        @Test
        public void testCancelJob_ThrowsInsulaApiException_WhenResponseStatusIsNotFound() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.cancelJob(4421L);
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /jobs/4421/cancel returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/4421/cancel");
        }

        @Test
        public void testTerminateJob_ThrowsInsulaApiException_WhenResponseStatusIsNotFound() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.terminateJob(4421L);
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API POST /jobs/4421/terminate returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/4421/terminate");
        }

        @Test
        public void testStacSearch_ThrowsInsulaApiException_WhenStacSearchResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.stacSearch("catalogue", "collection", "identifier");
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /search returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/search?catalogue=catalogue&collection=collection&identifier=identifier");
        }

        @Test
        public void testUpdate_ThrowsInsulaApiException_WhenUserCannotUpdateService() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(401));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.update(4421L, ServiceCreationRequest.builder()
                        .cwl(Cwl.builder().url("https://some.external.reference/cwl").build()).build());
                fail();
            }  catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API PUT /services/4421 returned with status: 401 UNAUTHORIZED");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/4421");
        }

        @Test
        public void testDisableService_ThrowsInsulaApiException_WhenUserCannotDeleteService() throws Exception {
            String insulaResponseAsString = Files.readString(BASE_TEST_PATH.resolve("get-service-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(insulaResponseAsString));
                mockWebServer.enqueue(new MockResponse().setResponseCode(401));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.disableService(4421L);
                fail();
            }  catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API POST /services/4421/disable returned with status: 401 UNAUTHORIZED");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/4421");
            }
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/services/4421/disable");
            }
        }

        @Test
        public void testDisableService_ThrowsServiceNotFoundException_WhenServiceDoesNotExist() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.disableService(4421L);
                fail();
            }  catch (ServiceNotFoundException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /services/4421 returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/4421");
        }

        @Test
        public void testGetJob_ThrowsInsulaApiException_WhenJobGetResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.getJob(0L);
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /jobs/0?projection=detailedJob returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/0?projection=detailedJob");
        }

        @Test
        public void testFindJob_ThrowsInsulaApiException_WhenJobFindResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.findJobs(null, null, null, null, null);
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /jobs/search/parametricFind?projection=detailedJob returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/search/parametricFind?projection=detailedJob");
        }

        @Test
        public void testCreate_ThrowsInsulaApiException_WhenServiceCreationIsRequestedAndResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.create(JobConfigCreationRequest.builder().service(URI.create("https://insula/services/1"))
                        .build());
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API POST /jobConfigs returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"https://insula/services/1\"}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }

        @Test
        public void testCreate_ThrowsInsulaApiException_WhenJobConfigCreationIsRequestedAndResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("service not found"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.create(ServiceCreationRequest.builder()
                        .cwl(Cwl.builder().url("https://some.external.reference/cwl").build()).build());
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API POST /services returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"cwl\":{\"url\":\"https://some.external.reference/cwl\"}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/services");
        }

        @Test
        public void testCreate_ThrowsInsulaApiExceptionAndSendsDeleteJobConfigRequest_WhenUserCannotLaunchJobConfig() throws Exception {
            String jobConfigCreationResponseBody = Files.readString(BASE_TEST_PATH.resolve("job-config-creation-response.json"));
            {
                // POST Job Config Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(jobConfigCreationResponseBody));

                // EstimateCost Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(402)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"));

                // Delete Job Config Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(204)
                        .setHeader("Content-Type", "application/json"));
            }

            Multimap<String, Object> inputs = ArrayListMultimap.create();
            inputs.put("inputOne", "eopaas://in1");
            inputs.put("inputTwo", "eopaas://in2");
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.create(JobConfigCreationRequest.builder()
                        .service(URI.create("https://insula/services/1"))
                        .inputs(inputs)
                        .build());
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /estimateCost/jobConfig/3039 returned with status: 402 PAYMENT_REQUIRED");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"https://insula/services/1\"," +
                        "\"inputs\":{" +
                        "\"inputOne\":[\"eopaas://in1\"]," +
                        "\"inputTwo\":[\"eopaas://in2\"]}}");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039");
            }
        }

        @Test
        public void testCreate_ThrowsInsulaApiException_WhenUserCannotLaunchJobConfigAndJobConfigDeleteResponseStatusIsUnauthorized() throws Exception {
            String jobConfigCreationResponseBody = Files.readString(BASE_TEST_PATH.resolve("job-config-creation-response.json"));
            {
                // POST Job Config Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(jobConfigCreationResponseBody));

                // EstimateCost Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(402)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"));

                // Delete Job Config Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(401)
                        .setHeader("Content-Type", "application/json"));
            }

            Multimap<String, Object> inputs = ArrayListMultimap.create();
            inputs.put("inputOne", "eopaas://in1");
            inputs.put("inputTwo", "eopaas://in2");
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.create(JobConfigCreationRequest.builder()
                        .service(URI.create("https://insula/services/1"))
                        .inputs(inputs)
                        .build());
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API DELETE /jobConfigs/3039 returned with status: 401 UNAUTHORIZED");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"https://insula/services/1\"," +
                        "\"inputs\":{" +
                        "\"inputOne\":[\"eopaas://in1\"]," +
                        "\"inputTwo\":[\"eopaas://in2\"]}}");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/estimateCost/jobConfig/3039");
            }

            {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
                assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
                assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
                assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
                assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3039");
            }
        }

        @Test
        public void testGetService_ThrowsServiceNotFoundException_WhenResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                        .setHeader("Content-Type", "application/json"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.getService(100L);
                fail();
            } catch (ServiceNotFoundException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /services/100 returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/100");
        }

        @Test
        public void testLaunch_ThrowsInsulaApIException_WhenResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                        .setHeader("Content-Type", "application/json"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.launch(JobLaunchRequest.builder().jobConfigId(3030L).build());
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API POST /jobConfigs/3030/launch returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs/3030/launch");
        }

        @Test
        public void testCreate_ThrowsResourceAccessException_WhenInsulaDoesNotRespondAndAfterReachingReadTimeout() {
            {
                mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            }

            Long start = System.currentTimeMillis();
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.create(ServiceCreationRequest.builder()
                        .cwl(Cwl.builder().url("https://some.external.reference/cwl").build()).build());
                fail();
            } catch (ResourceAccessException e) {
                Long finish = System.currentTimeMillis();
                long timeout = insulaClientProperties.getReadTimeout();
                assertThat((finish - start)).isBetween(timeout - 350, timeout + 350);
            }
        }

        @Test
        public void testGetServices_ThrowsInsulaApiException_WhenResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404)
                        .setHeader("Content-Type", "application/json"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.getServices(2);
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /services/search/parametricFind?size=2&status=AVAILABLE," +
                        "IN_DEVELOPMENT&projection=detailedPlatformService returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/services/search/parametricFind?size=2&status=AVAILABLE,IN_DEVELOPMENT&projection=detailedPlatformService");
        }

        @Test
        public void testGetJobConfig_ReturnsJobConfigGetResponseRepresentation() throws Exception {
            String getJobConfigResponseBody = Files.readString(BASE_TEST_PATH.resolve("get-job-config-response.json"));
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(getJobConfigResponseBody));
            }

            JobConfigGetResponse jobConfigGetResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                jobConfigGetResponse = insulaApiRestAdapter.getJobConfig(15455L);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/15455/config");

            assertThat(jobConfigGetResponse).isNotNull();
            assertThat(jobConfigGetResponse.getEmbedded()).isNotNull();
            assertThat(jobConfigGetResponse.getEmbedded().getService()).isNotNull();
            assertThat(jobConfigGetResponse.getEmbedded().getService().getDockerTag())
                    .isEqualTo("eopaas/sardem-sarsen-process:8f50d01c");
        }

        @Test
        public void testGetJobConfig_ThrowsInsulaApiException_WhenResponseStatusIsNotSuccessful() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.getJobConfig(15455L);
                fail();
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /jobs/15455/config returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/15455/config");
        }

        @Test
        public void testGetJobOutputs_ReturnJobOutputsAsStacSearchResponseRepresentation() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(Files.readString(BASE_TEST_PATH.resolve("stac-search-response.json"))));
            }

            StacSearchResponse stacSearchResponse;
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                stacSearchResponse = insulaApiRestAdapter
                        .getJobOutputs(1234L, "output identifier");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/1234/outputs/output%20identifier");

            // Assert response is as expected
            assertThat(stacSearchResponse.getType()).isEqualTo("FeatureCollection");
            assertThat(stacSearchResponse.getNumberReturned()).isEqualTo(1);
            assertThat(stacSearchResponse.getNumberMatched()).isEqualTo(1);
            assertThat(stacSearchResponse.getLinks()).containsExactlyInAnyOrder(
                    Link.of("href", "first"),
                    Link.of("href", "last")
            );
            assertThat(stacSearchResponse.getFeatures()).hasSize(1);
            Map<String, Object> feature = (Map<String, Object>) stacSearchResponse.getFeatures().get(0);
            assertThat(feature.get("id")).isEqualTo("597094e4-eaf5-5c57-8f5a-cfd61048a3c3");
            assertThat(feature.get("type")).isEqualTo("Feature");
            assertThat(feature.get("stac_version")).isEqualTo("1.0.0");
            assertThat(feature).containsKey("properties");
            assertThat(feature).containsKey("geometry");
            assertThat(feature).containsKey("stac_extensions");

        }

        @Test
        public void testGetJobOutputs_ThrowsInsulaApiException_WhenResponseStatusIsNotFound() throws Exception {
            {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            }

            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter
                        .getJobOutputs(1234L, "outputIdentifier");
            } catch (InsulaApiException e) {
                assertThat(e.getMessage()).isEqualTo("Insula API GET /jobs/{jobId}/outputs/{outputId} returned with status: 404 NOT_FOUND");
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getHeader("Accept")).isEqualTo("application/geo+json");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobs/1234/outputs/outputIdentifier");
        }

    }

    @TestPropertySource(locations = "classpath:test-application.properties", properties = {
            "ogcapi.processes.insula.client.baseUrl=http://10.0.10.10",
    })
    public static class InsulaApiRestAdapterUnreachableServiceIT extends InsulaApiRestAdapterIT {
        @Test
        public void testCreate_ThrowsResourceAccessException_WhenInsulaIsNotAvailableAndAfterReachingConnectionTimeout() {
            Long start = System.currentTimeMillis();
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                insulaApiRestAdapter.create(ServiceCreationRequest.builder()
                        .cwl(Cwl.builder().url("https://some.external.reference/cwl").build()).build());
                fail();
            } catch (ResourceAccessException e) {
                Long finish = System.currentTimeMillis();
                long timeout = insulaClientProperties.getConnectTimeout();
                assertThat((finish - start)).isBetween(timeout - 350, timeout + 350);
            }
        }
    }

    @TestPropertySource(properties = {
            "ogcapi.processes.job.costEstimate.enabled=false"
    })
    public static class InsulaApiRestAdapterCostEstimateDisabledIT extends InsulaApiRestAdapterIT {

        @Test
        public void testCreate_DoesNotRequestCostEstimate_WhenCostEstimateIsDisabled() throws Exception {
            {
                // POST Job Config Response
                mockWebServer.enqueue(new MockResponse().setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody(Files.readString(BASE_TEST_PATH.resolve("job-config-creation-response.json"))));
            }

            Multimap<String, Object> inputs = ArrayListMultimap.create();
            inputs.put("inputOne", "eopaas://in1");
            inputs.put("inputTwo", "eopaas://in2");
            try (RequestContextHolder rc = new RequestContextHolder(new RequestContext("insulaUser", "tenant"))) {
                assertThat(insulaApiRestAdapter.create(JobConfigCreationRequest.builder()
                        .service(URI.create("https://insula/services/1"))
                        .inputs(inputs)
                        .build()).getId()).isEqualTo(3039);
            }

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("user")).isEqualTo("insulaUser");
            assertThat(recordedRequest.getHeader("tenant")).isEqualTo("tenant");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"service\":\"https://insula/services/1\"," +
                    "\"inputs\":{" +
                    "\"inputOne\":[\"eopaas://in1\"]," +
                    "\"inputTwo\":[\"eopaas://in2\"]}}");
            assertThat(recordedRequest.getPath()).isEqualTo("/jobConfigs");
        }
    }
}