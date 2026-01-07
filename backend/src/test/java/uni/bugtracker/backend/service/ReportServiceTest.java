package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.dto.report.ReportUpdateRequestDashboard;
import uni.bugtracker.backend.exception.BusinessValidationException;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.*;
import uni.bugtracker.backend.repository.*;
import uni.bugtracker.backend.utility.ReportMapper;
import uni.bugtracker.backend.utility.ai_criticality.ReportCreatedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportService reportService;

    private ReportCreationRequestWidget widgetRequest;
    private ReportUpdateRequestDashboard dashboardRequest;
    private Project project;
    private Session session;
    private Developer developer;
    private Report report;
    private Event event;

    @BeforeEach
    void setUp() {
        // Setup test data
        project = new Project();
        project.setId("project-123");
        project.setName("Test Project");

        session = new Session();
        session.setId(1L);
        session.setProject(project);
        session.setIsActive(true);

        developer = Developer.builder()
                .id("dev-123")
                .username("john.doe")
                .build();

        event = new Event();
        event.setId(100L);
        event.setTimestamp(Instant.now());

        report = new Report();
        report.setId(1L);
        report.setTitle("Test Report");
        report.setProject(project);
        report.setSession(session);
        report.setReportedAt(Instant.now());
        report.setCriticality(CriticalityLevel.UNKNOWN);
        report.setStatus(ReportStatus.NEW);
        report.setTags(new ArrayList<>());

        widgetRequest = new ReportCreationRequestWidget();
        widgetRequest.setProjectId("project-123");
        widgetRequest.setSessionId(1L);
        widgetRequest.setTitle("Test Report");
        widgetRequest.setReportedAt(Instant.now());

        dashboardRequest = new ReportUpdateRequestDashboard();
        dashboardRequest.setTitle("Updated Title");
        dashboardRequest.setDeveloperName("john.doe");
    }

    @Test
    void createReport_shouldSaveReportAndPublishEvent() {
        // Given
        byte[] screen = new byte[]{1, 2, 3};
        List<Event> events = List.of(event);

        when(projectRepository.findById("project-123")).thenReturn(Optional.of(project));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(reportMapper.fromCreateOnWidget(widgetRequest, project, session, screen)).thenReturn(report);
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);
        when(eventRepository.findFirstBySessionIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(event));
        when(reportRepository.save(report)).thenReturn(report);

        // When
        Long reportId = reportService.createReport(widgetRequest, screen);

        // Then
        assertThat(reportId).isEqualTo(1L);
        verify(projectRepository).findById("project-123");
        verify(sessionRepository).findById(1L);
        verify(reportMapper).fromCreateOnWidget(widgetRequest, project, session, screen);
        verify(eventRepository).findAllBySessionId(1L);
        verify(reportMapper).attachEvents(report, events);
        verify(reportRepository).save(report);
        verify(eventPublisher).publishEvent(any(ReportCreatedEvent.class));

        verify(sessionRepository, never()).save(any(Session.class));

        verify(eventRepository).findFirstBySessionIdOrderByTimestampDesc(1L);
    }

    @Test
    void createReport_whenProjectNotFound_shouldThrowException() {
        // Given
        when(projectRepository.findById("project-123")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.createReport(widgetRequest, new byte[]{}))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project doesn't exist");
    }

    @Test
    void updateReportFromDashboard_shouldUpdateReport() {
        // Given
        String rawJson = "{\"title\":\"Updated Title\",\"developerName\":\"john.doe\"}";

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(developerRepository.findByUsername("john.doe")).thenReturn(Optional.of(developer));
        when(reportMapper.updateFromDashboard(eq(report), eq(dashboardRequest), anySet(), eq(null), eq(developer)))
                .thenReturn(report);
        when(reportRepository.save(report)).thenReturn(report);

        // When
        var result = reportService.updateReportFromDashboard(1L, dashboardRequest, rawJson);

        // Then
        assertThat(result).isNotNull();
        verify(reportRepository).findById(1L);
        verify(developerRepository).findByUsername("john.doe");
        verify(reportMapper).updateFromDashboard(any(), any(), anySet(), any(), any());
        verify(reportRepository).save(report);
    }

    @Test
    void updateReportFromDashboard_withInvalidJson_shouldThrowException() {
        // Given
        String invalidJson = "{invalid json";

        // When & Then
        assertThatThrownBy(() -> reportService.updateReportFromDashboard(1L, dashboardRequest, invalidJson))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Unexpected character");
    }

    @Test
    void updateReportFromDashboard_whenReportNotFound_shouldThrowException() {
        // Given
        String rawJson = "{}";
        when(reportRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.updateReportFromDashboard(999L, dashboardRequest, rawJson))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report doesn't exist");
    }

    @Test
    void getReportCard_shouldReturnDTO() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        // When
        var result = reportService.getReportCard(1L);

        // Then
        assertThat(result).isNotNull();
        verify(reportRepository).findById(1L);
    }

    @Test
    void getAllReportsOfProject_shouldReturnPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 1);

        when(reportRepository.findAllByProjectId("project-123", pageable)).thenReturn(reportPage);

        // When
        Page<?> result = reportService.getAllReportsOfProject("project-123", pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(reportRepository).findAllByProjectId("project-123", pageable);
    }

    @Test
    void getAllReportsSolvedOnProject_shouldReturnOnlyDoneReports() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        Report doneReport = new Report();
        doneReport.setId(2L);
        doneReport.setTitle("Done Report");
        doneReport.setProject(project);
        doneReport.setStatus(ReportStatus.DONE);
        doneReport.setCriticality(CriticalityLevel.UNKNOWN);
        doneReport.setTags(new ArrayList<>());

        Page<Report> reportPage = new PageImpl<>(List.of(doneReport), pageable, 1);

        when(reportRepository.findAllByProjectIdAndStatus("project-123", ReportStatus.DONE, pageable))
                .thenReturn(reportPage);

        // When
        Page<?> result = reportService.getAllReportsSolvedOnProject("project-123", pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(reportRepository).findAllByProjectIdAndStatus("project-123", ReportStatus.DONE, pageable);
    }

    @Test
    void deleteReport_shouldDeleteAndReturnDTO() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        // When
        var result = reportService.deleteReport(1L);

        // Then
        assertThat(result).isNotNull();
        verify(reportRepository).findById(1L);
        verify(reportRepository).delete(report);
    }

    @Test
    void getProjectIdByReportId_shouldReturnProjectId() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        // When
        String projectId = reportService.getProjectIdByReportId(1L);

        // Then
        assertThat(projectId).isEqualTo("project-123");
        verify(reportRepository).findById(1L);
    }
}