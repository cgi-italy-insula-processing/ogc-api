package com.cgi.eoss.ogcapi.processes.insula.model.response;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * <p>
 *     Interface representing the response of an endpoint that returns a Job object.
 * </p>
 */

public interface JobResponse {

    Long getId();

    Status getStatus();

    Phase getPhase();

    LocalDateTime getEndTime();

    LocalDateTime getStartTime();

    OffsetDateTime getCreated();

    OffsetDateTime getLastUpdated();

    enum Status {
        CREATED,
        RUNNING,
        COMPLETED,
        ERROR,
        CANCELLED,
        PENDING,
        WAITING,
        CONDITION_WAIT
    }

    enum Phase {
        CREATED("Step 0 of 3: Created"),
        DATA_FETCH("Step 1 of 3: Data-Fetch"),
        PROCESSING("Step 2 of 3: Processing"),
        OUTPUT_LIST("Step 3 of 3: Output-List");

        Phase(String text) {}
    }
}
