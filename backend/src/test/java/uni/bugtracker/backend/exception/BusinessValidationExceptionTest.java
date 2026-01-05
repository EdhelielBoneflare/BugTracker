package uni.bugtracker.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class BusinessValidationExceptionTest {

    @Test
    void businessValidationException_shouldHaveCorrectDefaults() {
        // Given
        String errorCode = "INVALID_ARGUMENT";
        String message = "Invalid argument provided";

        // When
        BusinessValidationException exception = new BusinessValidationException(errorCode, message);

        // Then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}