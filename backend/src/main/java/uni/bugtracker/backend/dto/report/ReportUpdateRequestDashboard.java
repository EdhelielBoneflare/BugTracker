package uni.bugtracker.backend.dto.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uni.bugtracker.backend.model.CriticalityLevel;
import uni.bugtracker.backend.model.ReportStatus;

import java.time.Instant;
import java.util.List;

// only for updates on dashboard

@Schema(name = "ReportUpdateRequestDashboard")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportUpdateRequestDashboard {
    @Positive
    private Long projectId;

    @Nullable
    @Schema(nullable = true, description = "Nullable title - null clears the field")
    @Size(min=1, max = 255, message = "Too long report title, max={max}. Or you entered empty title")
    private String title;

    @Nullable
    @Schema(nullable = true, description = "Nullable tags - null clears the field")
    @Size(max = 10, message = "Too many tags, max={max}")
    private List<String> tags;

    private Instant reportedAt;

    @Nullable
    @Schema(nullable = true, description = "Nullable comments - null clears the field")
    @Size(min=1, max = 5000, message = "Too long comments, max={max}. Or you entered empty comments")
    private String comments;

    @Nullable
    @Schema(nullable = true, description = "Nullable developerName - null clears the field")
    private String developerName;

    @Nullable
    @Schema(nullable = true, description = "Nullable criticality level - null clears the field")
    private CriticalityLevel level;

    private ReportStatus status;
    private Boolean userProvided;
}
