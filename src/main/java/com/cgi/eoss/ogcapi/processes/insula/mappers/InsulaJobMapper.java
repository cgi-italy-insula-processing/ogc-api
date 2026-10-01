package com.cgi.eoss.ogcapi.processes.insula.mappers;

import com.cgi.eoss.ogcapi.processes.controllers.JobsApi;
import com.cgi.eoss.ogcapi.processes.controllers.ProcessesApi;
import com.cgi.eoss.ogcapi.processes.insula.model.OutputFile;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobConfigCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobFindResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobGetResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse;
import com.cgi.eoss.ogcapi.processes.model.*;
import com.cgi.eoss.ogcapi.processes.model.inputs.InputValueNoObjectArray;
import com.cgi.eoss.ogcapi.processes.util.UrlRewriteUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.*;
import static com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse.Status.RUNNING;
import static com.cgi.eoss.ogcapi.processes.model.StatusCode.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Log4j2
@RequiredArgsConstructor
public class InsulaJobMapper extends InsulaOgcMapper {

    private final static String SERVICES_PATH = "/services";

    private final static String DATETIME_HALF_BOUNDED_INTERVAL = "..";

    private final static String DATETIME_SPLITTER = "/";

    private final boolean isJobApiMappingEnabled;

    /**
     * Maps input arguments to a {@link JobConfigCreationRequest} object.
     * @param baseUrl the base url of the request.
     * @param processId the ID of the process.
     * @param inputs the inputs of the process.
     * @return a JobConfigCreationRequest mapped from the input arguments.
     */
    public static JobConfigCreationRequest toJobConfigCreationRequest(String baseUrl, Long processId, Map<String, Input> inputs) {
        return JobConfigCreationRequest.builder()
                .service(getServiceUri(baseUrl, processId))
                .inputs(buildInputsMultiMap(inputs))
                .build();
    }

    /**
     * Maps a {@link JobGetResponse} object to a {@link StatusInfo} object.
     * @param jobGetResponse the JobGetResponse object.
     * @param links the list of links to be added to the StatusInfo object.
     * @param metadata the list of metadata entries to be added to the StatusInfo object
     * @return a {@code StatusInfo} object.
     */
    public static StatusInfo toStatusInfo(JobGetResponse jobGetResponse, List<Link> links, Long processId, List<Metadata> metadata) {
        StatusInfo statusInfo = toStatusInfo((JobResponse) jobGetResponse, links, processId, metadata);

        statusInfo.setTitle(jobGetResponse.getServiceName());
        statusInfo.setMessage(toSubJobStatusCodeCountsMessage(jobGetResponse.getSubJobStatusCounts()));
        return statusInfo;
    }

    /**
     * Maps a JobResponse object to a StatusInfo object.
     * @param jobResponse the JobResponse object.
     * @param links the list of links to be added to the StatusInfo object.
     * @param metadata the list of metadata entries to be added to the StatusInfo object
     * @return a StatusInfo object.
     */
    public static StatusInfo toStatusInfo(JobResponse jobResponse, List<Link> links, Long processId, List<Metadata> metadata) {
        StatusInfo statusInfo = new StatusInfo();
        statusInfo.setId(String.valueOf(jobResponse.getId()));
        statusInfo.setProcessID(
                linkTo(methodOn(ProcessesApi.class).getProcessDescription(processId))
                        .withSelfRel().expand().toUri()
        );
        OffsetDateTime created = jobResponse.getCreated();
        if (created != null) {
            statusInfo.setCreated(created);
        }
        OffsetDateTime lastUpdated = jobResponse.getLastUpdated();
        if (lastUpdated != null) {
            statusInfo.setUpdated(lastUpdated);
        }
        LocalDateTime started = jobResponse.getStartTime();
        if (started != null) {
            statusInfo.setStarted(started.atOffset(ZoneOffset.UTC));
        }
        LocalDateTime finished = jobResponse.getEndTime();
        if (finished != null) {
            statusInfo.setFinished(finished.atOffset(ZoneOffset.UTC));
        }
        statusInfo.setStatus(toStatusCode(jobResponse.getStatus()));
        statusInfo.setProgress(getProgress(jobResponse.getPhase()));
        statusInfo.setLinks(buildOgcLinks(links));

        statusInfo.setMetadata(metadata != null ? metadata : List.of());

        return statusInfo;
    }

