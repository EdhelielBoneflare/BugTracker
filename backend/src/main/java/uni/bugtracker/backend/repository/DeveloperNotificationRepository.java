package uni.bugtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uni.bugtracker.backend.model.DeveloperNotification;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperNotificationRepository extends JpaRepository<DeveloperNotification, Long> {

    @Query("SELECT d FROM DeveloperNotification d WHERE d.projectId = ?1 and d.state in ?2")
    List<DeveloperNotification> findByProjectIdAndStates(String projectId, List<DeveloperNotification.State> state);

    @Query("SELECT d FROM DeveloperNotification d WHERE d.regToken = ?1 and d.state in ?2")
    Optional<DeveloperNotification> findByRegTokenAndStates(String regToken, List<DeveloperNotification.State> state);

    @Query("SELECT d FROM DeveloperNotification d WHERE d.projectId = ?1")
    List<DeveloperNotification> findByProjectId(String projectId);

    @Query("SELECT d FROM DeveloperNotification d WHERE d.devId = ?1 and d.projectId = ?2")
    Optional<DeveloperNotification> findByDevIdAndProjectId(String devId, String projectId);
}
