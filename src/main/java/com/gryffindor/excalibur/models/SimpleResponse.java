package com.gryffindor.excalibur.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleResponse<T> {
    private HttpStatus status;
    private String message;
    private T content;
}
