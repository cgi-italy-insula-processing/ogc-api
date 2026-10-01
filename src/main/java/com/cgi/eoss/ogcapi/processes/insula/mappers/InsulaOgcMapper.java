package com.cgi.eoss.ogcapi.processes.insula.mappers;

import com.cgi.eoss.ogcapi.processes.model.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that maps Insula domain objects to OGC API Processes model objects and vice-versa.
 */
public abstract class InsulaOgcMapper {

    public static List<Link> buildOgcLinks(List<org.springframework.hateoas.Link> links) {
        List<Link> ogcLinks = new ArrayList<>();
        for (org.springframework.hateoas.Link link : links) {
            Link ogcLink = new Link();
            ogcLink.setRel(link.getRel().value());
            ogcLink.setHref(link.getHref());
            ogcLink.setType(link.getType());
            ogcLink.setTitle(link.getTitle());
            ogcLink.setHreflang(link.getHreflang());
            ogcLinks.add(ogcLink);
        }
        return ogcLinks;
    }
}
