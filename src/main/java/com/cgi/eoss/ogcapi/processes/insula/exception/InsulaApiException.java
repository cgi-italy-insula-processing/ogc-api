package com.cgi.eoss.ogcapi.processes.insula.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Exception class that represents an error that occurred when interacting with
 * Insula APIs.
 */
@Getter
public class InsulaApiException extends RuntimeException {

    private final HttpStatusCode httpStatusCode;

    /**
     * Creates an instance of this class with the provided message.
     * @param message the error message.
     * @param httpStatusCode the HTTP Status Code returned by Insula.
     */
    public InsulaApiException(String message, HttpStatusCode httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }
}
