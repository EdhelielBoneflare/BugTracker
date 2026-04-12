package uni.bugtracker.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationSetupResponse {
    private String setupUrl;
}

