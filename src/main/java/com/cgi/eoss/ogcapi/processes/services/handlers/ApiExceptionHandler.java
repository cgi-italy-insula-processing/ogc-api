package com.cgi.eoss.ogcapi.processes.services.handlers;

import com.cgi.eoss.ogcapi.processes.insula.exception.InsulaApiException;
import com.cgi.eoss.ogcapi.processes.insula.exception.ServiceNotFoundException;
import com.cgi.eoss.ogcapi.processes.model.Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ApiExceptionHandler {

    @ResponseBody
    @ExceptionHandler({ HttpMediaTypeException.class })
    public ResponseEntity<Exception> handleUnsupportedMediaTypeException(HttpMediaTypeException e) {
        ProblemDetail problemDetail = e.getBody();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(buildException("http://www.opengis.net/def/exceptions/ogcapi-processes-2/1.0/unsupported-media-type",
                        problemDetail.getTitle(), problemDetail.getStatus(), problemDetail.getDetail()));
    }

    @ResponseBody
    @ExceptionHandler({ InsulaApiException.class })
    public ResponseEntity<Exception> handleInsulaApiException(InsulaApiException e) {
        HttpStatusCode httpStatusCode = e.getHttpStatusCode();
        return ResponseEntity.status(e.getHttpStatusCode()).body(buildException(HttpStatus.CONFLICT.equals(httpStatusCode) ?
                        "http://www.opengis.net/def/exceptions/ogcapi-processes-2/1.0/duplicated-process" : "N/A",
                httpStatusCode.toString(), httpStatusCode.value(), e.getMessage()));
    }

    @ResponseBody
    @ExceptionHandler({ ServiceNotFoundException.class })
    public ResponseEntity<Exception> handleServiceNotFoundException(ServiceNotFoundException e) {
        HttpStatusCode httpStatusCode = e.getHttpStatusCode();
        return ResponseEntity.status(e.getHttpStatusCode()).body(buildException(HttpStatus.NOT_FOUND.equals(httpStatusCode) ?
                        "https://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process" : "N/A",
                        httpStatusCode.toString(), httpStatusCode.value(), e.getMessage()));
    }

    @ResponseBody
    @ExceptionHandler({ RuntimeException.class })
    public ResponseEntity<Exception> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildException(
                        "N/A",
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        e.getMessage()));
    }

    private static Exception buildException(String type, String title, Integer status, String detail) {
        Exception ogcException = new Exception(type);
        ogcException.setTitle(title);
        ogcException.setStatus(status);
        ogcException.setDetail(detail);
        return ogcException;
    }
}
