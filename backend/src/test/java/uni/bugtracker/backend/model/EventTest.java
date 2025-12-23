package uni.bugtracker.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    private Validator validator;
    private Event event;
    private Session session;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        Project project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        session = new Session();
        session.setId(1L);
        session.setProject(project);
        session.setStartTime(Instant.now());
        session.setIsActive(true);

        event = new Event();
        event.setSession(session);
        event.setType(EventType.ERROR);
        event.setTimestamp(Instant.now());
    }

    @Test
    void createEvent_WithValidData_ShouldBeValid() {
        // Arrange
        event.setName("JavaScript Error");
        event.setLog("TypeError: Cannot read property 'x' of undefined");
        event.setUrl("https://example.com/page");
        event.setElement("#submit-button");

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(EventType.class)
    void createEvent_WithAllEventTypes_ShouldBeValid(EventType type) {
        // Arrange
        event.setType(type);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("type"))
                .isEmpty();
    }

    @Test
    void createEvent_WithNullType_ShouldBeValid() {
        // Arrange
        event.setType(null);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("type"))
                .isEmpty();
    }

    @Test
    void createEvent_WithStackTraceTooLarge_ShouldBeInvalid() {
        // Arrange
        String largeStackTrace = "A".repeat(4_000_001); // Максимум 4,000,000 символов
        event.setStackTrace(largeStackTrace);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("stackTrace"))
                .isNotEmpty();
    }

    @Test
    void createEvent_WithLogTooLarge_ShouldBeInvalid() {
        // Arrange
        String largeLog = "A".repeat(4_000_001); // Максимум 4,000,000 символов
        event.setLog(largeLog);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("log"))
                .isNotEmpty();
    }

    @Test
    void createEvent_WithLogExactlyAtLimit_ShouldBeValid() {
        // Arrange
        String logAtLimit = "A".repeat(4_000_000);
        event.setLog(logAtLimit);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("log"))
                .isEmpty();
    }

    @Test
    void createEvent_WithStackTraceExactlyAtLimit_ShouldBeValid() {
        // Arrange
        String stackTraceAtLimit = "A".repeat(4_000_000);
        event.setStackTrace(stackTraceAtLimit);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("stackTrace"))
                .isEmpty();
    }

    @Test
    void createEvent_WithMetadata_ShouldStoreMetadata() {
        // Arrange
        Event.Metadata metadata = new Event.Metadata();
        metadata.setFileName("app.js");
        metadata.setLineNumber("42");
        metadata.setStatusCode("404");

        event.setMetadata(metadata);

        // Act & Assert
        assertThat(event.getMetadata()).isNotNull();
        assertThat(event.getMetadata().getFileName()).isEqualTo("app.js");
        assertThat(event.getMetadata().getLineNumber()).isEqualTo("42");
        assertThat(event.getMetadata().getStatusCode()).isEqualTo("404");
    }

    @Test
    void createEvent_WithoutMetadata_ShouldBeValid() {
        // Arrange
        event.setMetadata(null);

        // Act
        var violations = validator.validate(event);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("metadata"))
                .isEmpty();
    }

    @Test
    void createEvent_MetadataObject_ShouldBeMutable() {
        // Arrange
        Event.Metadata metadata = new Event.Metadata();
        metadata.setFileName("initial.js");

        event.setMetadata(metadata);

        // Act
        event.getMetadata().setFileName("updated.js");

        // Assert
        assertThat(event.getMetadata().getFileName()).isEqualTo("updated.js");
    }
}