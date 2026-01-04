package uni.bugtracker.backend.dto.report;


import lombok.Getter;
import uni.bugtracker.backend.model.Report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ReportCardDTO {
    private Long id;
    private String projectId;
    private Long sessionId;
    private String title;
    private List<String> tags;
    private Instant reportedAt;
    private String comments;
    private String userEmail;
    private String screen;
    private String currentUrl;
    private Boolean userProvided;
    private List<Long> eventIDs;
    private String level;
    private String status;
    private String developerName;


    public ReportCardDTO(Report report) {
        this.id = report.getId();
        this.projectId = report.getProject().getId();
        this.sessionId = report.getSession().getId();
        this.title = report.getTitle();
        this.tags = report.getTags().stream()
                .map(Enum::name)
                .toList();
        this.reportedAt = report.getReportedAt();
        this.comments = report.getComments();
        this.userEmail = report.getUserEmail();
        this.screen = report.getScreen();
        this.currentUrl = report.getCurrentUrl();
        this.userProvided = report.isUserProvided();
        this.eventIDs = report.getRelatedEventIds() == null
                ? List.of()
                : new ArrayList<>(report.getRelatedEventIds());
        this.level = report.getCriticality() != null
                ? report.getCriticality().name()
                : null;
        this.status = report.getStatus()!= null
                ? report.getStatus().name()
                : null;

        this.developerName = report.getDeveloper()!=null
                ? report.getDeveloper().getUsername()
                : null;
    }
}
