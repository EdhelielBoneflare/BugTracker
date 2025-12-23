package uni.bugtracker.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class DeveloperTest {

    private Validator validator;
    private Developer developer;
    private Project project;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        developer = new Developer();
        developer.setUsername("testdeveloper");
        developer.setPassword("securePassword123");
        developer.setProject(project);
    }

    @Test
    void createDeveloper_WithValidData_ShouldBeValid() {
        // Act
        var violations = validator.validate(developer);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createDeveloper_WithUsernameTooLong_ShouldBeInvalid() {
        // Arrange
        developer.setUsername("a".repeat(256)); // More than limit (255 symbols)

        // Act
        var violations = validator.validate(developer);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("username")
        );
    }

    @Test
    void createDeveloper_WithUsernameExactlyAtLimit_ShouldBeValid() {
        // Arrange
        String usernameAtLimit = "a".repeat(255);
        developer.setUsername(usernameAtLimit);

        // Act
        var violations = validator.validate(developer);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createDeveloper_WithNullProject_ShouldBeValid() {
        // Arrange
        developer.setProject(null);

        // Act
        var violations = validator.validate(developer);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createDeveloper_SetAndGetId_ShouldWork() {
        // Arrange
        Integer expectedId = 42;

        // Act
        developer.setId(expectedId);

        // Assert
        assertThat(developer.getId()).isEqualTo(expectedId);
    }

    @Test
    void createDeveloper_UsernameUniqueness_IsNotValidatedByBeanValidation() {
        // Arrange
        Developer developer2 = new Developer();
        developer2.setUsername("testdeveloper");
        developer2.setPassword("anotherPassword");

        // Act
        var violations1 = validator.validate(developer);
        var violations2 = validator.validate(developer2);

        // Assert
        assertThat(violations1).isEmpty();
        assertThat(violations2).isEmpty();
    }
}