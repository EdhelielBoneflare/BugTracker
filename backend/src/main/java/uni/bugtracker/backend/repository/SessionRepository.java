package uni.bugtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uni.bugtracker.backend.model.Session;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("""
            select s from Session s
                where s.isActive = true
                    and s.endTime is null
                    and coalesce(
                            (select max(e.timestamp) from Event e where e.session = s),
                            s.startTime
                        ) < :deadline
            """)
    List<Session> findExpiredSessions(@Param("deadline") Instant deadline);
}