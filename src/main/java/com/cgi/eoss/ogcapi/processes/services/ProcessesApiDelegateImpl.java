package com.cgi.eoss.ogcapi.processes.services;

import com.cgi.eoss.ogcapi.processes.controllers.ProcessesApi;
import com.cgi.eoss.ogcapi.processes.controllers.JobsApi;
import com.cgi.eoss.ogcapi.processes.config.InsulaClientProperties;
import com.cgi.eoss.ogcapi.processes.controllers.ProcessesApiDelegate;
import com.cgi.eoss.ogcapi.processes.insula.InsulaApi;
import com.cgi.eoss.ogcapi.processes.insula.exception.InsulaApiException;
import com.cgi.eoss.ogcapi.processes.insula.model.request.JobLaunchRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobConfigCreationResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.JobLaunchResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.ServiceResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.GetServicesResponse;
import com.cgi.eoss.ogcapi.processes.model.*;
import com.cgi.eoss.ogcapi.processes.model.Process;
import com.cgi.eoss.ogcapi.processes.util.UrlRewriteUtils;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.hateoas.Link;
import org.springframework.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toJobConfigCreationRequest;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaJobMapper.toStatusInfo;
import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaServiceMapper.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.net.URI;

/**
 * Service that implements the {@link ProcessesApiDelegate} interface and holds the logic for the OGC API Processes+
 * endpoints.
 */
@Log4j2
@Service
@AllArgsConstructor
public class ProcessesApiDelegateImpl implements ProcessesApiDelegate {

    private final InsulaApi insulaApi;

    private final InsulaClientProperties insulaClientProperties;

    private final NativeWebRequest nativeWebRequest;

    private final RequestBaseUriResolver requestBaseUriResolver;

    @Override
    public ResponseEntity<ProcessSummary> deploy(Ogcapppkg ogcapppkg) {
        LOG.info("Deploy OGC Process {}", ogcapppkg);
        ServiceResponse serviceResponse;

        try {
            serviceResponse = insulaApi.create(toServiceCreationRequest(ogcapppkg.getExecutionUnit()));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Deployed Insula Service with name {} from reference {}", serviceResponse.getName(),
                serviceResponse.getCwl().url());
        return new ResponseEntity<>(
            toProcessSummary(
                serviceResponse,
                List.of(
                    linkTo(methodOn(ProcessesApi.class)
                        .getProcessDescription(serviceResponse.getId()))
                        .withSelfRel()
                        .withTitle("self")
                        .withType("application/json")
                )
            ),
            buildDeployHeaders(serviceResponse),
            HttpStatus.CREATED
        );
    }

