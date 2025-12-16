package uni.bugtracker.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.dto.event.EventDetailsResponse;
import uni.bugtracker.backend.dto.event.EventRequest;
import uni.bugtracker.backend.service.EventService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEvent(
            @Valid @RequestBody EventRequest request
    ) {
        Long id = eventService.createEvent(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Event created successfully",
                        "eventId", id
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailsResponse> getEvent(
            @PathVariable Long id
    ) {
        return new ResponseEntity<>(eventService.getEvent(id), HttpStatus.OK);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<EventDetailsResponse>> getEventsBySession(
            @PathVariable Long sessionId
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        List<EventDetailsResponse> listOfEvents = eventService.getEventsBySession(sessionId);
        if (listOfEvents.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(listOfEvents, responseStatus);
    }

}
