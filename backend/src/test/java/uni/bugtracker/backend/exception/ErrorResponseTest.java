package uni.bugtracker.backend.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void errorResponse_shouldCreateWithCorrectFields() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        int status = 400;

        // When
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, status);

        // Then
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(status, errorResponse.getStatus());
        assertNotNull(errorResponse.getTimestamp());
        assertNotNull(errorResponse.getPath());
    }

    @Test
    void errorResponse_withGetters_shouldReturnCorrectValues() {
        // Given
        ErrorResponse errorResponse = new ErrorResponse("CODE", "Message", 404);

        // When & Then
        assertEquals("CODE", errorResponse.getErrorCode());
        assertEquals("Message", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
        assertNotNull(errorResponse.getTimestamp());
        // Path depends on request context, may be "unknown" in unit tests
    }
}