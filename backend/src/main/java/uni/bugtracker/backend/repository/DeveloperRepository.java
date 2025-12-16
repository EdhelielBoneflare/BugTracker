package uni.bugtracker.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import uni.bugtracker.backend.model.Developer;

import java.util.Optional;

public interface DeveloperRepository extends JpaRepository<Developer, Long> {
    Optional<Developer> findByUsername(String username);
}
