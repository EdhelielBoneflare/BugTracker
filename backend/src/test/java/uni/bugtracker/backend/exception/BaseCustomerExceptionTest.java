package uni.bugtracker.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class BaseCustomerExceptionTest {

    @Test
    void baseCustomerException_shouldStoreAllFieldsCorrectly() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // When
        BaseCustomerException exception = new BaseCustomerException(errorCode, message, status) {
            // Anonymous subclass for testing abstract class
        };

        // Then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertEquals(status, exception.getStatus());
    }

    @Test
    void baseCustomerException_shouldBeRuntimeException() {
        // When
        BaseCustomerException exception = new BaseCustomerException("TEST", "Message", HttpStatus.BAD_REQUEST) {};

        // Then
        assertInstanceOf(RuntimeException.class, exception);
    }
}