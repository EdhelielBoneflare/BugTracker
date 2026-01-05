package uni.bugtracker.backend.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionCreationResponse {
    private String message;
    private Long sessionId;
}
