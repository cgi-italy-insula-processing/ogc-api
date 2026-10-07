package com.cgi.eoss.ogcapi.processes.insula.mappers;

import com.cgi.eoss.ogcapi.processes.insula.model.Cwl;
import com.cgi.eoss.ogcapi.processes.insula.model.request.ServiceCreationRequest;
import com.cgi.eoss.ogcapi.processes.insula.model.response.GetServicesResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.response.ServiceResponse;
import com.cgi.eoss.ogcapi.processes.insula.model.ServiceDescriptor;
import com.cgi.eoss.ogcapi.processes.model.*;
import com.cgi.eoss.ogcapi.processes.model.Process;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InsulaServiceMapperTest {

    @Test
    public void testToServiceCreationRequest_ReturnsServiceCreationRequestWithProvidedReference_WhenOgcapppkgExecutionUnitIsLinkInstance() {
        String hrefReference = "https://reference.ogc/cwl";
        Link ogcLink = new Link();
        ogcLink.setHref(hrefReference);
        ogcLink.setTitle("title");
        ogcLink.setHreflang("lang");
        ogcLink.setRel("rel");
        ogcLink.setType("type");

        ServiceCreationRequest serviceCreationRequest = InsulaServiceMapper.toServiceCreationRequest(ogcLink);
        assertThat(serviceCreationRequest.getCwl().url()).isEqualTo(hrefReference);
    }

    @Test
    public void testToServiceCreationRequest_ThrowsIllegalArgumentException_WhenOgcapppkgExecutionUnitIsNotLinkInstance() {
        assertThatThrownBy(() -> InsulaServiceMapper.toServiceCreationRequest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot build Service Creation Request from object null");
    }

    @Test
    public void testToProcessSummary_ReturnsProcessSummaryWithProvidedValues_WhenAllServiceCreationResponseAttributesArePopulated() {
        ServiceResponse serviceResponse =  ServiceResponse.builder()
                .name("name").description("description").cwl(Cwl.builder().url("reference").document("document").build())
                .serviceDescriptor(ServiceDescriptor.builder().title("title").version("version").build())
                .build();
        ProcessSummary processSummary = InsulaServiceMapper.toProcessSummary(serviceResponse,
                List.of(org.springframework.hateoas.Link.of("href", "self"), org.springframework.hateoas.Link.of("href", "cwl")));
        assertThat(processSummary.getId()).isEqualTo("name");
        assertThat(processSummary.getDescription()).isEqualTo("description");
        assertThat(processSummary.getTitle()).isEqualTo("title");
        assertThat(processSummary.getVersion()).isEqualTo("version");
        assertThat(processSummary.getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));

        List<Link> ogcLinks = processSummary.getLinks();
        assertThat(ogcLinks.size()).isEqualTo(2);
        Link selfLink = new Link();
        selfLink.setRel("self");
        selfLink.setHref("href");
        assertThat(ogcLinks.get(0)).isEqualTo(selfLink);
        Link cwlLink = new Link();
        cwlLink.setRel("cwl");
        cwlLink.setHref("href");
        assertThat(ogcLinks.get(1)).isEqualTo(cwlLink);

        assertThat(processSummary.getKeywords()).isEmpty();
        assertThat(processSummary.getMetadata()).isEmpty();
    }

    @Test
    public void testToProcessSummary_ReturnsProcessSummaryWithEmptyFields_WhenServiceCreationResponseAttributesAreNotSet() {
        ServiceResponse serviceResponse =  ServiceResponse.builder().build();
        ProcessSummary processSummary = InsulaServiceMapper.toProcessSummary(serviceResponse, Collections.emptyList());
        assertThat(processSummary.getId()).isNull();
        assertThat(processSummary.getDescription()).isNull();
        assertThat(processSummary.getLinks()).isEmpty();
        assertThat(processSummary.getTitle()).isNull();
        assertThat(processSummary.getVersion()).isNull();
        assertThat(processSummary.getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));
        assertThat(processSummary.getKeywords()).isEmpty();
        assertThat(processSummary.getMetadata()).isEmpty();
    }

    @Test
    public void testToProcessSummary_ReturnsProcessSummaryWithProvidedValues_WhenRequiredVersionAttributeIsNotSet() {
        ServiceResponse serviceResponse =  ServiceResponse.builder()
                .name("name").description("description").cwl(Cwl.builder().url("reference").document("document").build())
                .serviceDescriptor(ServiceDescriptor.builder().title("title").build()).build();
        ProcessSummary processSummary = InsulaServiceMapper.toProcessSummary(serviceResponse, null);
        assertThat(processSummary.getId()).isEqualTo("name");
        assertThat(processSummary.getDescription()).isEqualTo("description");
        assertThat(processSummary.getLinks()).isEmpty();
        assertThat(processSummary.getTitle()).isEqualTo("title");
        assertThat(processSummary.getVersion()).isEqualTo("N/A");
        assertThat(processSummary.getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));
        assertThat(processSummary.getKeywords()).isEmpty();
        assertThat(processSummary.getMetadata()).isEmpty();
    }

    @Test
    public void testToOgcApppkg_ReturnsOgcapppkgWithReferenceLink_WhenServiceResponseContainsCwl() {
        String referenceLink = "https://url.com/reference";
        ServiceResponse serviceResponse = ServiceResponse.builder()
                .cwl(Cwl.builder().url(referenceLink).build())
                .build();

        Ogcapppkg ogcapppkg = InsulaServiceMapper.toOgcApppkg(serviceResponse);
        assertThat(((Link) ogcapppkg.getExecutionUnit()).getHref()).isEqualTo(referenceLink);
    }

    @Test
    public void testToOgcApppkg_ReturnsOgcapppkgWithSelfLink_WhenServiceResponseDoesNotContainCwl() {
        String selfLink = "https://processing.example.com/self";
        ServiceResponse serviceResponse = ServiceResponse.builder()
                .links(Collections.singletonMap("self", org.springframework.hateoas.Link.of(selfLink)))
                .build();

        Ogcapppkg ogcapppkg = InsulaServiceMapper.toOgcApppkg(serviceResponse);
        assertThat(((Link) ogcapppkg.getExecutionUnit()).getHref()).isEqualTo(selfLink);
    }

    @Test
    public void testToProcess_ReturnsProcessWithProvidedValues_WhenAllServiceResponseAttributesArePopulated() {
        ServiceResponse serviceResponse =  ServiceResponse.builder()
                .name("name").description("description")
                .serviceDescriptor(ServiceDescriptor.builder()
                        .title("title")
                        .version("version")
                        .dataInputs(List.of(
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputUrl")
                                        .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                        .platformMetadata(ImmutableMap.of("format", "CATALOGUE"))
                                        .title("Input Url Title")
                                        .description("Description of Input Url")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputFloat")
                                        .defaultAttrs(ImmutableMap.of("dataType", "float"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Float Title")
                                        .description("Description of Input Float")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputDouble")
                                        .defaultAttrs(ImmutableMap.of("dataType", "double"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Double Title")
                                        .description("Description of Input Double")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputInteger")
                                        .defaultAttrs(ImmutableMap.of("dataType", "integer"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Integer Title")
                                        .description("Description of Input Integer")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputLong")
                                        .defaultAttrs(ImmutableMap.of("dataType", "long"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Long Title")
                                        .description("Description of Input Long")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputBoolean")
                                        .defaultAttrs(ImmutableMap.of("dataType", "boolean"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Boolean Title")
                                        .description("Description of Input Boolean")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputRequired")
                                        .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Required Title")
                                        .description("Description of Input Required")
                                        .minOccurs(1)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputStac")
                                        .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                        .platformMetadata(ImmutableMap.of("format", "CATALOGUE", "type", "STAC"))
                                        .title("Input Stac Title")
                                        .description("Description of Input Stac")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputEnum")
                                        .defaultAttrs(ImmutableMap.of("dataType", "string", "allowedValues", "a,b,c"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Enum Title")
                                        .description("Description of Input Enum")
                                        .minOccurs(0)
                                        .maxOccurs(1)
                                        .build(),
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("inputArray")
                                        .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                        .platformMetadata(ImmutableMap.of("format", "OTHER"))
                                        .title("Input Array Title")
                                        .description("Description of Input Array")
                                        .minOccurs(0)
                                        .maxOccurs(100)
                                        .build()
                        ))
                        .dataOutputs(List.of(
                                ServiceDescriptor.InputOutputParameter.builder()
                                        .id("outputId")
                                        .defaultAttrs(ImmutableMap.of("dataType", "string"))
                                        .platformMetadata(ImmutableMap.of("format", "CATALOGUE"))
                                        .title("Output Title")
                                        .description("Description of outputId")
                                        .build()
                        ))
                        .build())
                .build();
        Process process = InsulaServiceMapper.toProcess(
                serviceResponse,
                List.of(org.springframework.hateoas.Link.of("href", "self"))
        );
        assertThat(process.getId()).isEqualTo("name");
        assertThat(process.getDescription()).isEqualTo("description");
        assertThat(process.getTitle()).isEqualTo("title");
        assertThat(process.getVersion()).isEqualTo("version");
        assertThat(process.getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));

        assertThat(process.getInputs()).hasSize(10);

        {
            InputDescription actualInput = process.getInputs().get("inputUrl");
            assertThat(actualInput.getTitle()).isEqualTo("Input Url Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Url");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_REFERENCE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.STRING);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputFloat");
            assertThat(actualInput.getTitle()).isEqualTo("Input Float Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Float");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.NUMBER);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputDouble");
            assertThat(actualInput.getTitle()).isEqualTo("Input Double Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Double");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.NUMBER);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputInteger");
            assertThat(actualInput.getTitle()).isEqualTo("Input Integer Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Integer");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.INTEGER);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputLong");
            assertThat(actualInput.getTitle()).isEqualTo("Input Long Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Long");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.INTEGER);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputBoolean");
            assertThat(actualInput.getTitle()).isEqualTo("Input Boolean Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Boolean");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.BOOLEAN);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputRequired");
            assertThat(actualInput.getTitle()).isEqualTo("Input Required Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Required");
            assertThat(actualInput.getMinOccurs()).isEqualTo(1);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.STRING);
            expectedInputSchema.setMinItems(1);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(false);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputStac");
            assertThat(actualInput.getTitle()).isEqualTo("Input Stac Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Stac");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_REFERENCE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.STRING);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            expectedInputSchema.setFormat("geojson-feature-collection");
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputEnum");
            assertThat(actualInput.getTitle()).isEqualTo("Input Enum Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Enum");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(1);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.STRING);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(1);
            expectedInputSchema.setNullable(true);
            expectedInputSchema.setEnum(List.of("a", "b", "c"));
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        {
            InputDescription actualInput = process.getInputs().get("inputArray");
            assertThat(actualInput.getTitle()).isEqualTo("Input Array Title");
            assertThat(actualInput.getDescription()).isEqualTo("Description of Input Array");
            assertThat(actualInput.getMinOccurs()).isEqualTo(0);
            assertThat(((InputDescriptionAllOfMaxOccursInteger) actualInput.getMaxOccurs()).getValue()).isEqualTo(100);
            assertThat(actualInput.getValuePassing()).contains(InputDescription.ValuePassingEnum.BY_VALUE);
            ProcessesCoreSchemaOneOf expectedInputSchema = new ProcessesCoreSchemaOneOf();
            expectedInputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.ARRAY);
            expectedInputSchema.setMinItems(0);
            expectedInputSchema.setMaxItems(100);
            expectedInputSchema.setNullable(true);
            ProcessesCoreSchemaOneOf itemsSchema = new ProcessesCoreSchemaOneOf();
            itemsSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.STRING);
            expectedInputSchema.setItems(itemsSchema);
            assertThat(actualInput.getSchema()).isEqualTo(expectedInputSchema);
        }

        assertThat(process.getOutputs()).hasSize(1);
        OutputDescription actualOutput = process.getOutputs().get("outputId");
        assertThat(actualOutput.getTitle()).isEqualTo("Output Title");
        assertThat(actualOutput.getDescription()).isEqualTo("Description of outputId");
        ProcessesCoreSchemaOneOf expectedOutputSchema = new ProcessesCoreSchemaOneOf();
        expectedOutputSchema.setType(ProcessesCoreSchemaOneOf.TypeEnum.STRING);
        assertThat(actualOutput.getSchema()).isEqualTo(expectedOutputSchema);

        List<Link> ogcLinks = process.getLinks();
        assertThat(ogcLinks.size()).isEqualTo(1);
        Link selfLink = new Link();
        selfLink.setRel("self");
        selfLink.setHref("href");
        assertThat(ogcLinks.get(0)).isEqualTo(selfLink);


        assertThat(process.getKeywords()).isEmpty();
        assertThat(process.getMetadata()).isEmpty();
    }

    @Test
    public void testToProcess_ReturnsProcessWithEmptyFields_WhenServiceResponseAttributesAreNotSet() {
        ServiceResponse serviceResponse =  ServiceResponse.builder().build();
        Process process = InsulaServiceMapper.toProcess(serviceResponse, List.of(org.springframework.hateoas.Link.of("href", "self")));
        assertThat(process.getId()).isNull();
        assertThat(process.getDescription()).isNull();
        assertThat(process.getTitle()).isNull();
        assertThat(process.getVersion()).isNull();
        assertThat(process.getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));
        assertThat(process.getInputs()).isEmpty();
        assertThat(process.getOutputs()).isEmpty();
        assertThat(process.getKeywords()).isEmpty();
        assertThat(process.getMetadata()).isEmpty();
        assertThat(process.getLinks()).hasSize(1);
        assertThat(process.getLinks().get(0).getHref()).isEqualTo("href");
        assertThat(process.getLinks().get(0).getRel()).isEqualTo("self");
    }

    @Test
    public void testToProcessList_ReturnsProcessListWithProvidedProcesses_WhenAllGetServicesResponseAttributesArePopulated() {
        ServiceResponse serviceResponse1 = ServiceResponse.builder()
                .name("service1").description("description1")
                .serviceDescriptor(ServiceDescriptor.builder()
                        .title("title1")
                        .version("version1")
                        .build())
                .build();
        ServiceResponse serviceResponse2 = ServiceResponse.builder()
                .name("service2").description("description2")
                .serviceDescriptor(ServiceDescriptor.builder()
                        .title("title2")
                        .version("version2")
                        .build())
                .build();

        GetServicesResponse getServicesResponse = GetServicesResponse.builder()
                .embeddedServices(new GetServicesResponse.EmbeddedServices(List.of(serviceResponse1, serviceResponse2)))
                .build();

        ProcessList processList = InsulaServiceMapper.toProcessList(getServicesResponse, org.springframework.hateoas.Link.of("href", "self"));

        assertThat(processList.getProcesses()).hasSize(2);
        assertThat(processList.getProcesses().get(0).getId()).isEqualTo("service1");
        assertThat(processList.getProcesses().get(0).getDescription()).isEqualTo("description1");
        assertThat(processList.getProcesses().get(0).getTitle()).isEqualTo("title1");
        assertThat(processList.getProcesses().get(0).getVersion()).isEqualTo("version1");
        assertThat(processList.getProcesses().get(0).getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));

        assertThat(processList.getProcesses().get(1).getId()).isEqualTo("service2");
        assertThat(processList.getProcesses().get(1).getDescription()).isEqualTo("description2");
        assertThat(processList.getProcesses().get(1).getTitle()).isEqualTo("title2");
        assertThat(processList.getProcesses().get(1).getVersion()).isEqualTo("version2");
        assertThat(processList.getProcesses().get(1).getJobControlOptions()).isEqualTo(List.of(JobControlOptions.ASYNC_EXECUTE, JobControlOptions.DISMISS));

        List<Link> ogcLinks = processList.getLinks();
        assertThat(ogcLinks.size()).isEqualTo(1);
        Link selfLink = new Link();
        selfLink.setRel("self");
        selfLink.setHref("href");
        assertThat(ogcLinks.get(0)).isEqualTo(selfLink);
    }

    @Test
    public void testToProcessList_ReturnsEmptyProcessList_WhenGetServicesResponseIsEmpty() {
        GetServicesResponse getServicesResponse = GetServicesResponse.builder()
                .embeddedServices(new GetServicesResponse.EmbeddedServices(Collections.emptyList()))
                .build();

        ProcessList processList = InsulaServiceMapper.toProcessList(getServicesResponse, org.springframework.hateoas.Link.of("href", "self"));

        assertThat(processList.getProcesses()).isEmpty();
        assertThat(processList.getLinks()).hasSize(1);
        assertThat(processList.getLinks().get(0).getRel()).isEqualTo("self");
        assertThat(processList.getLinks().get(0).getHref()).isEqualTo("href");
    }
}
