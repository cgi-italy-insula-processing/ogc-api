package com.cgi.eoss.ogcapi.processes.insula.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ServiceNotFoundException extends InsulaApiException {

    public ServiceNotFoundException(String message, HttpStatusCode httpStatusCode) {
        super(message, httpStatusCode);
    }
}