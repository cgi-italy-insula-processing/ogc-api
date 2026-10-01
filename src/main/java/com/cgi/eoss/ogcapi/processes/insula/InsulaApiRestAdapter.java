package com.cgi.eoss.ogcapi.processes.insula;

import com.cgi.eoss.ogcapi.processes.insula.exception.InsulaApiException;
import com.cgi.eoss.ogcapi.processes.insula.exception.ServiceNotFoundException;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobConfigCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobLaunchRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.request.ServiceCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.GetServicesResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobConfigCreationResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobConfigGetResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobFindResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobGetResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobLaunchResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.ServiceResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.StacSearchResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.withQueryParameters;

/**
 * Adapter that acts as a client to interact with Insula through its APIs.
 */
@Component
@Log4j2
public class InsulaApiRestAdapter implements InsulaApi {

    private final static String SERVICES_PATH = "/services";

    private final static String SERVICES_SEARCH_PARAMETRIC_FIND_PATH = "/services/search/parametricFind";

    private final static String JOB_CONFIGS_PATH = "/jobConfigs";

    private final static String JOB_CONFIGS_LAUNCH_PATH = JOB_CONFIGS_PATH.concat("/%s/launch");

    private final static String ESTIMATE_COST_JOB_CONFIG_PATH = "/estimateCost/jobConfig/%s";

    private final static String JOB_CONFIGS_WITH_ID_PATH = JOB_CONFIGS_PATH.concat("/%s");

    private final static String JOBS_PATH = "/jobs";

    private final static String JOBS_GET_OUTPUTS_PATH = JOBS_PATH.concat("/{jobId}/outputs/{outputId}");

    private final static String PARAMETRIC_FIND_PATH = "/search/parametricFind";

    private final static String DETAILED_JOB = "detailedJob";

    private final static String STAC_SEARCH_PATH = "/search";

    private final RestTemplate insulaRestTemplate;
    private final boolean costEstimateEnabled;

    @Autowired
    public InsulaApiRestAdapter(RestTemplate insulaRestTemplate,
                                @Value("${ogcapi.processes.job.costEstimate.enabled:true}") boolean costEstimateEnabled) {
        this.insulaRestTemplate = insulaRestTemplate;
        this.costEstimateEnabled = costEstimateEnabled;
    }

    @Override
    public ServiceResponse create(ServiceCreationRequest serviceCreationRequest){
        String reference = serviceCreationRequest.getCwl().url();
        LOG.info("Requesting creation of Service from reference {}", reference);
        ResponseEntity<ServiceResponse> response;
        try {
            response = insulaRestTemplate
                    .postForEntity(SERVICES_PATH, serviceCreationRequest, ServiceResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.POST, SERVICES_PATH);
        }
        LOG.info("Successfully created Service from reference {} on Insula", reference);
        return response.getBody();
    }

    @Override
    public ServiceResponse getService(Long serviceId) {
        LOG.info("Requesting Service with ID {}", serviceId);
        String getServicePath = SERVICES_PATH.concat("/"+serviceId);
        ResponseEntity<ServiceResponse> response;

        try {
            response = insulaRestTemplate.getForEntity(getServicePath, ServiceResponse.class);
        } catch (HttpStatusCodeException e) {
            if(e.getStatusCode()==HttpStatus.NOT_FOUND) {
                throw logAndBuildServiceNotFoundException(HttpStatus.NOT_FOUND, HttpMethod.GET, getServicePath);
            }
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, getServicePath);
        }

        if(response.getBody().getStatus().equalsIgnoreCase("DISABLED")) {
            throw logAndBuildServiceNotFoundException(HttpStatus.NOT_FOUND, HttpMethod.GET, getServicePath);
        }

