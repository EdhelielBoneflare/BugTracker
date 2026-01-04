package uni.bugtracker.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.dto.project.ProjectRequestBody;
import uni.bugtracker.backend.dto.session.ProjectCreationUpdatingResponse;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.security.dto.UserDTO;
import uni.bugtracker.backend.security.service.UserService;
import uni.bugtracker.backend.service.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final UserService userService;
    private final ProjectService projectService;

    @PatchMapping("/users/{userId}/projects/assign/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<UserDTO> assignProject(
            @PathVariable @NotNull @NotBlank String userId,
            @PathVariable @NotNull @NotBlank String projectId,
            Authentication auth
    ) {
        return ResponseEntity.ok(userService.assignProject(userId, projectId, auth));
    }

    @PatchMapping("/users/{userId}/projects/remove/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PM')")
    public ResponseEntity<UserDTO> removeProject(
            @PathVariable @NotNull @NotBlank String userId,
            @PathVariable @NotNull @NotBlank String projectId,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                userService.removeProject(userId, projectId, auth)
        );
    }

    @GetMapping("/my/projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Project>> getAllProjectsForCurrentUser(
            Authentication authentication
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        List<Project> list = projectService.getProjectsForCurrentUser(authentication);
        if (list.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(list, responseStatus);
    }

    @PostMapping("/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectCreationUpdatingResponse> addProject(
            @RequestBody @Valid ProjectRequestBody request
    ) {
        Project project = projectService.createProject(request);
        ProjectCreationUpdatingResponse response = new ProjectCreationUpdatingResponse(
                "Project created", project.getId()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Project>> getProjects() {
        HttpStatus responseStatus = HttpStatus.OK;
        List<Project> list = projectService.getAllProjects();
        if (list.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(list, responseStatus);
    }

    @PatchMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectCreationUpdatingResponse> updateProject(
            @PathVariable @NotNull @NotBlank String projectId,
            @RequestBody @Valid ProjectRequestBody request
    ) {
        Project project = projectService.updateProject(projectId, request);
        ProjectCreationUpdatingResponse response = new ProjectCreationUpdatingResponse(
                "Project updated", project.getId()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Project> deleteProject(
            @PathVariable @NotNull @NotBlank String projectId
    ) {
        return ResponseEntity.ok(projectService.deleteProject(projectId));
    }
}
