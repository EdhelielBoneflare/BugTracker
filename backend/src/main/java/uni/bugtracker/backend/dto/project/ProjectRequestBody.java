package uni.bugtracker.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectRequestBody {
    @NotBlank(message = "Project name cannot be empty")
    private String projectName;
}
