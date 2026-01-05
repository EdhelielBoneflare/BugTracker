package uni.bugtracker.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.security.dto.UserDTO;
import uni.bugtracker.backend.security.model.Role;
import uni.bugtracker.backend.security.service.UserService;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserDTO>> getAll(
            @PageableDefault(
                    page = 0,
                    size = 30
            ) Pageable pageable
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        Page<UserDTO> list = userService.getAllUsers(pageable);
        if (list.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(list, responseStatus);
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> changeRole(
            @PathVariable String userId,
            @RequestParam Role role
    ) {
        return ResponseEntity.ok(userService.changeUserRole(userId, role));
    }
}
