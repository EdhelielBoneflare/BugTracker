package uni.bugtracker.backend.utility;

import org.springframework.stereotype.Component;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.exception.BusinessValidationException;
import uni.bugtracker.backend.model.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReportMapper {
    private static final int MAX_TAGS = 10;
    private static final int MAX_TITLE = 255;
    private static final int MAX_COMMENTS = 5000;

    public Report fromCreateOnWidget(
            ReportCreationRequestWidget request,
            Project project,
            Session session
    ) {
        Report report = new Report();

        report.setProject(project);
        report.setSession(session);
        report.setTitle(trim(request.getTitle(), MAX_TITLE));

        List<Tag> tags = request.getTags().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> isValidEnum(Tag.class, s))
                .limit(MAX_TAGS)
                .map(Tag::valueOf)
                .toList();
        report.setTags(tags);

        report.setReportedAt(request.getReportedAt());
        report.setComments(trim(request.getComments(), MAX_COMMENTS));
        report.setUserEmail(request.getUserEmail());
        report.setScreen(request.getScreen());
        report.setCurrentUrl(request.getCurrentUrl());
        report.setUserProvided(request.getUserProvided());
        report.setStatus(ReportStatus.NEW);

        return report;
    }

    public Report updateFromDashboard(Report report,
                                      Map<String, Object> raw,
                                      Set<String> fields,
                                      Project project,
                                      Developer developer
    ) {
        if (fields.contains("projectId")) {
            report.setProject(project);
        }

        if (fields.contains("title")) {
            String title = getStringValue(raw, "title");
            report.setTitle(trim(title, MAX_TITLE));
        }

        if (fields.contains("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) raw.get("tags");
            report.setTags(tags == null ? null :
                tags.stream()
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .limit(MAX_TAGS)
                        .filter(this::isValidTag)
                        .map(Tag::valueOf)
                        .toList());
        }

        if (fields.contains("reportedAt")) {
            Instant reportedAt = getInstantValue(raw, "reportedAt");
            if (reportedAt == null)
                throw new BusinessValidationException("INVALID_ARGUMENT", "reportedAt cannot be null");
            report.setReportedAt(reportedAt);
        }

        if (fields.contains("comments")) {
            String comments = getStringValue(raw, "comments");
            report.setComments(trim(comments, MAX_COMMENTS));
        }

        if (fields.contains("developerName")) {
            report.setDeveloper(developer);
        }

        if (fields.contains("level")) {
            CriticalityLevel level = getEnumValue(raw, "level", CriticalityLevel.class);
            report.setCriticality(level);
        }

        if (fields.contains("status")) {
            ReportStatus status = getEnumValue(raw, "status", ReportStatus.class);
            if (status == null)
                throw new BusinessValidationException("INVALID_ARGUMENT", "status cannot be null");
            report.setStatus(status);
        }

        if (fields.contains("userProvided")) {
            Boolean userProvided = getBooleanValue(raw, "userProvided");
            if (userProvided == null)
                throw new BusinessValidationException("INVALID_ARGUMENT", "userProvided cannot be null");
            report.setUserProvided(userProvided);
        }
        return report;
    }

    public void attachEvents(Report report, List<Event> events) {
        report.setRelatedEventIds(
                events.stream()
                        .map(Event::getId)
                        .toList()
        );
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isValidTag(String value) {
        try {
            Tag.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private Instant getInstantValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            try {
                return Instant.parse((String) value);
            } catch (DateTimeParseException e) {
                throw new BusinessValidationException("INVALID_ARGUMENT", "Invalid reportedAt format");
            }
        }
        return null;
    }

    private <E extends Enum<E>> E getEnumValue(Map<String, Object> map, String key, Class<E> enumClass) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("INVALID_ARGUMENT", "Invalid " + key + " value");
        }
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return null;
    }
}
