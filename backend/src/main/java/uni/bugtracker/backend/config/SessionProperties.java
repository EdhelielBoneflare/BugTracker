package uni.bugtracker.backend.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "session")
@Getter
@Setter
public class SessionProperties {
    // Session live time without events
    @NotNull
    private Duration liveTimeout;

    // How often we check sessions
    @NotNull
    private Duration checkInterval;
}