    /**
     * Builds the path of an HTTP request with the given input parameters as query parameters.
     * @param projection the projection parameter value.
     * @param limit the size of the request.
     * @param processID the service name parameter value.
     * @param status the list of status.
     * @param datetime the datetime parameter value.
     * @return the resulting path of the HTTP request with the input query parameters.
     */
    public static String withQueryParameters(String path, String projection, Integer limit, Integer page,
                                             String processID, List<JobResponse.Status> status, String datetime) {
        UriComponentsBuilder builder =  UriComponentsBuilder.fromPath(path);
        if (projection != null) {
            builder.queryParam("projection", projection);
        }
        if (limit != null) {
            builder.queryParam("size", String.valueOf(limit));
            builder.queryParam("sort", "id");
        }
        if (page != null) {
            builder.queryParam("page", String.valueOf(page));
        }
        if (processID != null) {
            builder.queryParam("filter", processID);
        }
        String statuses = status != null
                ? status.stream().map(Enum::toString).collect(Collectors.joining(","))
                : null;
        if (statuses != null) {
            builder.queryParam("status", statuses);
        }
        mapDateTime(builder, datetime);
        return builder.build().toUriString();
    }

    /**
     * Maps a StatusCode enum to the corresponding JobResponse Status.
     * @param status the StatusCode enum.
     * @return the JobResponse Status enum.
     */
    public static JobResponse.Status toStatus(StatusCode status) {
        if (status == null) {
            LOG.warn("Received a Job with NULL status.");
            return JobResponse.Status.CANCELLED;
        }
        return switch (status) {
            case ACCEPTED -> CREATED;
            case RUNNING -> RUNNING;
            case SUCCESSFUL -> COMPLETED;
            case FAILED -> ERROR;
            case DISMISSED -> CANCELLED;
        };
    }

    /**
     * Maps a JobResponse Status enum to the corresponding StatusCode enum.
     * @param status the JobResponse Status enum.
     * @return the StatusCode enum.
     */
    public static StatusCode toStatusCode(JobResponse.Status status) {
        if (status == null) {
            LOG.warn("Received a Job with NULL status.");
            return DISMISSED;
        }
        return switch (status) {
            case CREATED, PENDING, WAITING, CONDITION_WAIT -> ACCEPTED;
            case RUNNING -> StatusCode.RUNNING;
            case COMPLETED -> SUCCESSFUL;
            case ERROR -> FAILED;
            case CANCELLED -> DISMISSED;
        };
    }

    /**
     * Maps a JobFindResponse object to a JobList object.
     * @param jobFindResponse the JobFindResponse object.
     * @param link the link to the document that produced the JobFindResponse.
     * @return the JobList object.
     */
    public static JobList toJobList(JobFindResponse jobFindResponse, List<Link> link) {
        JobList jobList = new JobList();
        List<JobGetResponse> jobResponseList = jobFindResponse.getEmbeddedJobs().getJobs();
        for (JobGetResponse job : jobResponseList) {
            jobList.addJobsItem(
                    toStatusInfo(
                            job,
                            List.of(
                                    linkTo(methodOn(JobsApi.class).getStatus(String.valueOf(job.getId())))
                                            .withSelfRel()
                                            .withTitle("Job Status")
                                            .withType("application/json")
                            ),
                            job.getServiceId(),
                            List.of()
                    )
            );
        }
        jobList.links(buildOgcLinks(link));
        return jobList;
    }

    /**
     * Maps a collection of outputs for the given job, to a map of links for each of such outputs.
     * @param jobId the ID of the job the outputs belong to.
     * @param output the collection of outputs of the given job.
     * @return a collection of links to retrieve each output.
     */
    public static Map<String, InlineOrRefData> toOutputResults(String jobId, Map<String, List<String>> output) {
        if (output == null) {
            return Collections.emptyMap();
        }
        Map<String, InlineOrRefData> outputResults = new HashMap<>();
        for (String outputId : output.keySet()) {
            com.cgi.eoss.ogcapi.processes.model.Link ogcLink = new com.cgi.eoss.ogcapi.processes.model.Link();
            ogcLink.setHref(linkTo(methodOn(JobsApi.class).getOutputResult(jobId, outputId)).toUri().toASCIIString());
            ogcLink.setRel("output");
            ogcLink.setTitle(outputId);
            outputResults.put(outputId, ogcLink);
        }
        return outputResults;
    }

