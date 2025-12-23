package uni.bugtracker.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest mockWebRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleBaseCustomerException_shouldReturnCorrectResponse() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        BaseCustomerException exception = new BaseCustomerException(errorCode, message, status) {};

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseCustomerException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorCode, response.getBody().getErrorCode());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(status.value(), response.getBody().getStatus());
    }

    @Test
    void handleBusinessValidationException_shouldUseBaseHandler() {
        // Given
        BusinessValidationException exception = new BusinessValidationException("INVALID_ARGUMENT", "Invalid argument");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseCustomerException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ARGUMENT", response.getBody().getErrorCode());
    }

    @Test
    void handleResourceNotFoundException_shouldUseBaseHandler() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseCustomerException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void handleGlobalException_shouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("Unexpected error", response.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturnValidationErrors() {
        // Given
        MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("object", "field1", "Field 1 is required");
        FieldError fieldError2 = new FieldError("object", "field2", "Field 2 must be positive");

        when(mockException.getBindingResult()).thenReturn(mockBindingResult);
        when(mockBindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(mockException);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("field1: Field 1 is required"));
        assertTrue(response.getBody().getMessage().contains("field2: Field 2 must be positive"));
    }

    @Test
    void handleMethodArgumentNotValidException_withEmptyErrors_shouldReturnEmptyMessage() {
        // Given
        MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult = mock(BindingResult.class);

        when(mockException.getBindingResult()).thenReturn(mockBindingResult);
        when(mockBindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(mockException);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        assertEquals("", response.getBody().getMessage());
    }

    @Test
    void handleHttpMessageNotReadableException_withInvalidEnumValue_shouldReturnBadRequest() {
        // Given
        String message = "Cannot deserialize value of type `uni.bugtracker.backend.model.ReportStatus` " +
                "from String \"INVALID_STATUS\": not one of the values accepted for Enum class: " +
                "[NEW, IN_PROGRESS, DONE]";

        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                message,
                null,
                null
        );

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ENUM_VALUE", response.getBody().getErrorCode());
        String responseMessage = response.getBody().getMessage();
        assertTrue(responseMessage.contains("'INVALID_STATUS'") || responseMessage.contains("INVALID_STATUS"));
        assertTrue(responseMessage.contains("status") || responseMessage.contains("field"));
        assertTrue(responseMessage.contains("NEW") || responseMessage.contains("IN_PROGRESS") || responseMessage.contains("DONE"));
    }

    @Test
    void handleHttpMessageNotReadableException_withInvalidCriticalityLevel_shouldReturnBadRequest() {
        // Given
        String message = "Cannot deserialize value of type `uni.bugtracker.backend.model.CriticalityLevel` " +
                "from String \"INVALID_LEVEL\": not one of the values accepted for Enum class: " +
                "[LOW, MEDIUM, HIGH, CRITICAL]";

        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                message,
                null,
                null
        );

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ENUM_VALUE", response.getBody().getErrorCode());
    }

    @Test
    void handleHttpMessageNotReadableException_withInvalidTag_shouldReturnBadRequest() {
        // Given
        String message = "Cannot deserialize value of type `uni.bugtracker.backend.model.Tag` " +
                "from String \"INVALID_TAG\": not one of the values accepted for Enum class: " +
                "[INTERFACE_ISSUE, MOBILE_VIEW]";

        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                message,
                null,
                null
        );

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ENUM_VALUE", response.getBody().getErrorCode());
    }

    @Test
    void handleHttpMessageNotReadableException_withGenericError_shouldReturnInternalServerError() {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Generic error",
                null,
                null
        );

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("SERVER_DESERIALIZATION_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void handleHttpMessageNotReadableException_withNullCause_shouldReturnInternalServerError() {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Error with null cause",
                null,
                null
        );

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("SERVER_DESERIALIZATION_ERROR", response.getBody().getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Cannot deserialize value of type `uni.bugtracker.backend.model.ReportStatus` from String \"INVALID\": not one of the values accepted for Enum class: [NEW, IN_PROGRESS, DONE]",
            "JSON parse error: Cannot deserialize value of type `ReportStatus` from String \"INVALID\": not one of the values accepted for Enum class: [NEW, IN_PROGRESS, DONE]; nested exception is com.fasterxml.jackson.databind.exc.InvalidFormatException",
            "Cannot deserialize value of type `ReportStatus` from String \"INVALID\": not one of the values accepted for Enum class: [NEW, IN_PROGRESS, DONE]"
    })
    void handleHttpMessageNotReadableException_withEnumErrorFormats_shouldReturnBadRequest(String errorMessage) {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                errorMessage,
                null,
                null
        );

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        assertNotNull(response.getBody());
    }

    @Test
    void handleHandlerValidationException_shouldReturnBadRequest() {
        // Given
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.getMessage()).thenReturn("Validation failed for argument [0]");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHandlerValidation(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        assertEquals("Validation failed for argument [0]", response.getBody().getMessage());
    }

    @Test
    void errorResponse_shouldContainTimestampAndPath() {
        // Given
        BusinessValidationException exception = new BusinessValidationException("TEST", "Test message");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseCustomerException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getPath());
        assertNotNull(response.getBody().getPath());
    }

    @Test
    void handleGlobalException_withNullMessage_shouldHandleGracefully() {
        // Given
        Exception exception = new RuntimeException();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        // Сообщение может быть null
        assertNull(response.getBody().getMessage());
    }

    @Test
    void handleHttpMessageNotReadableException_withEmptyMessage_shouldHandleGracefully() {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("", null, null);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJsonErrors(exception);

        // Then
        assertNotNull(response);
        // Unknown format -> 500 error
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("SERVER_DESERIALIZATION_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void handleException_withEmptyStackTrace_shouldHandleGracefully() {
        // Given
        Exception exception = new Exception("Test message") {
            @Override
            public StackTraceElement[] getStackTrace() {
                return new StackTraceElement[0]; // Пустой стектрейс
            }
        };

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(exception, mockWebRequest);

        // Then
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("Test message", response.getBody().getMessage());
    }
}