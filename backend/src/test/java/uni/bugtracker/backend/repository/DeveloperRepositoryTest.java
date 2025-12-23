package uni.bugtracker.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeveloperRepositoryTest {

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void findByUsername_shouldReturnDeveloperWhenExists() {
        // Arrange
        Project project = new Project();
        project.setName("Test Project");
        projectRepository.save(project);

        Developer developer = new Developer();
        developer.setUsername("john.doe");
        developer.setPassword("encodedPassword");
        developer.setProject(project);
        developerRepository.save(developer);

        // Act
        Optional<Developer> found = developerRepository.findByUsername("john.doe");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john.doe");
        assertThat(found.get().getProject().getName()).isEqualTo("Test Project");
    }

    @Test
    void findByUsername_shouldReturnEmptyWhenNotExists() {
        // Act
        Optional<Developer> found = developerRepository.findByUsername("non.existent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findByUsername_shouldBeCaseSensitive() {
        // Arrange
        Developer developer = new Developer();
        developer.setUsername("John.Doe");
        developer.setPassword("password");
        developerRepository.save(developer);

        // Act & Assert
        assertThat(developerRepository.findByUsername("john.doe")).isEmpty();
        assertThat(developerRepository.findByUsername("John.Doe")).isPresent();
    }
}