package uni.bugtracker.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.config.SessionProperties;
import uni.bugtracker.backend.model.*;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.ReportRepository;
import uni.bugtracker.backend.repository.SessionRepository;
import uni.bugtracker.backend.utility.ai_criticality.ReportCreatedEvent;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionAutoGenerationReportJob {
    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final ReportRepository reportRepository;
    private final SessionProperties sessionProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "#{@sessionProperties.checkInterval.toMillis()}")
    @Transactional
    public void handleExpiredSessions() {
        // if the last event is later than this time (this means that live_timeout has passed) -> report/delete events
        Instant deadline = Instant.now().minus(sessionProperties.getLiveTimeout());

        List<Session> expiredSessions = sessionRepository.findExpiredSessions(deadline);
        for (Session session : expiredSessions) {
            processSession(session);
        }
    }

    private void processSession(Session session) {
        boolean hasError = eventRepository.existsBySessionIdAndType(
                session.getId(), EventType.ERROR
        );

        if (hasError) {
            createAutoReport(session);
        } else {
            eventRepository.deleteBySessionId(session.getId());
        }

        closeSession(session);
    }

    private void createAutoReport(Session session) {
        Report report = new Report();

        report.setProject(session.getProject());
        report.setSession(session);
        report.setReportedAt(Instant.now());
        report.setUserProvided(false);
        report.setCriticality(CriticalityLevel.UNKNOWN);

        List<Event> events = eventRepository.findAllBySessionId(session.getId());
        attachEvents(report, events);

        reportRepository.save(report);

        eventPublisher.publishEvent(
                new ReportCreatedEvent(report.getId())
        );
    }

    private void closeSession(Session session) {
        session.setIsActive(false);
        session.setEndTime(
                eventRepository
                        .findFirstBySessionIdOrderByTimestampDesc(session.getId())
                        .map(Event::getTimestamp)
                        .orElse(Instant.now())
        );
    }

    private void attachEvents(Report report, List<Event> events) {
        report.setRelatedEventIds(
                events.stream()
                        .map(Event::getId)
                        .toList()
        );
    }
}
