package uni.bugtracker.backend.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import uni.bugtracker.backend.model.CriticalityLevel;
import uni.bugtracker.backend.model.ReportStatus;
import uni.bugtracker.backend.model.Tag;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseCustomerException.class)
    public ResponseEntity<ErrorResponse> handleBaseCustomerException(BaseCustomerException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getStatus().value()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_ERROR",
                ex.getMessage(),
                500
        );
        return ResponseEntity.status(500).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(";\n"));

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                errors,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerValidation(HandlerMethodValidationException ex) {

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonErrors(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        // data format error - client
        if (cause instanceof InvalidFormatException ife) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            "INVALID_FORMAT",
                            "Invalid format: " + ife.getValue(),
                            400
                    )
            );
        }

        // data type hasn't matched -> client fault
        else if (cause instanceof MismatchedInputException mie) {
            String field = mie.getPath().isEmpty() ? null : mie.getPath().get(0).getFieldName();
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            "INVALID_TYPE",
                            "Invalid type or size in field: " + field,
                            400
                    )
            );
        }

        // error - invalid value of enum field in request
        else if (ex.getMessage() != null && ex.getMessage().contains("not one of the values accepted for Enum class")) {
            String message = ex.getMessage();
            String fieldName = extractFieldName(message);
            String invalidValue = extractInvalidValue(message);
            String validValues = extractValidValues(message);
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            "INVALID_ENUM_VALUE",
                            String.format("Invalid enum value: '%s' for '%s' field. Allowed values: %s",
                                    invalidValue, fieldName, validValues),
                            400
                    )
            );
        }

        // everything else - server error
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(
                        "SERVER_DESERIALIZATION_ERROR",
                        "Internal deserialization error",
                        500
                )
        );
    }


    private String extractFieldName(String message) {
        if (message.contains("ReportStatus")) return "status";
        if (message.contains("CriticalityLevel")) return "level";
        if (message.contains("Tag")) return "tags";
        return "field";
    }

    private String extractInvalidValue(String message) {
        int start = message.indexOf("String \"") + 8;
        int end = message.indexOf("\"", start);
        return end > start ? message.substring(start, end) : "unknown";
    }

    private String extractValidValues(String message) {
        int start = message.indexOf("[") + 1;
        int end = message.indexOf("]");
        if (start > 0 && end > start) {
            return message.substring(start, end);
        }
        return "[unknown]";
    }


}
