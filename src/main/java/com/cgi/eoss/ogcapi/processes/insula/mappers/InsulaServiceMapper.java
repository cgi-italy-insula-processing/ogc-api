package com.cgi.eoss.ogcapi.processes.insula.mappers;

import com.cgi.eoss.ogcapi.processes.controllers.ProcessesApi;
import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.cgi.eoss.ogcapi.processes.insula.model.request.ServiceCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.ServiceResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.ServiceDescriptor;
import com.cgi.eoss.ogcapi.processes.insula.model.response.GetServicesResponse;
import com.cgi.eoss.ogcapi.processes.model.*;
import com.cgi.eoss.ogcapi.processes.model.Process;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Utility class that maps Insula Service Creation Request/Response objects
 * to OGC API Processes model objects and vice-versa.
 */
@Log4j2
public class InsulaServiceMapper extends InsulaOgcMapper {

    private InsulaServiceMapper() {}

    /**
     * Maps a OgcapppkgExecutionUnit object from OGC API Processes model to a ServiceCreationRequest object.
     * @param ogcapppkgExecutionUnit the Ogcapppkg Execution Unit to map.
     * @return the ServiceCreationRequest object.
     */
    public static ServiceCreationRequest toServiceCreationRequest(OgcapppkgExecutionUnit ogcapppkgExecutionUnit) {
        if (ogcapppkgExecutionUnit instanceof Link) {
            return new ServiceCreationRequest(Cwl.builder().url(((Link) ogcapppkgExecutionUnit).getHref()).build());
        }
        LOG.error("Unsupported Service Creation Request {}", ogcapppkgExecutionUnit);
        throw new IllegalArgumentException("Cannot build Service Creation Request from object " + ogcapppkgExecutionUnit);
    }

    /**
     * Maps a ServiceResponse object to an OgcapppkgExecutionUnit object from OGC API Processes model.
     * @param serviceResponse the ServiceResponse object to map.
     * @return the OgcapppkgExecutionUnit object.
     */
    public static Ogcapppkg toOgcApppkg(ServiceResponse serviceResponse) {
        Ogcapppkg ogcapppkg = new Ogcapppkg();
        Link executionUnitAsLink = new Link();
        executionUnitAsLink.setHref(getReferenceLink(serviceResponse));
        executionUnitAsLink.setRel("reference");
        executionUnitAsLink.setType("application/cwl");
        executionUnitAsLink.setTitle("Process Reference");
        ogcapppkg.executionUnit(executionUnitAsLink);
        return ogcapppkg;
    }

    /**
     * Maps a ServiceResponse object to a Process object from OGC API Processes model.
     * @param serviceResponse the ServiceResponse object to map.
     * @param links           list of links to be added to the Process
     * @return the Process object.
     */
    public static Process toProcess(ServiceResponse serviceResponse, List<org.springframework.hateoas.Link> links) {
        Process process = new Process();
        process.setId(serviceResponse.getName());
        process.setDescription(serviceResponse.getDescription());
        process.setJobControlOptions(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));

        ServiceDescriptor serviceDescriptor = serviceResponse.getServiceDescriptor();
        if (serviceDescriptor != null) {
            process.setTitle(serviceDescriptor.getTitle());
            String version = serviceDescriptor.getVersion();
            process.setVersion(version != null ? version : "N/A");
            mapInputOutputParameters(process, serviceDescriptor);
        }

