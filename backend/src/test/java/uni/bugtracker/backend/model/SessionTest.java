package uni.bugtracker.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    private Validator validator;
    private Session session;
    private Project project;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        session = new Session();
        session.setProject(project);
        session.setIsActive(true);
        session.setStartTime(Instant.now());
    }

    @Test
    void createSession_WithValidData_ShouldBeValid() {
        // Arrange
        session.setBrowser("Chrome");
        session.setBrowserVersion("120.0.0.0");
        session.setOs("Windows 10");
        session.setDeviceType("Desktop");
        session.setScreenResolution("1920x1080");
        session.setViewportSize("1900x1000");
        session.setLanguage("en-US");
        session.setUserAgent("Mozilla/5.0");
        session.setIpAddress("192.168.1.1");
        session.setCookiesHash("abc123hash");
        session.setPlugins(Arrays.asList("Adobe Flash", "Java", "Silverlight"));

        // Act
        var violations = validator.validate(session);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createSession_WithNullIsActive_ShouldBeInvalid() {
        // Arrange
        session.setIsActive(null);

        // Act
        var violations = validator.validate(session);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("isActive")
        );
    }

    @Test
    void createSession_WithNullStartTime_ShouldBeInvalid() {
        // Arrange
        session.setStartTime(null);

        // Act
        var violations = validator.validate(session);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("startTime")
        );
    }

    @Test
    void createSession_WithEndTimeBeforeStartTime_ShouldBeValid() {
        // Arrange
        session.setStartTime(Instant.parse("2024-01-01T10:00:00Z"));
        session.setEndTime(Instant.parse("2024-01-01T09:00:00Z")); // Раньше startTime

        // Act
        var violations = validator.validate(session);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createSession_WithPluginsList_ShouldStorePlugins() {
        // Arrange
        List<String> plugins = Arrays.asList("Plugin1", "Plugin2", "Plugin3");
        session.setPlugins(plugins);

        // Act & Assert
        assertThat(session.getPlugins())
                .hasSize(3)
                .containsExactly("Plugin1", "Plugin2", "Plugin3");
    }

    @Test
    void createSession_WithEmptyPluginsList_ShouldBeValid() {
        // Arrange
        session.setPlugins(List.of());

        // Act
        var violations = validator.validate(session);

        // Assert
        assertThat(violations).isEmpty();
        assertThat(session.getPlugins()).isEmpty();
    }

    @Test
    void createSession_WithNullPlugins_ShouldBeValid() {
        // Arrange
        session.setPlugins(null);

        // Act
        var violations = validator.validate(session);

        // Assert
        assertThat(violations).isEmpty();
        assertThat(session.getPlugins()).isNull();
    }
}