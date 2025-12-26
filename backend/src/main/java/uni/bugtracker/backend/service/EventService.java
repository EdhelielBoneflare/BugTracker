package uni.bugtracker.backend.service;

import uni.bugtracker.backend.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.dto.event.EventDetailsResponse;
import uni.bugtracker.backend.dto.event.EventRequest;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.Session;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.SessionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final SessionRepository sessionRepository;

    private static final int MAX_LOG = 4_000_000;
    private static final int MAX_NAME = 255;
    private static final int MAX_STACK_TRACE = 4_000_000;


    @Transactional
    public Long createEvent(EventRequest request) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found: " + request.getSessionId())
                );

        Event event = new Event();

        event.setSession(session);
        event.setType(request.getType());
        event.setName(trim(request.getName(), MAX_NAME));
        event.setLog(trim(request.getLog(), MAX_LOG));
        event.setStackTrace(trim(request.getStackTrace(), MAX_STACK_TRACE));
        event.setUrl(request.getUrl());
        event.setElement(request.getElement());
        event.setTimestamp(request.getTimestamp());

        if (request.getMetadata() != null) {
            Event.Metadata metadata = new Event.Metadata();
            metadata.setFileName(request.getMetadata().getFileName());
            metadata.setLineNumber(request.getMetadata().getLineNumber());
            metadata.setStatusCode(request.getMetadata().getStatusCode());
            event.setMetadata(metadata);
        }

        return eventRepository.save(event).getId();
    }

    public EventDetailsResponse getEvent(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found: " + eventId)
                );

        return new EventDetailsResponse(event);
    }

    @Transactional
    public List<EventDetailsResponse> getEventsBySession(Long sessionId) {
        return eventRepository.findAllBySessionId(sessionId)
                .stream()
                .map(EventDetailsResponse::new)
                .toList();
    }

    public Long getProjectIdByEventId(Long eventId) {
        Event event =  eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        Long sessionId = event.getSession().getId();
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session for this event not found")).getProject().getId();
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
