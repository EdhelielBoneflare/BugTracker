package uni.bugtracker.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uni.bugtracker.backend.utility.ai_criticality.ReportCreatedEvent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportCriticalityListenerTest {

    @Mock
    private CriticalityAnalysisService analysisService;

    @InjectMocks
    private ReportCriticalityListener listener;

    @Test
    void handle_shouldCallAnalysisService() {
        // Given
        ReportCreatedEvent event = new ReportCreatedEvent(100L);

        // When
        listener.handle(event);

        // Then
        verify(analysisService).analyzeAndUpdate(100L);
    }

    @Test
    void handle_whenAnalysisServiceThrowsException_shouldPropagate() {
        // Given
        ReportCreatedEvent event = new ReportCreatedEvent(100L);
        RuntimeException expectedException = new RuntimeException("Analysis failed");
        doThrow(expectedException).when(analysisService).analyzeAndUpdate(100L);

        // When & Then
        assertThatThrownBy(() -> listener.handle(event))
                .isSameAs(expectedException);

        verify(analysisService).analyzeAndUpdate(100L);
    }
}