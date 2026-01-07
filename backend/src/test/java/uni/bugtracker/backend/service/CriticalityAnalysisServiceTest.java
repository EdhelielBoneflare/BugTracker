package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uni.bugtracker.backend.model.*;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.ReportRepository;
import uni.bugtracker.backend.utility.ai_criticality.AIClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriticalityAnalysisServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AIClient aiClient;

    @InjectMocks
    private CriticalityAnalysisService analysisService;

    private Report report;
    private List<Event> events;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId("project-123");

        Session session = new Session();
        session.setId(1L);
        session.setProject(project);

        report = new Report();
        report.setId(100L);
        report.setSession(session);
        report.setCriticality(CriticalityLevel.UNKNOWN);

        Event event = new Event();
        event.setId(1L);
        event.setType(EventType.ERROR);
        event.setName("Error");
        events = List.of(event);
    }

    @Test
    void analyzeAndUpdate_shouldAnalyzeEventsAndUpdateReport() {
        // Given
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);
        when(aiClient.analyze(events)).thenReturn(CriticalityLevel.HIGH);
        when(reportRepository.save(report)).thenReturn(report);

        // When
        analysisService.analyzeAndUpdate(100L);

        // Then
        assertThat(report.getCriticality()).isEqualTo(CriticalityLevel.HIGH);
        verify(reportRepository).findById(100L);
        verify(eventRepository).findAllBySessionId(1L);
        verify(aiClient).analyze(events);
        verify(reportRepository).save(report);
    }

    @Test
    void analyzeAndUpdate_whenReportNotFound_shouldThrowException() {
        // Given
        when(reportRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> analysisService.analyzeAndUpdate(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Report not found");
    }

    @Test
    void analyzeAndUpdate_whenNoEvents_shouldSetLowCriticality() {
        // Given
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(eventRepository.findAllBySessionId(1L)).thenReturn(List.of());

        // When
        analysisService.analyzeAndUpdate(100L);

        // Then
        assertThat(report.getCriticality()).isEqualTo(CriticalityLevel.LOW);
        verify(aiClient, never()).analyze(anyList());
    }

    @Test
    void analyzeAndUpdate_whenAIClientThrowsException_shouldSetUnknownCriticality() {
        // Given
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);
        when(aiClient.analyze(events)).thenThrow(new RuntimeException("AI error"));

        // When
        analysisService.analyzeAndUpdate(100L);

        // Then
        assertThat(report.getCriticality()).isEqualTo(CriticalityLevel.UNKNOWN);
    }

    @Test
    void analyzeAndUpdate_whenEventsExist_shouldCallAIClient() {
        // Given
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);
        when(aiClient.analyze(events)).thenReturn(CriticalityLevel.MEDIUM);

        // When
        analysisService.analyzeAndUpdate(100L);

        // Then
        verify(aiClient).analyze(events);
        assertThat(report.getCriticality()).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void analyzeAndUpdate_whenNoEvents_shouldSetLowWithoutAIClient() {
        // Given
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(eventRepository.findAllBySessionId(1L)).thenReturn(List.of());

        // When
        analysisService.analyzeAndUpdate(100L);

        // Then
        verify(aiClient, never()).analyze(anyList());
        assertThat(report.getCriticality()).isEqualTo(CriticalityLevel.LOW);
    }
}