    @Override
    public ResponseEntity<Ogcapppkg> getPackage(Long processId) {
        LOG.info("Received getPackage request for process with ID {}", processId);
        ServiceResponse serviceResponse;
        try {
            serviceResponse = insulaApi.getService(processId);
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Successfully retrieved package for process with ID {}", processId);
        return new ResponseEntity<>(toOgcApppkg(serviceResponse), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StatusInfo> execute(Long processId, Execute execute) {
        LOG.info("Received execute request {}", execute);
        JobConfigCreationResponse jobConfigCreationResponse;
        try {
            jobConfigCreationResponse =
                    insulaApi.create(toJobConfigCreationRequest(insulaClientProperties.getBaseUrl(),
                            processId, execute.getInputs()));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }
        Long jobConfigId = jobConfigCreationResponse.getId();

        LOG.info("Requesting execution of Insula Job Config with ID {}", jobConfigId);
        JobLaunchResponse jobLaunchResponse;
        try {
            jobLaunchResponse = insulaApi.launch(JobLaunchRequest.builder().jobConfigId(jobConfigId).build());
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Executed Insula Job with ID {} from Job Config {}", jobLaunchResponse.getId(), jobConfigId);

        return new ResponseEntity<>(
                toStatusInfo(jobLaunchResponse, buildLinks(jobLaunchResponse), processId, List.of()),
                HttpStatus.CREATED
        );
    }

    @Override
    public ResponseEntity<Void> replace(Long processId, Ogcapppkg ogcapppkg) {
        LOG.info("Received replace request for process with ID {}", processId);

        try {
            insulaApi.update(processId, toServiceCreationRequest(ogcapppkg.getExecutionUnit()));
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Successfully replaced process with ID {}", processId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> undeploy(Long processId) {
        LOG.info("Received undeploy request for process with ID {}", processId);
        try {
            insulaApi.disableService(processId);
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Successfully undeployed process with ID {}", processId);
        return ResponseEntity.noContent().build();
    }


    @Override
    public ResponseEntity<Process> getProcessDescription(Long processId) {
        LOG.info("Received getProcessDescription request for process with ID {}", processId);
        ServiceResponse serviceResponse;

        try {
            serviceResponse = insulaApi.getService(processId);
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Successfully retrieved process description for process with ID {}", processId);
        return ResponseEntity.ok(
            toProcess(
                serviceResponse,
                List.of(
                    linkTo(methodOn(ProcessesApi.class).getProcessDescription(processId))
                        .withSelfRel()
                        .withTitle("self")
                        .withType("application/json"),
                    linkTo(methodOn(ProcessesApi.class).getPackage(processId))
                        .withRel("package")
                        .withTitle("Process formal description")
                        .withType("application/cwl"),
                    linkTo(methodOn(ProcessesApi.class).execute(processId, null))
                        .withRel("http://www.opengis.net/def/rel/ogc/1.0/execute")
                        .withTitle("Execute endpoint")
                        .withType("application/ogcapppkg+json")
                )
            )
        );
    }

    @Override
    public ResponseEntity<ProcessList> getProcesses(Integer limit) {
        LOG.info("Received getProcesses request");
        GetServicesResponse getServicesResponse;

        try {
            getServicesResponse = insulaApi.getServices(limit);
        } catch (InsulaApiException e) {
            LOG.error("Insula API responded with status code {}", e.getHttpStatusCode());
            throw e;
        }

        LOG.info("Successfully retrieved process list");
        return ResponseEntity.ok(
            toProcessList(
                getServicesResponse,
                linkTo(methodOn(ProcessesApi.class).getProcesses(limit))
                    .withSelfRel()
                    .withTitle("self")
                    .withType("application/json")
            )
        );

    }

    private static HttpHeaders buildDeployHeaders(ServiceResponse serviceResponse) {
        Link linkToGetProcessDescription = linkTo(methodOn(ProcessesApi.class)
                .getProcessDescription(serviceResponse.getId())).withSelfRel();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkToGetProcessDescription.toUri());
        return headers;
    }

    private List<Link> buildLinks(JobLaunchResponse jobLaunchResponse) {
        List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(JobsApi.class).getStatus(String.valueOf(jobLaunchResponse.getId())))
                .withSelfRel()
                .withTitle("Job Status")
                .withType("application/json")
        );

        buildInsulaJobLink(jobLaunchResponse, requestBaseUriResolver.resolveFrom(nativeWebRequest))
                .ifPresent(links::add);

        return links;
    }

    private Optional<Link> buildInsulaJobLink(JobLaunchResponse jobLaunchResponse, URI targetBaseUri) {
        Link selfLink = getJobLaunchResponseSelfLink(jobLaunchResponse);
        if (selfLink == null) {
            LOG.debug("Missing self link in JobLaunchResponse, jobId={}", jobLaunchResponse.getId());
            return Optional.empty();
        }

        return Optional.of(
                Link.of(UrlRewriteUtils.rewriteBaseUrl(selfLink.getHref(), targetBaseUri))
                    .withRel("insula-job")
                    .withTitle("Insula Job")
                    .withType("application/json")
        );
    }

    private static Link getJobLaunchResponseSelfLink(JobLaunchResponse jobLaunchResponse) {
        if (jobLaunchResponse == null || jobLaunchResponse.getLinks() == null) {
            return null;
        }
        return jobLaunchResponse.getLinks().get("self");
    }

}