    /**
     * Maps a collection of output files to a list of links through which the items for the given output ID
     * can be downloaded.
     *
     * If {@code targetBaseUri} is provided, the download {@code href} is rewritten to use the target base
     * scheme/host while preserving path, query and fragment.
     *
     * @param outputId the ID of the requested output.
     * @param outputs the collection of job's outputs.
     * @param outputFiles a list of output files.
     * @param targetBaseUri the target base URI to use for rewriting download links.
     * @return a list of links to download the requested output files.
     */
    public static InlineOrRefData toOutput(String outputId,
                                           Map<String, List<String>> outputs,
                                           List<OutputFile> outputFiles,
                                           URI targetBaseUri) {
        if (outputs == null || !outputs.containsKey(outputId)) {
            return new InputValueNoObjectArray<>(Collections.emptyList());
        }
        List<String> outputFileReferences = outputs.get(outputId);
        List<InlineOrRefData> outputList = new ArrayList<>();
        for (String outputFileReference : outputFileReferences) {
            Optional<OutputFile> outputFile = findOutputFile(outputFileReference, outputFiles);
            outputFile.ifPresent(file -> outputList.add(getOutputFileLink(file, targetBaseUri)));
        }
        return new InputValueNoObjectArray<>(outputList);
    }

    /**
     * Builds the Standard OGC self link + optional Insula extension links for parent/children.
     *
     * @param jobId the OGC job id used to build the {@code self} link
     * @param jobGetResponse the Insula job payload used to read optional {@code parentJob}/{@code subJobs} links
     * @param targetBaseUri the public base URI used to rewrite Insula hrefs (avoid internal k8s host).
     */
    public List<Link> buildGetJobStatusLinks(String jobId, JobGetResponse jobGetResponse, URI targetBaseUri) {
        List<Link> links = new ArrayList<>();

        links.add(
                linkTo(methodOn(JobsApi.class).getStatus(jobId))
                        .withSelfRel()
                        .withTitle("Job Status")
                        .withType("application/json")
        );

        Map<String, List<Link>> insulaLinks = jobGetResponse != null ? jobGetResponse.getLinks() : null;
        if (insulaLinks == null || insulaLinks.isEmpty()) {
            return links;
        }

        if (isJobApiMappingEnabled) {
            addParentHref(links, insulaLinks, targetBaseUri);
            addChildJobHref(links, insulaLinks, targetBaseUri);
        }

        return links;
    }

