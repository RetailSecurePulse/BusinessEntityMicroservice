package com.retailpulse.exception;

import com.retailpulse.controller.ErrorResponse;
import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.service.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgumentReturnsBadRequestWithMessage() {
        ResponseEntity<String> response = globalExceptionHandler.handleIllegalArgument(
                new IllegalArgumentException("bad argument")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad argument");
    }

    @Test
    void handleRuntimeExceptionReturnsBadRequestWithMessage() {
        ResponseEntity<String> response = globalExceptionHandler.handleRuntimeException(
                new RuntimeException("boom")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("boom");
    }

    @Test
    void applicationExceptionHandlerReturnsErrorResponse() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.applicationExceptionHandler(
                new ApplicationException("INVALID_REQUEST", "missing field")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().getMessage()).isEqualTo("missing field");
    }

    @Test
    void handleBusinessExceptionReturnsErrorResponse() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(
                new BusinessException("BUSINESS_ENTITY_NOT_FOUND", "missing")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("BUSINESS_ENTITY_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("missing");
    }
}
