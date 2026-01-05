package uni.bugtracker.backend.dto.session;

import lombok.Data;
import uni.bugtracker.backend.model.Session;

import java.time.Instant;
import java.util.List;

@Data
public class SessionDetailsResponse {
    private Long sessionId;
    private String projectId;
    private Boolean isActive;
    private Instant startTime;
    private Instant endTime;
    private String browser;
    private String browserVersion;
    private String os;
    private String deviceType;
    private String screenResolution;
    private String viewportSize;
    private String language;
    private String userAgent;
    private String ipAddress;
    private List<String> plugins;

    public SessionDetailsResponse(Session session) {
        this.sessionId = session.getId();
        this.projectId = session.getProject().getId();
        this.isActive = session.getIsActive();
        this.startTime = session.getStartTime();
        this.endTime = session.getEndTime();
        this.browser = session.getBrowser();
        this.browserVersion = session.getBrowserVersion();
        this.os = session.getOs();
        this.deviceType = session.getDeviceType();
        this.screenResolution = session.getScreenResolution();
        this.viewportSize = session.getViewportSize();
        this.language = session.getLanguage();
        this.userAgent = session.getUserAgent();
        this.ipAddress = session.getIpAddress();
        this.plugins = session.getPlugins();
    }
}
