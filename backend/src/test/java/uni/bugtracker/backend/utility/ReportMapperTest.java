package uni.bugtracker.backend.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.dto.report.ReportUpdateRequestDashboard;
import uni.bugtracker.backend.exception.BusinessValidationException;
import uni.bugtracker.backend.model.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportMapperTest {

    private ReportMapper reportMapper;
    private Project project;
    private Session session;
    private Developer developer;

    @BeforeEach
    void setUp() {
        reportMapper = new ReportMapper();

        project = new Project();
        project.setId("project-123");
        project.setName("Test Project");

        session = new Session();
        session.setId(1L);
        session.setIsActive(true);

        developer = Developer.builder()
                .id("dev-123")
                .username("test.dev")
                .build();
    }

    @Test
    void fromCreateOnWidget_shouldMapAllFieldsCorrectly() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Bug Report Title");
        request.setTags(Arrays.asList("INTERFACE_ISSUE", "MOBILE_VIEW"));
        request.setReportedAt(Instant.now());
        request.setComments("Some comments about the bug");
        request.setUserEmail("user@example.com");
        request.setCurrentUrl("http://example.com/page");
        request.setUserProvided(true);

        byte[] screen = new byte[]{1, 2, 3};

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, screen);

        // Then
        assertThat(report.getProject()).isEqualTo(project);
        assertThat(report.getSession()).isEqualTo(session);
        assertThat(report.getTitle()).isEqualTo("Bug Report Title");
        assertThat(report.getTags()).containsExactly(Tag.INTERFACE_ISSUE, Tag.MOBILE_VIEW);
        assertThat(report.getReportedAt()).isEqualTo(request.getReportedAt());
        assertThat(report.getComments()).isEqualTo("Some comments about the bug");
        assertThat(report.getUserEmail()).isEqualTo("user@example.com");
        assertThat(report.getScreen()).isEqualTo(screen);
        assertThat(report.getCurrentUrl()).isEqualTo("http://example.com/page");
        assertThat(report.isUserProvided()).isTrue();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.NEW);
        assertThat(report.getCriticality()).isEqualTo(CriticalityLevel.UNKNOWN);
    }

    @Test
    void fromCreateOnWidget_shouldIgnoreUnknownTags() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(Arrays.asList("INTERFACE_ISSUE", "INVALID_TAG", "MOBILE_VIEW", "ANOTHER_INVALID"));
        request.setReportedAt(Instant.now());
        request.setCurrentUrl("http://example.com");
        request.setUserProvided(false);

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, null);

        // Then
        assertThat(report.getTags())
                .containsExactly(Tag.INTERFACE_ISSUE, Tag.MOBILE_VIEW)
                .doesNotContain(Tag.NO_SUITABLE_TAG);
    }

    @Test
    void fromCreateOnWidget_shouldTrimExcessTags() {
        // Given
        List<String> manyTags = Arrays.asList(
                "INTERFACE_ISSUE", "MOBILE_VIEW", "SLOW_LOADING", "BLANK_SCREEN",
                "BROKEN_LINK", "FUNCTIONALITY_PROBLEMS", "BROKEN_IMAGE", "SEARCH_PROBLEM",
                "FORM_NOT_WORKING", "REDIRECT_LOOP", "LOGIN_ISSUE", "REGISTER_ISSUE"
        );

        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(manyTags);
        request.setReportedAt(Instant.now());
        request.setCurrentUrl("http://example.com");
        request.setUserProvided(false);

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, null);

        // Then
        assertThat(report.getTags()).hasSize(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "This is exactly 255 characters xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", // 255 символов
            "Short title",
            "A"
    })
    void fromCreateOnWidget_shouldHandleTitleLength(String title) {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle(title);
        request.setReportedAt(Instant.now());
        request.setCurrentUrl("http://example.com");
        request.setUserProvided(false);
        request.setTags(List.of("INTERFACE_ISSUE"));

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, null);

        // Then
        assertThat(report.getTitle()).isEqualTo(title);
    }

    @Test
    void fromCreateOnWidget_shouldTrimTitleWhenExceedsMaxLength() {
        // Given
        String longTitle = "x".repeat(300);

        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle(longTitle);
        request.setReportedAt(Instant.now());
        request.setCurrentUrl("http://example.com");
        request.setUserProvided(false);
        request.setTags(List.of("INTERFACE_ISSUE"));

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, null);

        // Then
        assertThat(report.getTitle()).hasSize(255);
        assertThat(report.getTitle()).isEqualTo("x".repeat(255));
    }

    @Test
    void fromCreateOnWidget_shouldNotTrimTitleWhenExactlyMaxLength() {
        // Given
        String exactTitle = "x".repeat(255);

        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle(exactTitle);
        request.setReportedAt(Instant.now());
        request.setCurrentUrl("http://example.com");
        request.setUserProvided(false);
        request.setTags(List.of("INTERFACE_ISSUE"));

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, null);

        // Then
        assertThat(report.getTitle()).hasSize(255);
        assertThat(report.getTitle()).isEqualTo(exactTitle);
    }

    @Test
    void fromCreateOnWidget_shouldHandleNullScreen() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setReportedAt(Instant.now());
        request.setCurrentUrl("http://example.com");
        request.setUserProvided(false);
        request.setTags(List.of("INTERFACE_ISSUE"));

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session, null);

        // Then
        assertThat(report.getScreen()).isNull();
    }

    @Test
    void updateFromDashboard_shouldUpdateOnlySpecifiedFields() {
        // Given
        Report existingReport = new Report();
        existingReport.setTitle("Old Title");
        existingReport.setComments("Old comments");
        existingReport.setStatus(ReportStatus.NEW);
        existingReport.setCriticality(CriticalityLevel.LOW);
        existingReport.setProject(project);
        existingReport.setDeveloper(developer);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTitle("New Title");
        request.setComments("Updated comments");
        request.setStatus(ReportStatus.IN_PROGRESS);
        request.setLevel(CriticalityLevel.HIGH);

        Set<String> fieldsToUpdate = new HashSet<>(Arrays.asList("title", "comments", "status", "level"));

        // When
        Report updated = reportMapper.updateFromDashboard(
                existingReport, request, fieldsToUpdate, project, developer);

        // Then
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getComments()).isEqualTo("Updated comments");
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        assertThat(updated.getCriticality()).isEqualTo(CriticalityLevel.HIGH);
        assertThat(updated.getProject()).isEqualTo(project);
        assertThat(updated.getDeveloper()).isEqualTo(developer);
    }

    @Test
    void updateFromDashboard_shouldThrowWhenRequiredFieldIsNull() {
        // Given
        Report existingReport = new Report();
        existingReport.setStatus(ReportStatus.NEW);
        existingReport.setProject(project);
        existingReport.setDeveloper(developer);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setStatus(null);

        Set<String> fieldsToUpdate = Set.of("status");

        // When & Then
        assertThatThrownBy(() ->
                reportMapper.updateFromDashboard(existingReport, request, fieldsToUpdate, project, developer))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("status cannot be null");
    }

    @Test
    void updateFromDashboard_shouldHandleTagValidation() {
        // Given
        Report existingReport = new Report();
        existingReport.setTags(List.of(Tag.INTERFACE_ISSUE));
        existingReport.setProject(project);
        existingReport.setDeveloper(developer);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTags(Arrays.asList("invalid_tag", "MOBILE_VIEW", "ANOTHER_INVALID"));

        Set<String> fieldsToUpdate = Set.of("tags");

        // When
        Report updated = reportMapper.updateFromDashboard(
                existingReport, request, fieldsToUpdate, project, developer);

        // Then
        assertThat(updated.getTags())
                .containsExactly(Tag.MOBILE_VIEW)
                .doesNotContain(Tag.INTERFACE_ISSUE);
    }

    @Test
    void updateFromDashboard_shouldHandleNullTags() {
        // Given
        Report existingReport = new Report();
        existingReport.setTags(List.of(Tag.INTERFACE_ISSUE));
        existingReport.setProject(project);
        existingReport.setDeveloper(developer);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTags(null);

        Set<String> fieldsToUpdate = Set.of("tags");

        // When
        Report updated = reportMapper.updateFromDashboard(
                existingReport, request, fieldsToUpdate, project, developer);

        // Then
        assertThat(updated.getTags()).isNull();
    }

    @Test
    void attachEvents_shouldSetRelatedEventIds() {
        // Given
        Report report = new Report();

        Event event1 = new Event();
        event1.setId(100L);

        Event event2 = new Event();
        event2.setId(200L);

        List<Event> events = Arrays.asList(event1, event2);

        // When
        reportMapper.attachEvents(report, events);

        // Then
        assertThat(report.getRelatedEventIds())
                .containsExactly(100L, 200L);
    }

    @Test
    void attachEvents_shouldHandleEmptyEvents() {
        // Given
        Report report = new Report();
        report.setRelatedEventIds(Arrays.asList(1L, 2L, 3L));

        List<Event> emptyEvents = List.of();

        // When
        reportMapper.attachEvents(report, emptyEvents);

        // Then
        assertThat(report.getRelatedEventIds()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void trim_shouldHandleNullOrEmptyStrings(String input) throws Exception {
        // When
        Method trimMethod = ReportMapper.class.getDeclaredMethod("trim", String.class, int.class);
        trimMethod.setAccessible(true);
        String result = (String) trimMethod.invoke(reportMapper, input, 100);

        // Then
        assertThat(result).isEqualTo(input);
    }

    @Test
    void isValidTag_shouldReturnTrueForValidTags() throws Exception {
        // Given
        ReportMapper mapper = new ReportMapper();
        Method method = ReportMapper.class.getDeclaredMethod("isValidTag", String.class);
        method.setAccessible(true);

        // When & Then
        assertThat((Boolean) method.invoke(mapper, "INTERFACE_ISSUE")).isTrue();
    }

    @Test
    void isValidTag_shouldReturnFalseForInvalidTags() throws Exception {
        // Given
        ReportMapper mapper = new ReportMapper();
        Method method = ReportMapper.class.getDeclaredMethod("isValidTag", String.class);
        method.setAccessible(true);

        // When & Then
        assertThat((Boolean) method.invoke(mapper, "INVALID_TAG_NAME")).isFalse();
    }
}