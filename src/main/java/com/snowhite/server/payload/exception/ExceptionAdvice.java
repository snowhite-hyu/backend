package com.snowhite.server.payload.exception;

import com.snowhite.server.payload.ApiResponse;

import com.snowhite.server.payload.code.status.ErrorStatus;
import com.snowhite.server.payload.dto.ErrorReasonDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ExceptionAdvice {

//    @ExceptionHandler
//    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
//        String errorMessage = e.getConstraintViolations().stream()
//                .map(ConstraintViolation::getMessage)
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("ConstraintViolationException 추출 도중 에러 발생"));
//
//        return handleExceptionInternalConstraint(e, ErrorStatus.valueOf(errorMessage), HttpHeaders.EMPTY,request);
//    }

//    @Override
//    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
//
//        Map<String, String> errors = new LinkedHashMap<>();
//
//        e.getBindingResult().getFieldErrors().stream()
//                .forEach(fieldError -> {
//                    String fieldName = fieldError.getField();
//                    String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
//                    errors.merge(fieldName, errorMessage, (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
//                });
//
//        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, ErrorStatus._BAD_REQUEST, request, errors);
//    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, ServerWebExchange request) {
        e.printStackTrace();

        return handleExceptionInternalFalse(
                e,
                ErrorStatus._INTERNAL_SERVER_ERROR,
                ErrorStatus._INTERNAL_SERVER_ERROR.getHttpStatus(),
                request,
                e.getMessage()
        );
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> handleGeneralException(GeneralException generalException, ServerWebExchange request) {

        ErrorReasonDTO errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();
        return handleExceptionInternal(
                generalException,
                errorReasonHttpStatus,
                null,
                request);
    }

    // 예외 code, message
    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorReasonDTO reason,
                                                           HttpHeaders headers, ServerWebExchange request) {
        ApiResponse<Object> body = ApiResponse.onFailure(
                reason.getCode(),
                reason.getMessage(),
                null);

        return createResponseEntity(body, headers, reason.getHttpStatus());
    }

    // 예외 code, message, errorPoint
    private ResponseEntity<Object> handleExceptionInternalFalse(Exception e, ErrorStatus errorStatus,
                                                                HttpStatus status, ServerWebExchange request, String errorPoint) {
        ApiResponse<Object> body = ApiResponse.onFailure(
                errorStatus.getCode(),
                errorStatus.getMessage(),
                errorPoint);

        return createResponseEntity(body, HttpHeaders.EMPTY, status);
    }

//    // 예외 code, message, errorArgs
//    private ResponseEntity<Object> handleExceptionInternalArgs(Exception e, HttpHeaders headers, ErrorStatus errorStatus,
//                                                               WebRequest request, Map<String, String> errorArgs) {
//        ApiResponse<Object> body = ApiResponse.onFailure(errorStatus.getCode(),errorStatus.getMessage(), errorArgs);
//        return super.handleExceptionInternal(
//                e,
//                body,
//                headers,
//                errorStatus.getHttpStatus(),
//                request
//        );
//    }
//
//    private ResponseEntity<Object> handleExceptionInternalConstraint(Exception e, ErrorStatus errorStatus,
//                                                                     HttpHeaders headers, WebRequest request) {
//        ApiResponse<Object> body = ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null);
//        return super.handleExceptionInternal(
//                e,
//                body,
//                headers,
//                errorStatus.getHttpStatus(),
//                request
//        );
//    }

    private ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode) {
        return new ResponseEntity(body, headers, statusCode);
    }
}
