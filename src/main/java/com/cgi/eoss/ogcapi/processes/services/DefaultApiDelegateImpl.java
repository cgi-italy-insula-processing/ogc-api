package com.cgi.eoss.ogcapi.processes.services;

import com.cgi.eoss.ogcapi.processes.controllers.ApiApi;
import com.cgi.eoss.ogcapi.processes.controllers.ConformanceApi;
import com.cgi.eoss.ogcapi.processes.controllers.DefaultApi;
import com.cgi.eoss.ogcapi.processes.controllers.DefaultApiDelegate;
import com.cgi.eoss.ogcapi.processes.model.LandingPage;
import com.cgi.eoss.ogcapi.processes.model.Link;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.cgi.eoss.ogcapi.processes.insula.mappers.InsulaOgcMapper.buildOgcLinks;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Service that implements the {@link com.cgi.eoss.ogcapi.processes.controllers.DefaultApiDelegate} interface and
 * holds the logic for the OGC API Default endpoints.
 */

@Service
@Log4j2
@AllArgsConstructor
public class DefaultApiDelegateImpl implements DefaultApiDelegate {

    @Override
    public ResponseEntity<LandingPage> getLandingPage(String f) {
        LOG.info("getLandingPage called with format: {}", f);
        if (f != null && !isJsonFormat(f)) {
            LOG.error("Unsupported format: {}", f);
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        LandingPage landingPage = new LandingPage(getLandingPageLinks());
        landingPage.title("Insula OGC API Processes Backend");
        landingPage.description("Insula REST API endpoints compliant with OGC Application Package Specification.");
        return ResponseEntity.ok(landingPage);
    }

    private static List<Link> getLandingPageLinks() {
        org.springframework.hateoas.Link selfLink = linkTo(methodOn(DefaultApi.class).getLandingPage(MediaType.APPLICATION_JSON_VALUE))
                .withRel("self")
                .withType(MediaType.APPLICATION_JSON_VALUE)
                .withTitle("Landing page");
        org.springframework.hateoas.Link apiLink = linkTo(methodOn(ApiApi.class).getAPI(MediaType.APPLICATION_JSON_VALUE))
                .withRel("service-desc")
                .withType(MediaType.APPLICATION_JSON_VALUE)
                .withTitle("API page");
        org.springframework.hateoas.Link conformanceLink = linkTo(methodOn(ConformanceApi.class).getConformance(MediaType.APPLICATION_JSON_VALUE))
                .withRel("http://www.opengis.net/def/rel/ogc/1.0/conformance")
                .withType(MediaType.APPLICATION_JSON_VALUE)
                .withTitle("Conformance Classes");
        return buildOgcLinks(List.of(selfLink, apiLink, conformanceLink));
    }

    private static boolean isJsonFormat(String format) {
        return MediaType.APPLICATION_JSON_VALUE.equals(format) || "json".equals(format);
    }

}
