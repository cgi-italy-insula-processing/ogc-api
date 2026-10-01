package com.cgi.eoss.ogcapi.processes.services;

import com.cgi.eoss.ogcapi.processes.controllers.JobsApi;
import com.cgi.eoss.ogcapi.processes.controllers.JobsApiDelegate;
import com.cgi.eoss.ogcapi.processes.insula.InsulaApi;
import com.cgi.eoss.ogcapi.processes.insula.exception.InsulaApiException;
import com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobFindResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobGetResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.StacSearchResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobConfigGetResponse;
import com.cgi.eoss.ogcapi.processes.model.InlineOrRefData;
import com.cgi.eoss.ogcapi.processes.model.JobList;
import com.cgi.eoss.ogcapi.processes.model.StatusCode;
import com.cgi.eoss.ogcapi.processes.model.StatusInfo;
import com.cgi.eoss.ogcapi.processes.model.Metadata;
import com.cgi.eoss.ogcapi.processes.model.Metadata2;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.Exception;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.net.URI;

/**
 * Service that implements the {@link JobsApiDelegate} interface and holds the logic for the OGC API Jobs
 * endpoints.
 */
@Service
@Log4j2
public class JobsApiDelegateImpl implements JobsApiDelegate {

    private static final String APPLICATION_GEO_JSON = "application/geo+json";

    private static final String PLATFORM_PRODUCTS = "PLATFORM_PRODUCTS";

    private final InsulaApi insulaApi;

    private final NativeWebRequest nativeWebRequest;

    private final RequestBaseUriResolver requestBaseUriResolver;

    private final InsulaJobMapper insulaJobMapper;

    private final boolean searchApiEnabled;

    @Autowired
    public JobsApiDelegateImpl(InsulaApi insulaApi, NativeWebRequest nativeWebRequest,
                               RequestBaseUriResolver requestBaseUriResolver, InsulaJobMapper insulaJobMapper,
                               @Value("${ogcapi.processes.insula.searchApi.enabled:true}") boolean searchApiEnabled) {
        this.insulaApi = insulaApi;
        this.nativeWebRequest = nativeWebRequest;
        this.requestBaseUriResolver = requestBaseUriResolver;
        this.insulaJobMapper = insulaJobMapper;
        this.searchApiEnabled = searchApiEnabled;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(nativeWebRequest);
    }

    @Override
    public ResponseEntity<StatusInfo> getStatus(String jobId) {
        checkValidId(jobId);
        LOG.info("Get Status for Job {}", jobId);
        JobGetResponse jobGetResponse;
        try {
            jobGetResponse = insulaApi.getJob(Long.valueOf(jobId));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }
        URI targetBaseUri = requestBaseUriResolver.resolveFrom(nativeWebRequest);
        List<Link> links = insulaJobMapper.buildGetJobStatusLinks(jobId, jobGetResponse, targetBaseUri);

        JobConfigGetResponse jobConfigGetResponse = null;
        try {
            jobConfigGetResponse = insulaApi.getJobConfig(Long.valueOf(jobId));
        } catch (Exception e) {
            LOG.debug("Could not load job config for docker metadata, jobId={}", jobId, e);
        }

        StatusInfo statusInfo = InsulaJobMapper.toStatusInfo(jobGetResponse, links, jobGetResponse.getServiceId(), buildJobMetadata(jobConfigGetResponse));

        LOG.info("Successfully retrieved Status for Job {}", jobId);
        return ResponseEntity.ok(statusInfo);
    }

    @Override
    public ResponseEntity<JobList> getJobs(Integer limit, Integer offset, List<String> processID, List<StatusCode> status,
                                           Integer minDuration, Integer maxDuration, List<String> type,
                                           String datetime) {
        if (hasUnsupportedRequestParams(processID, minDuration, maxDuration, type)) {
            LOG.error("Request contains unsupported parameters.");
            return ResponseEntity.status(HttpStatusCode.valueOf(501)).build();
        }
        String processId = Optional.ofNullable(processID)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
        List<JobResponse.Status> statuses = Optional.ofNullable(status)
                .map(list -> list.stream()
                        .map(InsulaJobMapper::toStatus)
                        .collect(Collectors.toList()))
                .orElse(null);
        JobFindResponse jobFindResponse = getJobsAggregated(offset,limit,processId,statuses,datetime);
        List<Link> links = buildLinks(limit, offset, processID, status, datetime, jobFindResponse);
        LOG.info("Retrieved jobs (process filter={})", processId);
        return ResponseEntity.ok(InsulaJobMapper.toJobList(jobFindResponse, links));
    }

