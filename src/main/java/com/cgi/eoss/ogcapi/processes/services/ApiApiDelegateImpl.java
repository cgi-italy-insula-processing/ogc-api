package com.cgi.eoss.ogcapi.processes.services;

import com.cgi.eoss.ogcapi.processes.controllers.ApiApi;
import com.cgi.eoss.ogcapi.processes.controllers.ApiApiDelegate;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Service that implements the {@link com.cgi.eoss.ogcapi.processes.controllers.ApiApiDelegate} interface and
 * holds the logic for the OGC API Api endpoints.
 */

@Service
@AllArgsConstructor
@Log4j2
public class ApiApiDelegateImpl implements ApiApiDelegate {

    private final static String SWAGGER_UI_INDEX = "swagger-ui/index.html";

    @Override
    public ResponseEntity<Object> getAPI(String f) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkTo(methodOn(ApiApi.class).getAPI(null))
                .slash(SWAGGER_UI_INDEX).toUri());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