        process.setLinks(buildOgcLinks(links));
        return process;
    }

    /**
     * Maps a ServiceCreationResponse object from Insula model to a ProcessSummary object
     * compliant with OGC API Processes model.
     * @param serviceResponse the Insula service creation response.
     * @param links           list of links to be added to the Process Summary
     * @return the ProcessSummary object.
     */
    public static ProcessSummary toProcessSummary(ServiceResponse serviceResponse, List<org.springframework.hateoas.Link> links) {
        ProcessSummary processSummary = new ProcessSummary();
        processSummary.setId(serviceResponse.getName());
        processSummary.setDescription(serviceResponse.getDescription());
        processSummary.setJobControlOptions(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));

        ServiceDescriptor serviceDescriptor = serviceResponse.getServiceDescriptor();
        if (serviceDescriptor != null) {
            processSummary.setTitle(serviceDescriptor.getTitle());
            String version = serviceDescriptor.getVersion();
            processSummary.setVersion(version != null ? version : "N/A");
        }

        if (links != null) {
            processSummary.setLinks(buildOgcLinks(links));
        }
        return processSummary;
    }

    /**
     * Maps a GetServicesResponse object to a ProcessList object from OGC API Processes model.
     * @param getServicesResponse the GetServicesResponse object to map.
     * @param selfLink            the self link to be added to the Process List
     * @return the ProcessList object.
     */
    public static ProcessList toProcessList(GetServicesResponse getServicesResponse, org.springframework.hateoas.Link selfLink) {
        ProcessList processList = new ProcessList();
        List<ServiceResponse> services = getServicesResponse.getEmbeddedServices().getServices();

        for (ServiceResponse service : services) {
            ProcessSummary processSummary = toProcessSummary(
                service,
                List.of(
                    linkTo(methodOn(ProcessesApi.class).getProcessDescription(service.getId()))
                        .withSelfRel()
                        .withTitle("OGC Process Description")
                        .withType("application/json")
                )
            );
            processList.addProcessesItem(processSummary);
        }

        processList.setLinks(buildOgcLinks(List.of(selfLink)));
        return processList;
    }

    private static String getReferenceLink(ServiceResponse serviceResponse) {
        Cwl cwl = serviceResponse.getCwl();
        if (cwl != null) {
            return cwl.url();
        }
        org.springframework.hateoas.Link selfLink = serviceResponse.getLinks().get("self");
        return selfLink.getHref();
    }

    private static void mapInputOutputParameters(Process process, ServiceDescriptor serviceDescriptor) {
        List<ServiceDescriptor.InputOutputParameter> inputs = serviceDescriptor.getDataInputs();

        for (ServiceDescriptor.InputOutputParameter input : inputs) {
            process.putInputsItem(input.getId(), mapInputParameter(input));
        }

        List<ServiceDescriptor.InputOutputParameter> outputs = serviceDescriptor.getDataOutputs();

        for (ServiceDescriptor.InputOutputParameter output : outputs) {
            process.putOutputsItem(output.getId(), mapOutputParameter(output));
        }
    }

    private static InputDescription mapInputParameter(ServiceDescriptor.InputOutputParameter input) {
        InputDescription inputDescription = new InputDescription();
        inputDescription.title(input.getTitle());
        inputDescription.description(input.getDescription());
        inputDescription.minOccurs(input.getMinOccurs());
        inputDescription.maxOccurs(new InputDescriptionAllOfMaxOccursInteger(input.getMaxOccurs()));
        inputDescription.setValuePassing(List.of(InputDescription.ValuePassingEnum.BY_VALUE));

        if (isDownloadable(input)) {
            inputDescription.setValuePassing(List.of(InputDescription.ValuePassingEnum.BY_REFERENCE));
        }

        inputDescription.setSchema(buildSchema(input));

        return inputDescription;
    }

    private static OutputDescription mapOutputParameter(ServiceDescriptor.InputOutputParameter output) {
        OutputDescription outputDescription = new OutputDescription();
        outputDescription.title(output.getTitle());
        outputDescription.description(output.getDescription());
        outputDescription.setSchema(buildSchema(output));
        return outputDescription;
    }

    private static boolean isDownloadable(ServiceDescriptor.InputOutputParameter input) {
        Map<String, String> platformMetadata = input.getPlatformMetadata();
        Map<String, String> defaultAttrs = input.getDefaultAttrs();

        if (defaultAttrs != null && !"string".equalsIgnoreCase(defaultAttrs.get("dataType"))) {
            return false;
        }

        return platformMetadata != null &&
                !"true".equalsIgnoreCase(platformMetadata.get("preventUrlDownload")) &&
                "CATALOGUE".equalsIgnoreCase(platformMetadata.get("format"));
    }


    private static ProcessesCoreSchemaOneOf buildSchema(ServiceDescriptor.InputOutputParameter inputOutputParameter) {
        ProcessesCoreSchemaOneOf schema = new ProcessesCoreSchemaOneOf();

        Integer maxOccurs = inputOutputParameter.getMaxOccurs();
        if (maxOccurs != null && maxOccurs > 1) {
            schema.setType(ProcessesCoreSchemaOneOf.TypeEnum.ARRAY);
            ProcessesCoreSchemaOneOf itemsSchema = new ProcessesCoreSchemaOneOf();
            itemsSchema.setType(determineSchemaType(inputOutputParameter));
            schema.setItems(itemsSchema);
        } else {
            schema.setType(determineSchemaType(inputOutputParameter));
        }

        String allowedValues = inputOutputParameter.getDefaultAttrs().get("allowedValues");
        if (allowedValues != null) {
            schema.setEnum(List.of(allowedValues.split(",")));
        }

        if ("STAC".equalsIgnoreCase(inputOutputParameter.getPlatformMetadata().get("type"))) {
            schema.setFormat("geojson-feature-collection");
        }

        setOccurrences(schema, inputOutputParameter.getMinOccurs(), maxOccurs);

        return schema;
    }

    private static ProcessesCoreSchemaOneOf.TypeEnum determineSchemaType(ServiceDescriptor.InputOutputParameter parameter) {
        String dataType = parameter.getDefaultAttrs() != null ? parameter.getDefaultAttrs().get("dataType") : null;

        if (dataType == null) {
            return ProcessesCoreSchemaOneOf.TypeEnum.STRING;
        }

        return switch (dataType) {
            case "integer", "long" -> ProcessesCoreSchemaOneOf.TypeEnum.INTEGER;
            case "double", "float" -> ProcessesCoreSchemaOneOf.TypeEnum.NUMBER;
            case "boolean" -> ProcessesCoreSchemaOneOf.TypeEnum.BOOLEAN;
            default -> ProcessesCoreSchemaOneOf.TypeEnum.STRING;
        };
    }

    private static void setOccurrences(ProcessesCoreSchemaOneOf schema, Integer minOccurs, Integer maxOccurs) {
        if (minOccurs == null || maxOccurs == null) {
            return;
        }

        if (minOccurs == 0 && maxOccurs >= 1) {
            schema.nullable(true);
        }

        if (minOccurs.equals(maxOccurs) && minOccurs == 1) {
            schema.nullable(false);
        }

        schema.minItems(minOccurs);
        schema.maxItems(maxOccurs);
    }
}
