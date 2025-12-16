package uni.bugtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uni.bugtracker.backend.model.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
}