    @Override
    public ResponseEntity<Map<String, InlineOrRefData>> getResult(String jobId) {
        checkValidId(jobId);
        LOG.info("Get results for Job {}", jobId);
        JobGetResponse jobGetResponse;

        try {
            jobGetResponse = insulaApi.getJob(Long.valueOf(jobId));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }
        return ResponseEntity.ok(InsulaJobMapper.toOutputResults(jobId, jobGetResponse.getOutputs()));
    }

    @Override
    public ResponseEntity<Object> getOutputResult(String jobId, String outputId) {
        checkValidId(jobId);
        LOG.info("Get output {} for Job {}", outputId, jobId);
        String acceptHeader = getRequest().get().getHeader("Accept");

        JobGetResponse jobGetResponse;

        try {
            jobGetResponse = insulaApi.getJob(Long.valueOf(jobId));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        if (APPLICATION_GEO_JSON.equals(acceptHeader)) {
            return ResponseEntity.ok(stacOutputResult(jobGetResponse, outputId));
        }

        URI targetBaseUri  = requestBaseUriResolver.resolveFrom(nativeWebRequest);
        InlineOrRefData result = linkOutputResult(jobGetResponse, outputId, targetBaseUri);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<StatusInfo> dismiss(String jobId) {
        checkValidId(jobId);
        LOG.info("Dismiss Job {}", jobId);
        JobGetResponse jobGetResponse;

        Long jobIdAsLong = Long.valueOf(jobId);
        try {
            jobGetResponse = insulaApi.getJob(jobIdAsLong);
            cancelOrTerminate(jobIdAsLong, InsulaJobMapper.toStatusCode(jobGetResponse.getStatus()));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Successfully dismissed Job {}", jobId);
        StatusInfo statusInfo = InsulaJobMapper.toStatusInfo(jobGetResponse,
            List.of(
                linkTo(methodOn(JobsApi.class).dismiss(jobId))
                    .withSelfRel()
                    .withTitle("Dismiss Job")
                    .withType("application/json")
            ),
            jobGetResponse.getServiceId(),
            List.of()
        );
        statusInfo.setMessage("Job dismission requested");
        return ResponseEntity.ok(statusInfo);
    }

    private static List<Metadata> buildJobMetadata(JobConfigGetResponse jobConfigGetResponse) {
        List<Metadata> metadata = new ArrayList<>();

        if (jobConfigGetResponse == null) {
            return metadata;
        }

        String dockerTag = jobConfigGetResponse.getEmbedded().getService().getDockerTag();
        if (dockerTag == null || dockerTag.isBlank()) {
            return metadata;
        }

        metadata.add(new Metadata2()
                .title("dockerTag")
                .role("runtime")
                .value(dockerTag));

        return metadata;
    }

    private void cancelOrTerminate(Long jobId, StatusCode status) {
        if (StatusCode.ACCEPTED.equals(status)) {
            LOG.info("Cancelling job {}", jobId);
            insulaApi.cancelJob(jobId);
            return;
        }
        LOG.info("Terminating job {}", jobId);
        insulaApi.terminateJob(jobId);
    }

    private InlineOrRefData linkOutputResult(JobGetResponse jobGetResponse, String outputId, URI targetBaseUri) {
        return InsulaJobMapper.toOutput(outputId, jobGetResponse.getOutputs(), jobGetResponse.getOutputFiles(), targetBaseUri);
    }

    private StacSearchResponse stacOutputResult(JobGetResponse jobGetResponse, String outputId) {
        if (searchApiEnabled) {
            return getJobOutputResultFromSearch(
                    jobGetResponse.getCollectionId(outputId),
                    jobGetResponse.getOutputFilesIdentifier(outputId));
        }
        return getJobOutputResult(jobGetResponse.getId(), outputId);
    }

    private StacSearchResponse getJobOutputResultFromSearch(String collectionId, String outputFilesIdentifier) {
        return insulaApi.stacSearch(PLATFORM_PRODUCTS, collectionId, outputFilesIdentifier);
    }

    private StacSearchResponse getJobOutputResult(Long jobId, String outputId) {
        return insulaApi.getJobOutputs(jobId, outputId);
    }

    private static boolean hasUnsupportedRequestParams(List<String> processID, Integer minDuration,
                                                       Integer maxDuration, List<String> type) {
        return minDuration != null || maxDuration != null || type != null ||
                (processID != null && processID.size() != 1);
    }

    private static void checkValidId(String id) {
        try {
            Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID " + id + " is not a valid number");
        }
    }

    private JobFindResponse getJobsAggregated(int offset, int limit, String processId,
                                             List<JobResponse.Status> statuses, String datetime) {
        int startPage = offset / limit;
        int startIndex = offset % limit;

        JobFindResponse firstPageResponse = getJobs(limit, startPage, processId, statuses, datetime);
        List<JobGetResponse> jobs = new ArrayList<>(extractJobsFromStartIndex(firstPageResponse, startIndex));
        int currentPage = startPage + 1;

        JobFindResponse lastPageResponse = firstPageResponse;

        lastPageResponse = appendRemainingJobsIfPresent(limit, processId, statuses, datetime, currentPage, jobs, lastPageResponse);
        return buildJobFindResponse(lastPageResponse, jobs);
    }

    private JobFindResponse getJobs(int limit, int page, String processId,
                                    List<JobResponse.Status> statuses, String datetime) {
        try {
            return insulaApi.findJobs(limit, page, processId, statuses, datetime);
        } catch (InsulaApiException e) {
            LOG.error("API error on page {}: {}", page, e.getHttpStatusCode());
            throw e;
        }
    }

    private List<JobGetResponse> extractJobsFromStartIndex(JobFindResponse response, int startIndex) {
        List<JobGetResponse> jobs = response.getEmbeddedJobs().getJobs();
        if (startIndex >= jobs.size()) {
            return Collections.emptyList();
        }
        return jobs.subList(startIndex, jobs.size());
    }

    private List<JobGetResponse> limitJobs(List<JobGetResponse> jobs, int limit) {
        if (jobs.size() > limit) {
            return jobs.subList(0, limit);
        } else {
            return jobs;
        }
    }

    private JobFindResponse appendRemainingJobsIfPresent(int limit, String processId, List<JobResponse.Status> statuses, String datetime,
                                                         int currentPage, List<JobGetResponse> jobs, JobFindResponse lastPageResponse) {
        int totalPages = getTotalPages(lastPageResponse);
        while (currentPage < totalPages && jobs.size() < limit) {
            JobFindResponse nextPageResponse = getJobs(limit, currentPage, processId, statuses, datetime);
            List<JobGetResponse> nextPageJobs = nextPageResponse.getEmbeddedJobs().getJobs();

            int remaining = limit - jobs.size();
            jobs.addAll(limitJobs(nextPageJobs, remaining));

            currentPage++;
            lastPageResponse = nextPageResponse;  // update last page response
        }
        return lastPageResponse;
    }

    private int getTotalPages(JobFindResponse response) {
        Map<String, Object> page = response.getPage();
        return (Integer) page.get("totalPages");
    }

    private JobFindResponse buildJobFindResponse(JobFindResponse lastResponse, List<JobGetResponse> jobs) {
        JobFindResponse.EmbeddedJobs newEmbeddedJobs = JobFindResponse.EmbeddedJobs.builder()
                .jobs(jobs)
                .build();

        return JobFindResponse.builder()
                .embeddedJobs(newEmbeddedJobs)
                .page(lastResponse.getPage())
                .build();
    }

    private static List<Link> buildLinks(Integer limit, Integer offset, List<String> processID, List<StatusCode> status, String datetime, JobFindResponse jobFindResponse) {
        Map<String, Object> pageInfo = jobFindResponse.getPage();
        int currentPage = (int) pageInfo.getOrDefault("number", 0);
        int totalPages = (int) pageInfo.getOrDefault("totalPages", 0);

        int nextOffset = offset + limit;
        List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(JobsApi.class)
                .getJobs(limit, offset, processID, status, null, null, null, datetime))
                .withSelfRel()
                .withTitle("Get Jobs")
                .withType("application/json"));

        if (currentPage + 1 < totalPages) {
            links.add(linkTo(methodOn(JobsApi.class)
                    .getJobs(limit, nextOffset, processID, status, null, null, null, datetime))
                    .withRel("next")
                    .withTitle("Next page")
                    .withType("application/json"));
        }
        return links;
    }

}
