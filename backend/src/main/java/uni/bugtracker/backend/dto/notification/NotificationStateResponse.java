package uni.bugtracker.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uni.bugtracker.backend.model.DeveloperNotification;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStateResponse {
    @NotBlank
    private String devId;
    @NotBlank
    private String projectId;

    @NotBlank
    private String state;

    public static NotificationStateResponse fromDevNotif(DeveloperNotification notif) {
        return new NotificationStateResponse(
                notif.getDevId(),
                notif.getProjectId(),
                notif.getState().name()
        );
    }
}
