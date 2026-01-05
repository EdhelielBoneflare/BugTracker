package uni.bugtracker.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.model.ReportStatus;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    //filter by projectId

    /*filter by PROJECT_ID, date/devName/tags/level/status
    * */

    Page<Report> findAllByProjectId(String projectId, Pageable pageable);
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);
    Page<Report> findAllByProjectIdAndStatus(String projectId, ReportStatus status, Pageable pageable);
}
