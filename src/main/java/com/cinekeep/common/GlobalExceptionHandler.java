package com.cinekeep.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception) {
        return toResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException exception) {
        return toResponse(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyRequests(TooManyRequestsException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(new ApiErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
        return toResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
        return toResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldMessage)
                .sorted()
                .collect(Collectors.joining(", "));

        return handleExceptionInternal(exception, new ApiErrorResponse(status.value(), message), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = exception.getParameterValidationResults().stream()
                .flatMap(this::toParameterMessages)
                .sorted()
                .collect(Collectors.joining(", "));

        return handleExceptionInternal(exception, new ApiErrorResponse(status.value(), message), headers, status, request);
    }

    private Stream<String> toParameterMessages(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            return parameterErrors.getFieldErrors().stream().map(this::toFieldMessage);
        }

        String parameterName = result.getMethodParameter().getParameterName();
        return result.getResolvableErrors().stream()
                .map(error -> parameterName + ": " + error.getDefaultMessage());
    }

    private String toFieldMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        Object errorBody = body instanceof ApiErrorResponse
                ? body
                : new ApiErrorResponse(statusCode.value(), resolveMessage(exception, body, statusCode));

        return super.handleExceptionInternal(exception, errorBody, headers, statusCode, request);
    }

    private String resolveMessage(Exception exception, Object body, HttpStatusCode statusCode) {
        if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
            return problemDetail.getDetail();
        }

        if (exception instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
            return errorResponse.getBody().getDetail();
        }

        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.getReasonPhrase() : "Request failed";
    }

    private ResponseEntity<ApiErrorResponse> toResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), message));
    }
}
