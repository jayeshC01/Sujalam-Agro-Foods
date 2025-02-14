package com.gryffindor.excalibur.models;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public class ErrorResponse {
    private HttpStatus code;
    private String message;
    private String stackTrace;
}
