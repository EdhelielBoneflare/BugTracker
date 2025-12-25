package uni.bugtracker.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.dto.session.SessionCreationResponse;
import uni.bugtracker.backend.dto.session.SessionDetailsResponse;
import uni.bugtracker.backend.dto.session.SessionRequest;
import uni.bugtracker.backend.service.SessionService;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionCreationResponse> createSession(
            @Valid @RequestBody SessionRequest request
    ) {
        return new ResponseEntity<>(sessionService.createSession(request),
                HttpStatus.CREATED);
    }

    @PreAuthorize("@projectSecurity.hasAccessToProject(@sessionService.getProjectIdBySessionId(#id), authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailsResponse> getSession(
            @PathVariable Long id
    ) {
        return new ResponseEntity<>(sessionService.getSession(id), HttpStatus.OK);
    }
}
