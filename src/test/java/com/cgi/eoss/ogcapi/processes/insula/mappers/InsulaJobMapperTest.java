package com.cgi.eoss.ogcapi.processes.insula.mappers;

import com.cgi.eoss.ogcapi.processes.controllers.ProcessesApi;
import com.cgi.eoss.ogcapi.processes.insula.model.OutputFile;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobConfigCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobFindResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobGetResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobLaunchResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse;
import com.cgi.eoss.ogcapi.processes.model.Execute;
import com.cgi.eoss.ogcapi.processes.model.InlineOrRefData;
import com.cgi.eoss.ogcapi.processes.model.Input;
import com.cgi.eoss.ogcapi.processes.model.JobList;
import com.cgi.eoss.ogcapi.processes.model.Link;
import com.cgi.eoss.ogcapi.processes.model.StatusCode;
import com.cgi.eoss.ogcapi.processes.model.StatusInfo;
import com.cgi.eoss.ogcapi.processes.model.inputs.InputValueNoObjectArray;
import com.cgi.eoss.ogcapi.processes.model.inputs.InputValueNoObjectBoolean;
import com.cgi.eoss.ogcapi.processes.model.inputs.InputValueNoObjectInteger;
import com.cgi.eoss.ogcapi.processes.model.inputs.InputValueNoObjectString;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toJobList;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toOutput;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toOutputResults;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toStatus;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toStatusCode;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.withQueryParameters;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.CANCELLED;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.COMPLETED;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.CONDITION_WAIT;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.CREATED;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.ERROR;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.PENDING;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.WAITING;
import static com.cgi.eoss.ogcapi.processes.model.StatusCode.ACCEPTED;
import static com.cgi.eoss.ogcapi.processes.model.StatusCode.DISMISSED;
import static com.cgi.eoss.ogcapi.processes.model.StatusCode.FAILED;
import static com.cgi.eoss.ogcapi.processes.model.StatusCode.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class InsulaJobMapperTest {

    @Test
    public void testBuildGetJobStatusLinks_ReturnsOnlySelfLink_WhenJobGetResponseAndLinksAreEmpty() {
        List<org.springframework.hateoas.Link> links = insulaJobMapperWithJobLinkMappingEnabled()
                .buildGetJobStatusLinks("1", null, null);

        assertThat(links).hasSize(1);
        org.springframework.hateoas.Link selfLink = links.get(0);
        assertThat(selfLink.getHref()).isEqualTo("/jobs/1");
        assertThat(selfLink.getRel().value()).isEqualTo("self");
        assertThat(selfLink.getTitle()).isEqualTo("Job Status");
        assertThat(selfLink.getType()).isEqualTo("application/json");
    }

    @Test
    public void testBuildGetJobStatusLinks_ReturnsOnlySelfLink_WhenJobLinkMappingIsDisabled() {
        Map<String, List<org.springframework.hateoas.Link>> insulaLinks = Map.of(
                "parentJob", List.of(org.springframework.hateoas.Link.of("http://insula/jobs/parent-1")),
                "subJobs", List.of(org.springframework.hateoas.Link.of("http://insula/jobs/1/subjobs"))
        );
        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .links(insulaLinks)
                .build();

        List<org.springframework.hateoas.Link> links = insulaJobMapperWithJobLinkMappingDisabled()
                .buildGetJobStatusLinks("1", jobGetResponse, null);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).getRel().value()).isEqualTo("self");
    }

    @Test
    public void testBuildGetJobStatusLinks_ReturnsSelfParentAndChildLinks_WhenJobLinkMappingIsEnabled() {
        Map<String, List<org.springframework.hateoas.Link>> insulaLinks = Map.of(
                "parentJob", List.of(org.springframework.hateoas.Link.of("http://insula/jobs/parent-1")),
                "subJobs", List.of(org.springframework.hateoas.Link.of("http://insula/jobs/1/subjobs"))
        );
        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .links(insulaLinks)
                .build();

        List<org.springframework.hateoas.Link> links = insulaJobMapperWithJobLinkMappingEnabled()
                .buildGetJobStatusLinks("1", jobGetResponse, null);

        assertThat(links).hasSize(3);
        assertThat(links.get(0).getRel().value()).isEqualTo("self");

        org.springframework.hateoas.Link parentLink = links.get(1);
        assertThat(parentLink.getHref()).isEqualTo("http://insula/jobs/parent-1");
        assertThat(parentLink.getRel().value()).isEqualTo("insula-parent-job");
        assertThat(parentLink.getTitle()).isEqualTo("Insula Parent Job");
        assertThat(parentLink.getType()).isEqualTo("application/json");

        org.springframework.hateoas.Link childLink = links.get(2);
        assertThat(childLink.getHref()).isEqualTo("http://insula/jobs/1/subjobs");
        assertThat(childLink.getRel().value()).isEqualTo("insula-child-jobs");
        assertThat(childLink.getTitle()).isEqualTo("Insula Child Jobs");
        assertThat(childLink.getType()).isEqualTo("application/json");
    }

    @Test
    public void testToJobConfigCreationRequest_ReturnsJobConfigCreationRequestWithProvidedValues() {
        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest("https://insula.url/api", 10L,
                        Map.of("stringInput", new InputValueNoObjectString("stringValue"),
                                "booleanInput", new InputValueNoObjectBoolean(true)));
        assertThat(jobConfigCreationRequest.getService()).isEqualTo(URI.create("https://insula.url/api/services/10"));
        Multimap<String, Object> inputs = ArrayListMultimap.create();
        inputs.put("stringInput", "stringValue");
        inputs.put("booleanInput", true);
        assertThat(jobConfigCreationRequest.getInputs()).isEqualTo(inputs);
    }

    @Test
    public void testToJobConfigCreationRequest_ReturnsJobConfigCreationRequestWithProvidedValues_WhenInputsAreEmpty() {
        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest("https://insula.url/api", 10L, Collections.emptyMap());
        assertThat(jobConfigCreationRequest.getService()).isEqualTo(URI.create("https://insula.url/api/services/10"));
        assertThat(jobConfigCreationRequest.getInputs().asMap()).isEmpty();
    }

    @Test
    public void testToJobConfigCreationRequest_ReturnsJobConfigCreationRequestWithProvidedValues_WhenInputsAreNull() {
        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest("https://insula.url/api", 10L, null);
        assertThat(jobConfigCreationRequest.getService()).isEqualTo(URI.create("https://insula.url/api/services/10"));
        assertThat(jobConfigCreationRequest.getInputs().asMap()).isEmpty();
    }

    @Test
    public void testToJobConfigCreationRequest_ReturnsJobConfigCreationRequestWithProvidedValues_WhenBaseUrlContainsTrailingSlash() {
        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest("https://insula.url/api/", 10L, null);
        assertThat(jobConfigCreationRequest.getService()).isEqualTo(URI.create("https://insula.url/api/services/10"));
        assertThat(jobConfigCreationRequest.getInputs().asMap()).isEmpty();
    }

    @Test
    public void testToJobConfigCreationRequest_ThrowsIllegalArgumentException_WhenBaseUrlIsNotValid() {
        assertThatThrownBy(() ->
                InsulaJobMapper.toJobConfigCreationRequest("an invalid url", 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal character in path at index 2: an invalid url/services/10");
    }

    @Test
    public void testToJobConfigCreationRequest_FlattensStringArrayInputs_WhenInputIsArrayOfStrings(){
        Input bboxInput = new InputValueNoObjectArray<>(
                List.of(
                        new InputValueNoObjectString("10 20 30 40"),
                        new InputValueNoObjectString("11 21 31 41")
                )
        );

        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest(
                        "https://insula.url/api", 10L, Map.of("bbox", bboxInput)
                );

        Multimap<String, Object> expected = ArrayListMultimap.create();
        expected.put("bbox", "10 20 30 40");
        expected.put("bbox", "11 21 31 41");

        assertThat(jobConfigCreationRequest.getInputs()).isEqualTo(expected);
    }

    @Test
    public void testToJobConfigCreationRequest_MapsInputValue_WhenInputIsString() {
        Input bboxInput = new InputValueNoObjectString(
                "42.07 43.32 52.07 53.32"
        );

        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest(
                        "https://insula.url/api", 10L, Map.of("bbox", bboxInput)
                );

        Multimap<String, Object> expected = ArrayListMultimap.create();
        expected.put("bbox", "42.07 43.32 52.07 53.32");

        assertThat(jobConfigCreationRequest.getInputs()).isEqualTo(expected);
    }

    @Test
    public void testToJobConfigCreationRequest_ExcludesBlankStringValues_WhenInputIsArray() {
        Input malformedBboxInput = new InputValueNoObjectArray<>(
                List.of(
                        new InputValueNoObjectString("  "),
                        new InputValueNoObjectString("42.07 43.32 52.07 53.32")
                )
        );

        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest(
                        "https://insula.url/api", 10L, Map.of("bbox", malformedBboxInput)
                );

        assertThat(jobConfigCreationRequest.getInputs().get("bbox")).containsExactly("42.07 43.32 52.07 53.32");
    }

    @Test
    public void testToJobConfigCreationRequest_SkipsNullValues_WhenInputIsArray() {
        List<Input> values = new ArrayList<>();
        values.add(null);
        values.add(new InputValueNoObjectString("42.07 43.32 52.07 53.32"));

        JobConfigCreationRequest req = InsulaJobMapper.toJobConfigCreationRequest(
                "https://insula.url/api", 10L, Map.of("bbox", new InputValueNoObjectArray<>(values))
        );

        assertThat(req.getInputs().get("bbox")).containsExactly("42.07 43.32 52.07 53.32");
    }

    @Test
    public void testToJobConfigCreationRequest_PreservesNumericValues_WhenInputIsArray() {
        Input numericArray = new InputValueNoObjectArray<>(
                List.of(
                        new InputValueNoObjectInteger(2),
                        new InputValueNoObjectInteger(3)
                )
        );

        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest(
                        "https://insula.url/api", 10L, Map.of("intValue", numericArray)
                );

        Multimap<String, Object> expected = ArrayListMultimap.create();
        expected.put("intValue", 2);
        expected.put("intValue", 3);

        assertThat(jobConfigCreationRequest.getInputs()).isEqualTo(expected);
    }

    @Test
    public void testToJobConfigCreationRequest_SkipsInputEntry_WhenUnwrappedValueIsNull() {
        JobConfigCreationRequest jobConfigCreationRequest =
                InsulaJobMapper.toJobConfigCreationRequest(
                        "https://insula.url/api",
                        10L,
                        Map.of(
                                "nullInput", new InputValueNoObjectString(null),
                                "validInput", new InputValueNoObjectString("ok")
                        )
                );

        assertThat(jobConfigCreationRequest.getInputs().get("nullInput")).isEmpty();
        assertThat(jobConfigCreationRequest.getInputs().get("validInput")).containsExactly("ok");
    }

    @Test
    public void testToJobConfigCreationRequest_DoesNotUnwrapInput_WhenInputIsNotInputValueNoObjectOneOf() {
        Input nonWrapperInput = mock(Input.class);
        JobConfigCreationRequest jobConfigCreationRequest = InsulaJobMapper.toJobConfigCreationRequest(
                "https://insula.url/api",
                10L,
                Map.of("refInput", nonWrapperInput)
        );
        assertThat(jobConfigCreationRequest.getInputs().get("refInput")).containsExactly(nonWrapperInput);
    }

    @Test
    public void testToStatusInfo_ReturnsStatusInfoWithProvidedValues_WhenAllJobLaunchResponseResponseAttributesArePopulated() {
        JobLaunchResponse jobLaunchResponse =  JobLaunchResponse.builder()
                .status(JobLaunchResponse.Status.COMPLETED).phase(JobLaunchResponse.Phase.OUTPUT_LIST).id(10L)
                .created(OffsetDateTime.parse("2025-02-06T13:03:02Z")).lastUpdated(OffsetDateTime.parse("2025-02-06T13:03:05Z"))
                .startTime(LocalDateTime.parse("2025-02-06T13:31:10")).endTime(LocalDateTime.parse("2025-02-06T13:43:06"))
                .build();
        StatusInfo statusInfo = InsulaJobMapper.toStatusInfo(jobLaunchResponse,
                List.of(linkTo(methodOn(ProcessesApi.class).execute(10L, new Execute()))
                        .withSelfRel().withTitle("self")), 70L, List.of());
        assertThat(statusInfo.getId()).isEqualTo("10");
        assertThat(statusInfo.getCreated()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:03:02Z"));
        assertThat(statusInfo.getStarted()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:31:10Z"));
        assertThat(statusInfo.getUpdated()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:03:05Z"));
        assertThat(statusInfo.getFinished()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:43:06Z"));
        assertThat(statusInfo.getStatus()).isEqualTo(SUCCESSFUL);
        assertThat(statusInfo.getProgress()).isEqualTo(100);
        assertThat(statusInfo.getProcessID()).isEqualTo(URI.create("/processes/70"));
        assertThat(statusInfo.getLinks()).hasSize(1);
        Link selfLink = new Link("/processes/10/execution");
        selfLink.setRel("self");
        selfLink.setTitle("self");
        assertThat(statusInfo.getLinks().get(0)).isEqualTo(selfLink);

        assertThat(statusInfo.getDescription()).isNull();
        assertThat(statusInfo.getTitle()).isNull();
        assertThat(statusInfo.getKeywords()).isEmpty();
        assertThat(statusInfo.getMetadata()).isEmpty();
        assertThat(statusInfo.getRequest()).isNull();
        assertThat(statusInfo.getMessage()).isNull();
        assertThat(statusInfo.getType()).isNull();
        assertThat(statusInfo.getException()).isNull();
    }

    @Test
    public void testToStatusInfo_ReturnsStatusInfoWithProvidedValues_WhenAllJobGetResponseAttributesArePopulated() {
        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .status(JobResponse.Status.COMPLETED).phase(JobResponse.Phase.OUTPUT_LIST).id(10L)
                .created(OffsetDateTime.parse("2025-02-06T13:03:02Z")).lastUpdated(OffsetDateTime.parse("2025-02-06T13:03:05Z"))
                .startTime(LocalDateTime.parse("2025-02-06T13:31:10")).endTime(LocalDateTime.parse("2025-02-06T13:43:06"))
                .serviceName("serviceOne")
                .subJobStatusCounts(Map.of(JobResponse.Status.RUNNING, 1, JobResponse.Status.COMPLETED, 2))
                .build();
        StatusInfo statusInfo = InsulaJobMapper.toStatusInfo(jobGetResponse,
                List.of(linkTo(methodOn(ProcessesApi.class).execute(10L, new Execute()))
                        .withSelfRel().withTitle("self")), 70L, List.of());
        assertThat(statusInfo.getId()).isEqualTo("10");
        assertThat(statusInfo.getCreated()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:03:02Z"));
        assertThat(statusInfo.getStarted()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:31:10Z"));
        assertThat(statusInfo.getUpdated()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:03:05Z"));
        assertThat(statusInfo.getFinished()).isEqualTo(OffsetDateTime.parse("2025-02-06T13:43:06Z"));
        assertThat(statusInfo.getStatus()).isEqualTo(SUCCESSFUL);
        assertThat(statusInfo.getProgress()).isEqualTo(100);
        assertThat(statusInfo.getProcessID()).isEqualTo(URI.create("/processes/70"));
        assertThat(statusInfo.getLinks()).hasSize(1);
        Link selfLink = new Link("/processes/10/execution");
        selfLink.setRel("self");
        selfLink.setTitle("self");
        assertThat(statusInfo.getLinks().get(0)).isEqualTo(selfLink);

        assertThat(statusInfo.getDescription()).isNull();
        assertThat(statusInfo.getTitle()).isEqualTo("serviceOne");
        assertThat(statusInfo.getKeywords()).isEmpty();
        assertThat(statusInfo.getMetadata()).isEmpty();
        assertThat(statusInfo.getRequest()).isNull();
        assertThat(statusInfo.getMessage()).isEqualTo("Subjobs: RUNNING: 1 - SUCCESSFUL: 2");
        assertThat(statusInfo.getType()).isNull();
        assertThat(statusInfo.getException()).isNull();
    }

    @Test
    public void testToStatusInfo_ReturnsStatusInfoWithEmptyFields_WhenJobGetResponseAttributesAreNotPopulated() {
        JobGetResponse jobGetResponse = JobGetResponse.builder()
                .build();
        StatusInfo statusInfo = InsulaJobMapper.toStatusInfo(jobGetResponse, Collections.emptyList(), 70L, List.of());
        assertThat(statusInfo.getId()).isEqualTo("null");
        assertThat(statusInfo.getProcessID()).isEqualTo(URI.create("/processes/70"));
        assertThat(statusInfo.getDescription()).isNull();
        assertThat(statusInfo.getLinks()).isEmpty();
        assertThat(statusInfo.getTitle()).isNull();
        assertThat(statusInfo.getKeywords()).isEmpty();
        assertThat(statusInfo.getMetadata()).isEmpty();
        assertThat(statusInfo.getCreated()).isNull();
        assertThat(statusInfo.getStarted()).isNull();
        assertThat(statusInfo.getUpdated()).isNull();
        assertThat(statusInfo.getFinished()).isNull();
        assertThat(statusInfo.getStatus()).isEqualTo(DISMISSED);
        assertThat(statusInfo.getProgress()).isEqualTo(0);
        assertThat(statusInfo.getRequest()).isNull();
        assertThat(statusInfo.getMessage()).isNull();
        assertThat(statusInfo.getType()).isNull();
        assertThat(statusInfo.getException()).isNull();
    }

    @Test
    public void testToStatusInfo_ReturnsStatusInfoWithEmptyFields_WhenJobLaunchResponseResponseAttributesAreNotPopulated() {
        JobLaunchResponse jobLaunchResponse =  JobLaunchResponse.builder()
                .build();
        StatusInfo statusInfo = InsulaJobMapper.toStatusInfo(jobLaunchResponse, Collections.emptyList(), 70L, List.of());
        assertThat(statusInfo.getId()).isEqualTo("null");
        assertThat(statusInfo.getProcessID()).isEqualTo(URI.create("/processes/70"));
        assertThat(statusInfo.getDescription()).isNull();
        assertThat(statusInfo.getLinks()).isEmpty();
        assertThat(statusInfo.getTitle()).isNull();
        assertThat(statusInfo.getKeywords()).isEmpty();
        assertThat(statusInfo.getMetadata()).isEmpty();
        assertThat(statusInfo.getCreated()).isNull();
        assertThat(statusInfo.getStarted()).isNull();
        assertThat(statusInfo.getUpdated()).isNull();
        assertThat(statusInfo.getFinished()).isNull();
        assertThat(statusInfo.getStatus()).isEqualTo(DISMISSED);
        assertThat(statusInfo.getProgress()).isEqualTo(0);
        assertThat(statusInfo.getRequest()).isNull();
        assertThat(statusInfo.getMessage()).isNull();
        assertThat(statusInfo.getType()).isNull();
        assertThat(statusInfo.getException()).isNull();
    }

    @Test
    public void testWithQueryParameters_ReturnsPathWithQueryParameters() {
        String path = "/path/to/query";
        String projection = "projectionValue";
        Integer limit = 10;
        Integer page = 0;
        String processID = "service";
        List<JobResponse.Status> status = List.of(JobResponse.Status.CREATED);
        String datetime = "2025-03-12T13:51:01/2025-03-12T13:52:09";

        String pathWithQueryParameters = withQueryParameters(path, projection, limit, page, processID, status, datetime);
        assertThat(pathWithQueryParameters).isEqualTo(path+"?projection="+projection+
                "&size="+limit+"&sort=id&page="+page+"&filter="+processID+"&status=CREATED&startTime=2025-03-12T13:51:01&endTime=2025-03-12T13:52:09");
    }

    @Test
    public void testWithQueryParameters_ReturnsPathAsItIs_WhenQueryParametersAreMissing() {
        String path = "/path/to/query";
        String projection = null;
        Integer limit = null;
        String processID = null;
        List<JobResponse.Status> status = null;
        String datetime = null;
        Integer page = null;

        String pathWithQueryParameters = withQueryParameters(path, projection, limit, page, processID, status, datetime);
        assertThat(pathWithQueryParameters).isEqualTo(path);
    }

    @Test
    public void testToStatus_MapsStatusCodeToStatus() {
        assertThat(toStatus(null)).isEqualTo(CANCELLED);
        assertThat(toStatus(ACCEPTED)).isEqualTo(CREATED);
        assertThat(toStatus(StatusCode.RUNNING)).isEqualTo(JobResponse.Status.RUNNING);
        assertThat(toStatus(SUCCESSFUL)).isEqualTo(COMPLETED);
        assertThat(toStatus(FAILED)).isEqualTo(ERROR);
        assertThat(toStatus(DISMISSED)).isEqualTo(CANCELLED);
    }

    @Test
    public void testToStatusCode_MapsStatusToStatusCode() {
        assertThat(toStatusCode(null)).isEqualTo(DISMISSED);
        assertThat(toStatusCode(CREATED)).isEqualTo(ACCEPTED);
        assertThat(toStatusCode(PENDING)).isEqualTo(ACCEPTED);
        assertThat(toStatusCode(WAITING)).isEqualTo(ACCEPTED);
        assertThat(toStatusCode(CONDITION_WAIT)).isEqualTo(ACCEPTED);
        assertThat(toStatusCode(JobResponse.Status.RUNNING)).isEqualTo(StatusCode.RUNNING);
        assertThat(toStatusCode(COMPLETED)).isEqualTo(SUCCESSFUL);
        assertThat(toStatusCode(ERROR)).isEqualTo(FAILED);
        assertThat(toStatusCode(CANCELLED)).isEqualTo(DISMISSED);
    }

    @Test
    public void testToJobList_MapsJobFindResponseToJobList() {
        JobFindResponse jobFindResponse = JobFindResponse.builder()
                .embeddedJobs(JobFindResponse.EmbeddedJobs.builder()
                        .jobs(List.of(
                                JobGetResponse.builder()
                                        .id(1L)
                                        .serviceName("serviceOne")
                                        .serviceId(10L)
                                        .build(),
                                JobGetResponse.builder()
                                        .id(2L)
                                        .serviceName("serviceTwo")
                                        .serviceId(20L)
                                        .build()))
                        .build())
                .build();

        JobList jobList = toJobList(jobFindResponse, List.of(org.springframework.hateoas.Link.of("href", "self")));

        Link selfLink = new Link("href");
        selfLink.setRel("self");
        assertThat(jobList.getLinks()).containsExactly(selfLink);

        List<StatusInfo> jobs = jobList.getJobs();
        assertThat(jobs).hasSize(2);

        StatusInfo firstJob = new StatusInfo();
        firstJob.setId("1");
        firstJob.setTitle("serviceOne");
        firstJob.setStatus(DISMISSED);
        firstJob.setProcessID(URI.create("/processes/10"));
        firstJob.setProgress(0);
        Link ogcLinkOne = new Link("/jobs/1");
        ogcLinkOne.setRel("self");
        ogcLinkOne.setTitle("Job Status");
        ogcLinkOne.setType("application/json");
        firstJob.setLinks(List.of(ogcLinkOne));

        StatusInfo secondJob = new StatusInfo();
        secondJob.setId("2");
        secondJob.setTitle("serviceTwo");
        secondJob.setStatus(DISMISSED);
        secondJob.setProcessID(URI.create("/processes/20"));
        secondJob.setProgress(0);
        Link ogcLinkTwo = new Link("/jobs/2");
        ogcLinkTwo.setRel("self");
        ogcLinkTwo.setTitle("Job Status");
        ogcLinkTwo.setType("application/json");
        secondJob.setLinks(List.of(ogcLinkTwo));

        assertThat(jobs).containsExactlyInAnyOrder(firstJob, secondJob);
    }

    @Test
    public void testToJobList_MapsJobFindResponseToEmptyJobList_WhenJobFindResponseDoesNotContainJobs() {
        JobFindResponse jobFindResponse = JobFindResponse.builder()
                .embeddedJobs(JobFindResponse.EmbeddedJobs.builder()
                        .jobs(List.of())
                        .build())
                .build();

        List<StatusInfo> jobs = toJobList(jobFindResponse, List.of(org.springframework.hateoas.Link.of("self"))).getJobs();
        assertThat(jobs).isEmpty();
    }

    @Test
    public void testToOutputResults_MapsOutputsToOutputResultsWithLinks() {
        String jobId = "jobId";
        Map<String, List<String>> outputs = Map.of(
                "outputOne", List.of("eopaas://ref/file1.tiff"),
                "outputTwo", List.of("eopaas://ref/file2.tiff")
        );

        Link outputOneLink = new Link();
        outputOneLink.setHref("/jobs/jobId/results/outputOne");
        outputOneLink.setRel("output");
        outputOneLink.setTitle("outputOne");
        Link outputTwoLink = new Link();
        outputTwoLink.setHref("/jobs/jobId/results/outputTwo");
        outputTwoLink.setRel("output");
        outputTwoLink.setTitle("outputTwo");

        assertThat(toOutputResults(jobId, outputs)).isEqualTo(Map.of(
                "outputOne", outputOneLink,
                "outputTwo", outputTwoLink)
        );
    }

    @Test
    public void testToOutputResults_ReturnsEmptyMap_WhenOutputIsNull() {
        String jobId = "jobId";
        Map<String, List<String>> output = null;
        assertThat(toOutputResults(jobId, output)).isEmpty();
    }

    @Test
    public void testToOutput_MapsOutputFilesToListOfDownloadLinks() {
        String outputId = "out";
        List<String> outputFileReferences = List.of("eopaas://ref/file.tiff");
        Map<String, List<String>> output = Map.of(outputId, outputFileReferences);
        List<OutputFile> outputFiles = List.of(
                OutputFile.builder()
                        .filename("file.tiff")
                        .links(Map.of("download", org.springframework.hateoas.Link.of("http://platform/dl/1")))
                        .build(),
                OutputFile.builder()
                        .filename("otherFile.tiff")
                        .links(Map.of("download", org.springframework.hateoas.Link.of("http://platform/dl/20")))
                        .build()
        );

        Link expectedLink = new Link();
        expectedLink.setHref("http://platform/dl/1");
        expectedLink.setRel("download");
        expectedLink.setTitle("Download file.tiff");

        assertThat(toOutput(outputId, output, outputFiles, null)).isEqualTo(
                new InputValueNoObjectArray<>(List.of(expectedLink))
        );
    }

    @Test
    public void testToOutput_ReturnsEmptyList_WhenOutputIsNull() {
        String outputId = "out";
        Map<String, List<String>> output = null;
        List<OutputFile> outputFiles = List.of();
        InputValueNoObjectArray outputList = (InputValueNoObjectArray) toOutput(outputId, output, outputFiles, null);
        assertThat((List<String>) outputList.get()).isEmpty();
    }

    @Test
    public void testToOutput_ReturnsEmptyList_WhenOutputDoesNotContainRequestedOutputId() {
        String outputId = "out";
        Map<String, List<String>> output = Map.of("fakeOutput", List.of("FakeValue"));
        List<OutputFile> outputFiles = List.of();
        InputValueNoObjectArray outputList = (InputValueNoObjectArray) toOutput(outputId, output, outputFiles, null);
        assertThat((List<String>) outputList.get()).isEmpty();
    }

    @Test
    public void testToOutput_MapsOutputFileReferenceToDownloadLink_WhenReferencePathIsEncoded() {
        String outputId = "out";
        String outputFileReference = "eopaas://outputProduct/e9d114d8-81ea-4651-9d67-e43ebf579ad7/out/sample5%20.tif";
        Map<String, List<String>> output = Map.of(outputId, List.of(outputFileReference));
        List<OutputFile> outputFiles = List.of(
                OutputFile.builder()
                        .filename("e9d114d8-81ea-4651-9d67-e43ebf579ad7/out/sample5 .tif")
                        .links(Map.of("download", org.springframework.hateoas.Link.of("http://platform/dl/1")))
                        .build()
        );

        Link expectedLink = new Link();
        expectedLink.setHref("http://platform/dl/1");
        expectedLink.setRel("download");
        expectedLink.setTitle("Download e9d114d8-81ea-4651-9d67-e43ebf579ad7/out/sample5 .tif");

        assertThat(toOutput(outputId, output, outputFiles, null)).isEqualTo(
                new InputValueNoObjectArray<>(List.of(expectedLink))
        );
    }

    @Test
    public void testToOutput_RewritesBaseUrlInDownloadHrefToTargetBaseUrl_WhenTargetBaseUrlIsProvided() {
        String outputId = "out";

        Map<String, List<String>> outputs = Map.of(
                outputId, List.of("eopaas://ref/file.tiff")
        );

        List<OutputFile> outputFiles = List.of(
                OutputFile.builder()
                        .filename("file.tiff")
                        .links(Map.of("download",
                                org.springframework.hateoas.Link.of("http://eopaas-int-server:8090/secure/api/v2.0/files/1?token=abc#frag")))
                        .build()
        );

        Link expectedLink = new Link();
        expectedLink.setHref("https://int.insula.earth/secure/api/v2.0/files/1?token=abc#frag");
        expectedLink.setRel("download");
        expectedLink.setTitle("Download file.tiff");

        URI targetBaseUri = URI.create("https://int.insula.earth");
        InlineOrRefData actual = toOutput(outputId, outputs, outputFiles, targetBaseUri);
        assertThat(actual).isInstanceOf(InputValueNoObjectArray.class);
        InputValueNoObjectArray<InlineOrRefData> actualArray = (InputValueNoObjectArray<InlineOrRefData>) actual;
        assertThat(actualArray).isEqualTo(new InputValueNoObjectArray<>(List.of(expectedLink)));
    }

    @Test
    public void testToOutput_RewritesBaseUrlInDownloadHrefToTargetBaseUrl_WhenTargetBaseUrlIsProvidedAndOriginalHrefUriHasTemplate() {
        String outputId = "out";

        Map<String, List<String>> outputs = Map.of(
                outputId, List.of("eopaas://ref/file.tiff")
        );

        List<OutputFile> outputFiles = List.of(
                OutputFile.builder()
                        .filename("file.tiff")
                        .links(Map.of("download",
                                org.springframework.hateoas.Link.of("https://eopaas-int-server:8090/secure/api/v2.0/platformFiles/4864{?projection}/dl?token=abc#frag")))
                        .build()
        );
        Link expectedLink = new Link();
        expectedLink.setHref("http://int.insula.earth/secure/api/v2.0/platformFiles/4864{?projection}/dl?token=abc#frag");
        expectedLink.setRel("download");
        expectedLink.setTitle("Download file.tiff");


        URI targetBaseUri = URI.create("http://int.insula.earth");
        InlineOrRefData actual = toOutput(outputId, outputs, outputFiles, targetBaseUri);
        assertThat(actual).isInstanceOf(InputValueNoObjectArray.class);
        InputValueNoObjectArray<InlineOrRefData> actualArray = (InputValueNoObjectArray<InlineOrRefData>) actual;
        assertThat(actualArray).isEqualTo(new InputValueNoObjectArray<>(List.of(expectedLink)));
    }

    @Test
    public void testToOutput_DoesNotRewriteDownloadHref_WhenTargetBaseUrlIsNull() {
        String outputId = "out";

        Map<String, List<String>> outputs = Map.of(
                outputId, List.of("eopaas://ref/file.tiff")
        );

        List<OutputFile> outputFiles = List.of(
                OutputFile.builder()
                        .filename("file.tiff")
                        .links(Map.of("download", org.springframework.hateoas.Link.of("http://eopaas-int-server:8090/secure/api/v2.0/files/1")))
                        .build()
        );
        Link expectedLink = new Link();
        expectedLink.setHref("http://eopaas-int-server:8090/secure/api/v2.0/files/1");
        expectedLink.setRel("download");
        expectedLink.setTitle("Download file.tiff");

        InlineOrRefData actual = toOutput(outputId, outputs, outputFiles, null);
        assertThat(actual).isInstanceOf(InputValueNoObjectArray.class);
        InputValueNoObjectArray<InlineOrRefData> actualArray = (InputValueNoObjectArray<InlineOrRefData>) actual;
        assertThat(actualArray).isEqualTo(new InputValueNoObjectArray<>(List.of(expectedLink)));
    }

    @Test
    public void testToOutput_DoesNotRewriteDownloadHref_WhenOriginalHrefIsNull() {
        String outputId = "out";

        Map<String, List<String>> outputs = Map.of(
                outputId, List.of("eopaas://ref/file.tiff")
        );

        org.springframework.hateoas.Link downloadLink = mock(org.springframework.hateoas.Link.class);

        when(downloadLink.getHref()).thenReturn(null);

        List<OutputFile> outputFiles = List.of(
                OutputFile.builder()
                        .filename("file.tiff")
                        .links(Map.of("download", downloadLink))
                        .build()
        );

        Link expectedLink = new Link();
        expectedLink.setHref(null);
        expectedLink.setRel("download");
        expectedLink.setTitle("Download file.tiff");

        URI targetBaseUri = URI.create("http://int.insula.earth");
        InlineOrRefData actual = toOutput(outputId, outputs, outputFiles, targetBaseUri);
        assertThat(actual).isInstanceOf(InputValueNoObjectArray.class);
        InputValueNoObjectArray<InlineOrRefData> actualArray = (InputValueNoObjectArray<InlineOrRefData>) actual;
        assertThat(actualArray).isEqualTo(new InputValueNoObjectArray<>(List.of(expectedLink)));
    }

    private InsulaJobMapper insulaJobMapperWithJobLinkMappingEnabled() {
        return new InsulaJobMapper(true);
    }

    private InsulaJobMapper insulaJobMapperWithJobLinkMappingDisabled() {
        return new InsulaJobMapper(false);
    }
}