package uni.bugtracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uni.bugtracker.backend.model.CriticalityLevel;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.ReportRepository;
import uni.bugtracker.backend.utility.ai_criticality.AIClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CriticalityAnalysisService {
    private final ReportRepository reportRepository;
    private final EventRepository eventRepository;
    private final AIClient aiClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void analyzeAndUpdate(Long reportId) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalStateException("Report not found"));

        List<Event> events =
                eventRepository.findAllBySessionId(report.getSession().getId());

        CriticalityLevel level = determineCriticality(events);

        report.setCriticality(level);
        reportRepository.save(report);
    }

    private CriticalityLevel determineCriticality(List<Event> events) {
        if (events.isEmpty()) {
            return CriticalityLevel.LOW;
        }

        try {
            return aiClient.analyze(events);
        } catch (Exception e) {
            return CriticalityLevel.UNKNOWN;
        }
    }


}
