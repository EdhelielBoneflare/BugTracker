package uni.bugtracker.backend.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;

import java.util.List;
import java.util.Optional;

public interface DeveloperRepository extends JpaRepository<Developer, Long> {
    Optional<Developer> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByIdAndProjects_Id(Long devId, Long projectId);

    Page<Developer> findAll(Pageable pageable);

    @Query("""
        select p
        from Developer d
        join d.projects p
        where d.id = :developerId
    """)
    List<Project> findProjectsByDeveloperId(@Param("developerId") Long developerId);
}
