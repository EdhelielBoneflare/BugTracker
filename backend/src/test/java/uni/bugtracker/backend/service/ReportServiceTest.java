package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.dto.report.ReportUpdateRequestDashboard;
import uni.bugtracker.backend.exception.BusinessValidationException;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.*;
import uni.bugtracker.backend.repository.*;
import uni.bugtracker.backend.utility.ReportMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ReportMapper reportMapper;

    @InjectMocks
    private ReportService reportService;

    private Project project;
    private Session session;
    private Report report;
    private Developer developer;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        session = new Session();
        session.setId(1L);
        session.setProject(project);

        report = new Report();
        report.setId(100L);
        report.setProject(project);
        report.setSession(session);
        report.setReportedAt(Instant.now());
        report.setStatus(ReportStatus.NEW);
        report.setUserProvided(false);
        report.setTags(List.of(Tag.BROKEN_LINK));
        report.setRelatedEventIds(List.of(1L, 2L));
        report.setTitle("Test Title");
        report.setComments("Test Comments");
        report.setCurrentUrl("https://example.com");

        developer = new Developer();
        developer.setId(1L);
        developer.setUsername("dev1");
        developer.setPassword("password");
    }

    @Test
    void createReport_WithValidRequest_ShouldCreateReport() {
        // Arrange
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setProjectId(1L);
        request.setSessionId(1L);
        request.setTitle("Test Title");
        request.setUserProvided(true);
        request.setReportedAt(Instant.now());

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        Report mappedReport = new Report();
        mappedReport.setId(100L);
        mappedReport.setProject(project);
        mappedReport.setSession(session);
        mappedReport.setTags(List.of(Tag.BROKEN_LINK));
        when(reportMapper.fromCreateOnWidget(eq(request), eq(project), eq(session)))
                .thenReturn(mappedReport);

        List<Event> events = Arrays.asList(new Event(), new Event());
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);

        when(reportRepository.save(mappedReport)).thenReturn(mappedReport);

        // Act
        Long reportId = reportService.createReport(request);

        // Assert
        assertThat(reportId).isEqualTo(100L);
        verify(projectRepository).findById(1L);
        verify(sessionRepository).findById(1L);
        verify(reportMapper).fromCreateOnWidget(request, project, session);
        verify(reportMapper).attachEvents(mappedReport, events);
        verify(reportRepository).save(mappedReport);
    }

    @Test
    void createReport_WithNonExistentProject_ShouldThrowException() {
        // Arrange
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setProjectId(999L);
        request.setSessionId(1L);

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reportService.createReport(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project doesn't exist");
    }

    @Test
    void getReport_WithExistingId_ShouldReturnReport() {
        // Arrange
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        // Act
        Report result = reportService.getReport(100L);

        // Assert
        assertThat(result).isEqualTo(report);
        verify(reportRepository).findById(100L);
    }

    @Test
    void getReport_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reportService.getReport(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report doesn't exist");
    }

    @Test
    void updateReportFromDashboard_WithValidRequest_ShouldUpdateReport() {
        // Arrange
        String rawJson = "{\"projectId\": 1, \"developerName\": \"dev1\"}";
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setProjectId(1L);
        request.setDeveloperName("dev1");

        Report updatedReport = new Report();
        updatedReport.setId(100L);
        updatedReport.setProject(project);
        updatedReport.setSession(session);
        updatedReport.setTags(List.of(Tag.BROKEN_LINK));

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(developerRepository.findByUsername("dev1")).thenReturn(Optional.of(developer));

        when(reportMapper.updateFromDashboard(eq(report), eq(request), anySet(), eq(project), eq(developer)))
                .thenReturn(updatedReport);

        when(reportRepository.save(updatedReport)).thenReturn(updatedReport);

        // Act
        var result = reportService.updateReportFromDashboard(100L, request, rawJson);

        // Assert
        assertThat(result).isNotNull();
        verify(reportRepository).findById(100L);
        verify(projectRepository).findById(1L);
        verify(developerRepository).findByUsername("dev1");
        verify(reportMapper).updateFromDashboard(eq(report), eq(request), anySet(), eq(project), eq(developer));
    }

    @Test
    void updateReportFromDashboard_WithInvalidJson_ShouldThrowException() {
        // Arrange
        String invalidJson = "{invalid json";
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();

        // Act & Assert
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> reportService.updateReportFromDashboard(100L, request, invalidJson)
        );

        assertThat(exception.toString())
                .contains("BusinessValidationException");
    }

    @Test
    void updateReportFromDashboard_WithProjectIdNull_ShouldThrowException() {
        // Arrange
        String rawJson = "{\"projectId\": null}";
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setProjectId(null);

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        // Act & Assert
        assertThatThrownBy(() -> reportService.updateReportFromDashboard(100L, request, rawJson))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("projectId cannot be null");
    }

    @Test
    void getAllReportsOfProject_ShouldReturnPage() {
        // Arrange
        Pageable pageable = Pageable.ofSize(10);
        Report reportWithData = createCompleteReport();

        Page<Report> reportPage = new PageImpl<>(List.of(reportWithData));

        when(reportRepository.findAllByProjectId(1L, pageable)).thenReturn(reportPage);

        // Act
        var result = reportService.getAllReportsOfProject(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(reportRepository).findAllByProjectId(1L, pageable);
    }

    @Test
    void getAllReportsSolvedOnProject_ShouldReturnPage() {
        // Arrange
        Pageable pageable = Pageable.ofSize(10);
        Report reportWithData = createCompleteReport();
        reportWithData.setStatus(ReportStatus.DONE);

        Page<Report> reportPage = new PageImpl<>(List.of(reportWithData));

        when(reportRepository.findAllByProjectIdAndStatus(1L, ReportStatus.DONE, pageable))
                .thenReturn(reportPage);

        // Act
        var result = reportService.getAllReportsSolvedOnProject(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(reportRepository).findAllByProjectIdAndStatus(1L, ReportStatus.DONE, pageable);
    }

    @Test
    void deleteReport_WithExistingId_ShouldDeleteAndReturnDTO() {
        // Arrange
        Report reportWithData = createCompleteReport();

        when(reportRepository.findById(100L)).thenReturn(Optional.of(reportWithData));

        // Act
        var result = reportService.deleteReport(100L);

        // Assert
        assertThat(result).isNotNull();
        verify(reportRepository).findById(100L);
        verify(reportRepository).delete(reportWithData);
    }

    @Test
    void getReportCard_WithExistingId_ShouldReturnDTO() {
        // Arrange
        Report reportWithData = createCompleteReport();

        when(reportRepository.findById(100L)).thenReturn(Optional.of(reportWithData));

        // Act
        var result = reportService.getReportCard(100L);

        // Assert
        assertThat(result).isNotNull();
        verify(reportRepository).findById(100L);
    }

    @Test
    void getReportCard_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reportService.getReportCard(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report doesn't exist");
    }

    @Test
    void updateReportFromDashboard_WithDeveloperNameNull_ShouldNotSearchDeveloper() {
        // Arrange
        String rawJson = "{\"developerName\": null}";
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setDeveloperName(null);

        Report updatedReport = createCompleteReport();

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(reportMapper.updateFromDashboard(eq(report), eq(request), anySet(), isNull(), isNull()))
                .thenReturn(updatedReport);
        when(reportRepository.save(updatedReport)).thenReturn(updatedReport);

        // Act
        var result = reportService.updateReportFromDashboard(100L, request, rawJson);

        // Assert
        assertThat(result).isNotNull();
        verify(developerRepository, never()).findByUsername(anyString());
    }

    @Test
    void updateReportFromDashboard_WithoutProjectIdInJson_ShouldNotUpdateProject() {
        // Arrange
        String rawJson = "{\"developerName\": \"dev1\"}";
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setDeveloperName("dev1");

        Report updatedReport = createCompleteReport();

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(developerRepository.findByUsername("dev1")).thenReturn(Optional.of(developer));
        when(reportMapper.updateFromDashboard(eq(report), eq(request), anySet(), isNull(), eq(developer)))
                .thenReturn(updatedReport);
        when(reportRepository.save(updatedReport)).thenReturn(updatedReport);

        // Act
        var result = reportService.updateReportFromDashboard(100L, request, rawJson);

        // Assert
        assertThat(result).isNotNull();
        verify(projectRepository, never()).findById(anyLong());
    }

    @Test
    void updateReportFromDashboard_WithEmptyJson_ShouldUpdateWithDefaultFields() {
        // Arrange
        String rawJson = "{}";
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();

        Report updatedReport = createCompleteReport();

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(reportMapper.updateFromDashboard(eq(report), eq(request), eq(Set.of()), isNull(), isNull()))
                .thenReturn(updatedReport);
        when(reportRepository.save(updatedReport)).thenReturn(updatedReport);

        // Act
        var result = reportService.updateReportFromDashboard(100L, request, rawJson);

        // Assert
        assertThat(result).isNotNull();
        verify(reportRepository).findById(100L);
        verify(projectRepository, never()).findById(anyLong());
        verify(developerRepository, never()).findByUsername(anyString());
    }

    // Method for creating full report
    private Report createCompleteReport() {
        Report completeReport = new Report();
        completeReport.setId(100L);
        completeReport.setProject(project);
        completeReport.setSession(session);
        completeReport.setReportedAt(Instant.now());
        completeReport.setStatus(ReportStatus.NEW);
        completeReport.setUserProvided(false);
        completeReport.setTags(List.of(Tag.BROKEN_LINK));
        completeReport.setRelatedEventIds(List.of(1L, 2L));
        completeReport.setTitle("Test Title");
        completeReport.setComments("Test Comments");
        completeReport.setCurrentUrl("https://example.com");
        completeReport.setCriticality(CriticalityLevel.MEDIUM);
        return completeReport;
    }
}