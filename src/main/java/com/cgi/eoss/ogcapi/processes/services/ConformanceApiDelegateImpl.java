package com.cgi.eoss.ogcapi.processes.services;

import com.cgi.eoss.ogcapi.processes.controllers.ConformanceApiDelegate;
import com.cgi.eoss.ogcapi.processes.model.ConfClasses;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that implements the {@link com.cgi.eoss.ogcapi.processes.controllers.ConformanceApiDelegate} interface and
 * holds the logic for the OGC API Conformance endpoints.
 */

@Service
@Log4j2
@AllArgsConstructor
public class ConformanceApiDelegateImpl implements ConformanceApiDelegate {

    @Override
    public ResponseEntity<ConfClasses> getConformance(String f) {
        LOG.info("getConformance called with format: {}", f);
        if (f != null && !isJsonFormat(f)) {
            LOG.error("Unsupported format: {}", f);
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(new ConfClasses().conformsTo(
                List.of(
                        "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/core",
                        "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/ogc-process-description",
                        "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/json",
                        "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/oas30",
                        "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/job-list",
                        "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/dismiss",
                        "http://www.opengis.net/spec/ogcapi-processes-2/1.0/conf/ogcapppkg",
                        "http://www.opengis.net/spec/ogcapi-processes-2/1.0/conf/deploy-replace-undeploy"
                )
        ));
    }

    private static boolean isJsonFormat(String format) {
        return MediaType.APPLICATION_JSON_VALUE.equals(format) || "json".equals(format);
    }
}
