package uni.bugtracker.backend.dto.session;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

// only create

@Data
public class SessionRequest {
    @NotNull
    @NotBlank
    private String projectId;

    @NotNull
    @PastOrPresent(message = "Session startTime cannot be in the future")
    private Instant startTime;

    private String browser;

    private String browserVersion;

    private String os;

    @Pattern(regexp = "^(?i)(desktop|mobile|tablet)$",
            message = "Unknown device type")
    private String deviceType;

    @Pattern(regexp = "^\\d+x\\d+$",
            message = "screenResolution invalid format (1920x1080)")
    private String screenResolution;

    @Pattern(regexp = "^\\d+x\\d+$",
            message = "viewportSize invalid format (1920x947)")
    private String viewportSize;

    private String language;
    private String userAgent;

    @Pattern(regexp = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "ipAddress invalid IP format")
    private String ipAddress;

    private String cookiesHash;
    List<String> plugins;

}
