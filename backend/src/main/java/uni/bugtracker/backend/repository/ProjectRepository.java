package uni.bugtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uni.bugtracker.backend.model.Project;

public interface ProjectRepository extends JpaRepository<Project, String> {
}
