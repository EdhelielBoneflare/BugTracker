package uni.bugtracker.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTest {

    private Validator validator;
    private Project project;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        project = new Project();
        project.setName("Test Project");
    }

    @Test
    void createProject_WithValidName_ShouldBeValid() {
        // Act
        var violations = validator.validate(project);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createProject_SetAndGetId_ShouldWork() {
        // Arrange
        Long expectedId = 42L;

        // Act
        project.setId(expectedId);

        // Assert
        assertThat(project.getId()).isEqualTo(expectedId);
    }
}