    private static String toSubJobStatusCodeCountsMessage(Map<JobResponse.Status, Integer> subJobStatusCounts) {
        Map<StatusCode, Integer> subJobStatusCodeCounts = toSubJobStatusCodeCounts(subJobStatusCounts);

        if (subJobStatusCodeCounts.isEmpty()) {
            return null;
        }

        return "Subjobs: " + subJobStatusCodeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().name() + ": " + entry.getValue())
                .collect(Collectors.joining(" - "));
    }

    private static Map<StatusCode, Integer> toSubJobStatusCodeCounts(Map<JobResponse.Status, Integer> subJobStatusCounts) {
        if (subJobStatusCounts == null || subJobStatusCounts.isEmpty()) {
            return Map.of();
        }

        return subJobStatusCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> toStatusCode(entry.getKey()),
                        Map.Entry::getValue,
                        Integer::sum));
    }

    private static void addParentHref(List<Link> links, Map<String, List<Link>> insulaLinks, URI targetBaseUri) {
        Link parentLink = firstLink(insulaLinks.get("parentJob"));
        if (parentLink == null || parentLink.getHref().isBlank()) {
            return;
        }

        String href = (targetBaseUri == null)
                ? parentLink.getHref()
                : UrlRewriteUtils.rewriteBaseUrl(parentLink.getHref(), targetBaseUri);

        links.add(Link.of(href)
                .withRel("insula-parent-job")
                .withTitle("Insula Parent Job")
                .withType("application/json"));
    }

    private static void addChildJobHref(List<Link> links, Map<String, List<Link>> insulaLinks, URI targetBaseUri) {
        Link subJobsLink = firstLink(insulaLinks.get("subJobs"));
        if (subJobsLink == null || subJobsLink.getHref().isBlank()) {
            return;
        }

        String href = (targetBaseUri == null)
                ? subJobsLink.getHref()
                : UrlRewriteUtils.rewriteBaseUrl(subJobsLink.getHref(), targetBaseUri);

        links.add(Link.of(href)
                .withRel("insula-child-jobs")
                .withTitle("Insula Child Jobs")
                .withType("application/json"));
    }

    private static Link firstLink(List<Link> links) {
        return (links == null || links.isEmpty()) ? null : links.get(0);
    }

    private static Optional<OutputFile> findOutputFile(String outputFileReference, List<OutputFile> outputFiles) {
        String decodedOutputFileReference = decodeUriPath(outputFileReference);

        return outputFiles.stream()
                .filter(file -> decodedOutputFileReference.contains(file.getFilename()))
                .findAny();
    }

    private static String decodeUriPath(String outputFileReference) {
        return URI.create(outputFileReference).getPath();
    }

    private static InlineOrRefData getOutputFileLink(OutputFile outputFile, URI targetBaseUri) {
        com.cgi.eoss.ogcapi.processes.model.Link ogcLink = new com.cgi.eoss.ogcapi.processes.model.Link();

        String insulaHref = outputFile.getLinks().get("download").getHref();
        String href = targetBaseUri != null
                ? UrlRewriteUtils.rewriteBaseUrl(insulaHref, targetBaseUri)
                : insulaHref;
        ogcLink.setHref(href);
        ogcLink.setRel("download");
        ogcLink.setTitle("Download " + outputFile.getFilename());
        return ogcLink;
    }

    private static void mapDateTime(UriComponentsBuilder builder, String dateTime) {
        if (dateTime == null) {
            return;
        }
        if (!dateTime.contains(DATETIME_SPLITTER)) {
            builder.queryParam("startTime", dateTime);
            return;
        }
        String[] splitDateTime = dateTime.split(DATETIME_SPLITTER);
        String startTime = splitDateTime[0];
        String endTime = splitDateTime[1];
        if (!startTime.equals(DATETIME_HALF_BOUNDED_INTERVAL)) {
            builder.queryParam("startTime", startTime);
        }
        if (!endTime.equals(DATETIME_HALF_BOUNDED_INTERVAL)) {
            builder.queryParam("endTime", endTime);
        }
    }

    private static URI getServiceUri(String baseUrl, Long processId) {
        return URI.create(baseUrl.concat(SERVICES_PATH)+"/"+processId).normalize();
    }

    private static Multimap<String, Object> buildInputsMultiMap(Map<String, Input> inputs) {
        Multimap<String, Object> inputsMultiMap = ArrayListMultimap.create();
        if (inputs == null) {
            return inputsMultiMap;
        }
        for (Map.Entry<String, Input> entry : inputs.entrySet()) {
            String inputId = entry.getKey();
            Input input = entry.getValue();
            Object value = unwrapInput(input);
            if (value == null) {
                LOG.debug("Got input {} with null value", inputId);
                continue;
            }
            LOG.debug("Got input {} with type {} and value {}", inputId, value.getClass(), value);

            if (isFlattenableArrayInput(input, value)) {
                addFlattenedArrayValues(inputsMultiMap, inputId, (Iterable<?>) value);
            } else {
                inputsMultiMap.put(inputId, value);
            }
        }
        return inputsMultiMap;
    }

    private static boolean isFlattenableArrayInput(Input input, Object value) {
        return input instanceof InputValueNoObjectArray<?> && value instanceof Iterable<?>;
    }

    private static void addFlattenedArrayValues(Multimap<String, Object> inputsMultiMap,
                                                String inputId,
                                                Iterable<?> iterable) {

        for (Object element : iterable) {
            Object unwrappedValue = unwrapValue(element);
            if (shouldSkipFlattenedValue(unwrappedValue)) {
                continue;
            }
            inputsMultiMap.put(inputId, unwrappedValue);
        }
    }

    private static Object unwrapValue(Object value) {
        if (value instanceof Input input) {
            return unwrapInput(input);
        }
        return value;
    }

    private static Object unwrapInput(Input inputValue) {
        if (inputValue instanceof InputValueNoObjectOneOf<?>) {
            return ((InputValueNoObjectOneOf<?>) inputValue).get();
        }
        return inputValue;
    }

    private static boolean shouldSkipFlattenedValue(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }

    private static Integer getProgress(JobResponse.Phase phase) {
        if (phase == null) {
            LOG.warn("Received a Job with NULL phase.");
            return 0;
        }
        return switch (phase) {
            case CREATED -> 25;
            case DATA_FETCH -> 50;
            case PROCESSING -> 75;
            case OUTPUT_LIST -> 100;
        };
    }
}