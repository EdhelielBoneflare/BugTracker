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

        // everything else - server error
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(
                        "SERVER_DESERIALIZATION_ERROR",
                        "Internal deserialization error",
                        500
                )
        );
    }

}
