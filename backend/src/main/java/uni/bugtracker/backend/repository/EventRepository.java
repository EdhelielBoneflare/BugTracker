package uni.bugtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.EventType;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllBySessionId(Long sessionId);

    Optional<Event> findFirstBySessionIdOrderByTimestampDesc(Long sessionId);

    boolean existsBySessionIdAndType(Long sessionId, EventType type);

    void deleteBySessionId(Long sessionId);
}
