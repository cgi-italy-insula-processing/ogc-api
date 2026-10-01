package com.cgi.eoss.ogcapi.processes.insula;

import com.cgi.eoss.ogcapi.processes.insula.model.request.JobConfigCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobLaunchRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.request.ServiceCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.*;

import java.util.List;

/**
 * Exposes all the available Insula APIs to interact with the platform.
 */
public interface InsulaApi {

    /**
     * Creates a Service on Insula, sending the provided service request.
     * @param serviceCreationRequest the description of the service to be created.
     * @return the created service.
     */
    ServiceResponse create(ServiceCreationRequest serviceCreationRequest);

    /**
     * Gets the Service with the given ID.
     * @param serviceId the Service ID.
     * @return the requested Service.
     */
    ServiceResponse getService(Long serviceId);

    /**
     * Gets the available Services on Insula
     * ù@param limit the maximum number of elements retrieved.
     */
    GetServicesResponse getServices(Integer limit);

    /**
     * Creates a JobConfig on Insula, sending the provided job config creation request.
     * @param jobConfigCreationRequest the description of the Job to be created.
     * @return the created JobConfig.
     */
    JobConfigCreationResponse create(JobConfigCreationRequest jobConfigCreationRequest);

    /**
     * Launches the provided Job Config on Insula.
     * @param jobLaunchRequest the description of the Job to be launched.
     * @return the launched Job.
     */
    JobLaunchResponse launch(JobLaunchRequest jobLaunchRequest);

    /**
     * Updates the Service with the given ID.
     * @param serviceId              the Service ID.
     * @param serviceCreationRequest the description of the Service to be updated.
     */
    void update(Long serviceId, ServiceCreationRequest serviceCreationRequest);

    /**
     * Disable the Service with the given ID.
     * Make the service no longer available until re-enabled
     * @param serviceId the Service ID.
     */
    void disableService(Long serviceId);

    /**
     * Gets the job representation given its ID.
     * @param jobId the Job ID.
     * @return the requested Job.
     */
    JobGetResponse getJob(Long jobId);

    /**
     * Finds all the Jobs that match the given search criteria.
     * @param limit the maximum number of elements retrieved
     * @param processID the ID of the process (service, in Insula) the jobs belong to.
     * @param status ths status of the jobs.
     * @param datetime interval representing the jobs' duration.
     * @return a list of jobs matching the search criteria.
     */
    JobFindResponse findJobs(Integer limit, Integer page, String processID, List<JobResponse.Status> status, String datetime);

    /**
     * Performs a search on the STAC Search API, using the provided collection and identifier.
     * @param collection the collection to search in.
     * @param identifier the identifier of the item to search for.
     * @return a STAC Search response.
     */
    StacSearchResponse stacSearch(String catalogue, String collection, String identifier);

    /**
     * Cancels the Job with the given ID.
     * @param jobId the Job ID.
     */
    void cancelJob(Long jobId);

    /**
     * Terminates the Job with the given ID.
     * @param jobId the Job ID.
     */
    void terminateJob(Long jobId);

    /**
     * Gets the Job Config associated with the given Job ID.
     * @param jobId the Job ID.
     * @return the requested Job Config.
     */
    JobConfigGetResponse getJobConfig(Long jobId);

    /**
     * Retrieves the outputs for the given Job ID and output identifier, in STAC format
     * @param jobId the Job ID
     * @param outputId the output identifier
     * @return the job outputs as a STAC document
     */
    StacSearchResponse getJobOutputs(Long jobId, String outputId);

}
