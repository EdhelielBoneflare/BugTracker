package uni.bugtracker.backend.security.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.security.CustomUserDetails;
import uni.bugtracker.backend.security.ProjectSecurity;
import uni.bugtracker.backend.security.dto.UserDTO;
import uni.bugtracker.backend.security.model.Role;



@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSecurity projectSecurity;

    public UserDTO changeUserRole(Long userId, Role newRole) {
        Developer user = developerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User nor found with id: " + userId));
        user.setRole(newRole);
        return new UserDTO(developerRepository.save(user));
    }

    public UserDTO getUserByUsername(String username) {
        Developer user = developerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User nor found with username: " + username));
        return new UserDTO(developerRepository.save(user));
    }

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        Page<Developer> usersPage = developerRepository.findAll(pageable);
        return usersPage.map(UserDTO::new);
    }

    @Transactional
    public UserDTO assignProject(
            Long targetUserId,
            Long projectId,
            Authentication authentication
    ) {
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        checkPermission(currentUser, projectId, authentication);

        Developer targetUser = developerRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User nor found with id: " + targetUserId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project nor found with id: " + projectId));
        if (targetUser.getProjects().contains(project)) {
            return new UserDTO(targetUser);
        }
        targetUser.getProjects().add(project);
        return new UserDTO(developerRepository.save(targetUser));
    }

    @Transactional
    public UserDTO removeProject(
            Long targetUserId,
            Long projectId,
            Authentication authentication
    ) {
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        // check if the current user has access to this project
        checkPermission(currentUser, projectId, authentication);

        Developer targetUser = developerRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + targetUserId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));

        // if the target user doesn't have this project - nothing to do
        if (!targetUser.getProjects().contains(project)) {
            return new UserDTO(targetUser);
        }

        targetUser.getProjects().remove(project);

        return new UserDTO(developerRepository.save(targetUser));
    }

    private void checkPermission(
            CustomUserDetails currentUser,
            Long projectId,
            Authentication authentication
    ) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.PM &&
            projectSecurity.hasAccessToProject(projectId, authentication)
        ) {
            return;
        }
        throw new AccessDeniedException("You have no rights to assign this project");
    }
}