        LOG.info("Successfully retrieved Service with ID {}", serviceId);
        return response.getBody();
    }

    @Override
    public GetServicesResponse getServices(Integer limit) {
        LOG.info("Requesting available Services on Insula");
        ResponseEntity<GetServicesResponse> response;
        String getServicesPath = SERVICES_SEARCH_PARAMETRIC_FIND_PATH.concat("?size=" + limit + "&status=AVAILABLE,IN_DEVELOPMENT&projection=detailedPlatformService");

        try {
            response = insulaRestTemplate.getForEntity(getServicesPath, GetServicesResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, getServicesPath);
        }

        LOG.info("Successfully retrieved all Services");
        return response.getBody();
    }

    @Override
    public JobConfigCreationResponse create(JobConfigCreationRequest jobConfigCreationRequest) {
        URI serviceUri = jobConfigCreationRequest.getService();
        LOG.info("Requesting creation of Job Config from service {}", serviceUri);
        ResponseEntity<JobConfigCreationResponse> response;
        try {
            response = insulaRestTemplate.
                    postForEntity(JOB_CONFIGS_PATH, jobConfigCreationRequest, JobConfigCreationResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.POST, JOB_CONFIGS_PATH);
        }
        JobConfigCreationResponse jobConfigCreationResponse = response.getBody();
        if (costEstimateEnabled) {
            canLaunchJobConfig(jobConfigCreationResponse.getId());
        }
        LOG.info("Successfully created Job Config from service {} on Insula", serviceUri);
        return jobConfigCreationResponse;
    }

    @Override
    public JobLaunchResponse launch(JobLaunchRequest jobLaunchRequest) {
        Long jobConfigId = jobLaunchRequest.getJobConfigId();
        LOG.info("Requesting launch of Job Config {}", jobConfigId);
        String launchJobConfigPath = String.format(JOB_CONFIGS_LAUNCH_PATH, jobConfigId);
        ResponseEntity<JobLaunchResponse> jobLaunchResponse;
        try {
            jobLaunchResponse = insulaRestTemplate.postForEntity(launchJobConfigPath, null, JobLaunchResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.POST, launchJobConfigPath);
        }
        LOG.info("Successfully launched Job Config {}", jobConfigId);
        return jobLaunchResponse.getBody();
    }

    @Override
    public void update(Long serviceId, ServiceCreationRequest serviceCreationRequest) {
        String reference = serviceCreationRequest.getCwl().url();
        LOG.info("Requesting update of Service {} from reference {}", serviceId, reference);
        String updateServicePath = SERVICES_PATH.concat("/"+serviceId);
        try {
            insulaRestTemplate.put(updateServicePath, serviceCreationRequest);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.PUT, updateServicePath);
        }
        LOG.info("Successfully updated Service {} from reference {} on Insula", serviceId, reference);
    }

    @Override
    public void disableService(Long serviceId) {
        // Verify that the service to disable exists, otherwise throws not found.
        getService(serviceId);
        LOG.info("Requesting disabling of Service with ID {}", serviceId);
        String disableServicePath = SERVICES_PATH.concat("/" +serviceId + "/disable");
        try {
            insulaRestTemplate.postForEntity(disableServicePath, null, Void.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.POST, disableServicePath);
        }
        LOG.info("Successfully disabled Service with ID {}", serviceId);
    }

    @Override
    public JobGetResponse getJob(Long jobId) {
        LOG.info("Requesting job with ID {}", jobId);
        String getJobPath = JOBS_PATH.concat("/"+jobId).concat("?projection="+ DETAILED_JOB);
        ResponseEntity<JobGetResponse> jobGetResponse;

        try {
            jobGetResponse = insulaRestTemplate.getForEntity(getJobPath, JobGetResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, getJobPath);
        }

        LOG.info("Successfully received job with ID {}", jobId);
        return jobGetResponse.getBody();
    }

    @Override
    public JobFindResponse findJobs(Integer limit, Integer page, String processID,
                                    List<JobResponse.Status> status, String datetime) {
        LOG.info("Searching jobs with parameters {} {} {} {} {}", limit, page, processID, status, datetime);
        String findJobsPath = withQueryParameters(JOBS_PATH.concat(PARAMETRIC_FIND_PATH),
                DETAILED_JOB, limit, page, processID, status, datetime);
        ResponseEntity<JobFindResponse> jobFindResponse;

        try {
            jobFindResponse = insulaRestTemplate.getForEntity(findJobsPath, JobFindResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, findJobsPath);
        }

        LOG.info("Successfully received jobs for parameters {} {} {} {}", limit, processID, status, datetime);
        return jobFindResponse.getBody();
    }

    @Override
    public StacSearchResponse stacSearch(String catalogue, String collection, String identifier) {
        LOG.info("Performing STAC Search with parameters {} {} {}", catalogue, collection, identifier);
        ResponseEntity<StacSearchResponse> stacSearchResponse;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.parseMediaType("application/geo+json")));
        HttpEntity<HttpHeaders> entityHeaders = new HttpEntity<>(httpHeaders);

        try {
            stacSearchResponse = insulaRestTemplate.exchange(
                    STAC_SEARCH_PATH.concat("?catalogue={catalogue}&collection={collection}&identifier={identifier}"),
                    HttpMethod.GET,
                    entityHeaders,
                    StacSearchResponse.class,
                    Map.of("catalogue", catalogue, "collection", collection, "identifier", identifier)
            );
        } catch(HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, STAC_SEARCH_PATH);
        }

        LOG.info("Successfully performed STAC Search with parameters {} {} {}", catalogue, collection, identifier);
        return stacSearchResponse.getBody();
    }

    @Override
    public void cancelJob(Long jobId) {
        LOG.info("Requesting cancellation of the execution of Job with ID {}", jobId);
        String cancelJobPath = JOBS_PATH.concat("/"+jobId).concat("/cancel");

        try {
            insulaRestTemplate.getForObject(cancelJobPath, Void.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, cancelJobPath);
        }

        LOG.info("Successfully cancelled execution of Job with ID {}", jobId);
    }

    @Override
    public void terminateJob(Long jobId) {
        LOG.info("Requesting termination of Job with ID {}", jobId);
        String terminateJobPath = JOBS_PATH.concat("/"+jobId).concat("/terminate");

        try {
            insulaRestTemplate.postForObject(terminateJobPath, null, Void.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.POST, terminateJobPath);
        }

        LOG.info("Successfully terminated Job with ID {}", jobId);
    }

    @Override
    public JobConfigGetResponse getJobConfig(Long jobId) {
        LOG.info("Requesting Job Config for Job with ID {}", jobId);
        String getJobConfigPath = JOBS_PATH.concat("/" + jobId).concat("/config");
        ResponseEntity<JobConfigGetResponse> jobConfigGetResponse;

        try {
            jobConfigGetResponse = insulaRestTemplate.getForEntity(getJobConfigPath, JobConfigGetResponse.class);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, getJobConfigPath);
        }

        LOG.info("Successfully received Job Config for Job with ID {}", jobId);
        return jobConfigGetResponse.getBody();
    }

    @Override
    public StacSearchResponse getJobOutputs(Long jobId, String outputId) {

        LOG.info("Requesting Job Outputs with parameters: jobId = {} ; outputId = {} ", jobId, outputId);
        ResponseEntity<StacSearchResponse> stacSearchResponse;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.parseMediaType("application/geo+json")));
        HttpEntity<HttpHeaders> entityHeaders = new HttpEntity<>(httpHeaders);

        try {
            stacSearchResponse = insulaRestTemplate.exchange(
                    JOBS_GET_OUTPUTS_PATH,
                    HttpMethod.GET,
                    entityHeaders,
                    StacSearchResponse.class,
                    Map.of("jobId", jobId, "outputId", outputId)
            );
        } catch(HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.GET, JOBS_GET_OUTPUTS_PATH);
        }

        LOG.info("Successfully retrieved Job Outputs with parameters: jobId = {} ; outputId = {} ", jobId, outputId);
        return stacSearchResponse.getBody();
    }

    private void canLaunchJobConfig(Long jobConfigId) {
        LOG.info("Checking if user can launch Job Config {}", jobConfigId);
        String estimateCostPath = String.format(ESTIMATE_COST_JOB_CONFIG_PATH, jobConfigId);
        HttpStatusCode httpStatusCode;
        try {
            httpStatusCode = insulaRestTemplate.getForEntity(estimateCostPath, Void.class).getStatusCode();
        } catch (HttpStatusCodeException e) {
            httpStatusCode = e.getStatusCode();
        }
        if (!httpStatusCode.is2xxSuccessful()) {
            LOG.warn("User cannot launch Job Config {}", jobConfigId);
            deleteJobConfig(jobConfigId);
            throw logAndBuildInsulaApiException(httpStatusCode, HttpMethod.GET, estimateCostPath);
        }
        LOG.info("User can launch Job Config {}", jobConfigId);
    }

    private void deleteJobConfig(Long jobConfigId) {
        LOG.info("Requesting deletion of Job Config {}", jobConfigId);
        String deleteJobConfigPath = String.format(JOB_CONFIGS_WITH_ID_PATH, jobConfigId);
        try {
            insulaRestTemplate.delete(deleteJobConfigPath);
        } catch (HttpStatusCodeException e) {
            throw logAndBuildInsulaApiException(e.getStatusCode(), HttpMethod.DELETE, deleteJobConfigPath);
        }
        LOG.info("Successfully deleted Job Config {}", jobConfigId);
    }

    private static InsulaApiException logAndBuildInsulaApiException(HttpStatusCode httpStatusCode, HttpMethod method, String path) {
        String errorMessage = String.format("Insula API %s %s returned with status: %s", method, path, httpStatusCode);
        LOG.error(errorMessage);
        return new InsulaApiException(errorMessage, httpStatusCode);
    }

    private static ServiceNotFoundException logAndBuildServiceNotFoundException(HttpStatusCode httpStatusCode, HttpMethod method, String path) {
        String errorMessage = String.format("Insula API %s %s returned with status: %s", method, path, httpStatusCode);
        LOG.error(errorMessage);
        return new ServiceNotFoundException(errorMessage, httpStatusCode);
    }
}
