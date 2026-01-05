package uni.bugtracker.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uni.bugtracker.backend.model.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        project1 = new Project();
        project1.setName("Project 1");
        projectRepository.save(project1);

        project2 = new Project();
        project2.setName("Project 2");
        projectRepository.save(project2);
    }

    @Test
    void findAllByProjectId_shouldFilterByProject() {
        // Arrange
        createReport(project1, ReportStatus.NEW);
        createReport(project1, ReportStatus.IN_PROGRESS);
        createReport(project2, ReportStatus.NEW); // Другой проект

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Report> project1Reports = reportRepository.findAllByProjectId(project1.getId(), pageable);
        Page<Report> project2Reports = reportRepository.findAllByProjectId(project2.getId(), pageable);

        // Assert
        assertThat(project1Reports.getContent()).hasSize(2);
        assertThat(project2Reports.getContent()).hasSize(1);
    }

    @Test
    void findAllByStatus_shouldFilterByStatus() {
        // Arrange
        createReport(project1, ReportStatus.NEW);
        createReport(project1, ReportStatus.NEW);
        createReport(project2, ReportStatus.IN_PROGRESS);
        createReport(project2, ReportStatus.DONE);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Report> newReports = reportRepository.findAllByStatus(ReportStatus.NEW, pageable);
        Page<Report> inProgressReports = reportRepository.findAllByStatus(ReportStatus.IN_PROGRESS, pageable);

        // Assert
        assertThat(newReports.getContent()).hasSize(2);
        assertThat(inProgressReports.getContent()).hasSize(1);
    }

    @Test
    void findAllByProjectIdAndStatus_shouldFilterByBoth() {
        // Arrange
        createReport(project1, ReportStatus.NEW);
        createReport(project1, ReportStatus.IN_PROGRESS);
        createReport(project1, ReportStatus.NEW);
        createReport(project2, ReportStatus.NEW);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Report> result = reportRepository.findAllByProjectIdAndStatus(
                project1.getId(), ReportStatus.NEW, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(report -> report.getProject().getId().equals(project1.getId()))
                .allMatch(report -> report.getStatus() == ReportStatus.NEW);
    }

    @Test
    void findAllByProjectId_withPagination_shouldReturnPage() {
        // 15 reports for project1
        for (int i = 0; i < 15; i++) {
            createReport(project1, ReportStatus.NEW);
        }

        // Act
        Page<Report> page1 = reportRepository.findAllByProjectId(project1.getId(), PageRequest.of(0, 5));
        Page<Report> page2 = reportRepository.findAllByProjectId(project1.getId(), PageRequest.of(1, 5));
        Page<Report> page3 = reportRepository.findAllByProjectId(project1.getId(), PageRequest.of(2, 5));

        // Assert
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page3.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    private void createReport(Project project, ReportStatus status) {
        Session session = new Session();
        session.setProject(project);
        session.setIsActive(true);
        session.setStartTime(Instant.now());
        sessionRepository.save(session);

        Report report = new Report();
        report.setProject(project);
        report.setSession(session);
        report.setTitle("Test Report");
        report.setReportedAt(Instant.now());
        report.setStatus(status);
        report.setUserProvided(true);

        reportRepository.save(report);
    }
}