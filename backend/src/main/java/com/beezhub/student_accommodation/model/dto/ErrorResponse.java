package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private List<String> errors;
    private String path;

    /** Fallback when an exception carries no message; List.of rejects null (GH-149). */
    private static final String UNSPECIFIED_ERROR = "Request could not be completed";

    /**
     * Every handler in {@code RestResponseExceptionHandler} builds its body through here, so the
     * null guard belongs here rather than at the call sites. A RuntimeException carrying no
     * message used to make {@code List.of} throw inside the handler itself, and the request fell
     * through to the container error page.
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.errors = List.of(error == null || error.isBlank() ? UNSPECIFIED_ERROR : error);
        this.path = path;
    }
}
