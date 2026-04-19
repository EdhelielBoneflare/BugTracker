package uni.bugtracker.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationSetupRequest {
    @NotBlank
    private String devId;

    @NotBlank
    private String projectId;
}

