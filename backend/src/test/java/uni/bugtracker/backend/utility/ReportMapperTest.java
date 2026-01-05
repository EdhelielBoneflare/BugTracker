package uni.bugtracker.backend.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.dto.report.ReportUpdateRequestDashboard;
import uni.bugtracker.backend.exception.BusinessValidationException;
import uni.bugtracker.backend.model.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportMapperTest {

    private final ReportMapper reportMapper = new ReportMapper();

    @Test
    void fromCreateOnWidget_shouldCreateReportWithTrimmedFields() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("A".repeat(300)); // > 255
        request.setTags(List.of("INTERFACE_ISSUE", "MOBILE_VIEW"));
        request.setReportedAt(Instant.now());
        request.setComments("C".repeat(6000)); // > 5000
        request.setUserEmail("test@example.com");
        request.setScreen("screenshot_data");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertEquals(project, report.getProject());
        assertEquals(session, report.getSession());
        assertEquals(255, report.getTitle().length());
        assertEquals(2, report.getTags().size());
        assertEquals(Instant.class, report.getReportedAt().getClass());
        assertEquals(5000, report.getComments().length());
        assertEquals("test@example.com", report.getUserEmail());
        assertEquals("screenshot_data", report.getScreen());
        assertEquals("https://example.com", report.getCurrentUrl());
        assertTrue(report.isUserProvided());
        assertEquals(ReportStatus.NEW, report.getStatus());
    }

    @Test
    void fromCreateOnWidget_withFalseUserProvided_shouldSetFalse() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(List.of("INTERFACE_ISSUE"));
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(false);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertFalse(report.isUserProvided());
    }

    @Test
    void fromCreateOnWidget_shouldIgnoreUnknownTags() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(List.of("INTERFACE_ISSUE", "INVALID_TAG", "MOBILE_VIEW", "ANOTHER_INVALID"));
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertEquals(2, report.getTags().size());
        assertTrue(report.getTags().contains(Tag.INTERFACE_ISSUE));
        assertTrue(report.getTags().contains(Tag.MOBILE_VIEW));
    }

    @Test
    void fromCreateOnWidget_shouldLimitTagsToMax() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");

        List<String> tags = List.of(
                "INTERFACE_ISSUE", "MOBILE_VIEW", "BROKEN_LINK", "SLOW_LOADING",
                "BLANK_SCREEN", "FUNCTIONALITY_PROBLEMS", "BROKEN_IMAGE",
                "SEARCH_PROBLEM", "FORM_NOT_WORKING", "REDIRECT_LOOP",
                "LOGIN_ISSUE", "REGISTER_ISSUE" // 12 тегов
        );
        request.setTags(tags);
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertEquals(10, report.getTags().size());
    }

    @Test
    void fromCreateOnWidget_shouldHandleNullComments() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(List.of("INTERFACE_ISSUE"));
        request.setReportedAt(Instant.now());
        request.setComments(null);
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertNull(report.getComments());
    }

    @Test
    void updateFromDashboard_shouldUpdateOnlySpecifiedFields() {
        // Given
        Report existingReport = new Report();
        existingReport.setTitle("Old Title");
        existingReport.setComments("Old Comments");
        existingReport.setCriticality(CriticalityLevel.LOW);
        existingReport.setStatus(ReportStatus.IN_PROGRESS);
        existingReport.setUserProvided(true);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTitle("New Title");
        request.setComments("New Comments");
        request.setLevel(CriticalityLevel.HIGH);
        request.setStatus(ReportStatus.DONE);
        request.setUserProvided(false);

        Project project = new Project();
        project.setId(1L);
        Developer developer = new Developer();
        developer.setId(1L);
        developer.setUsername("dev1");

        // When: Only update title and comments
        Set<String> fields = Set.of("title", "comments");
        Report updatedReport = reportMapper.updateFromDashboard(
                existingReport, request, fields, project, developer);

        // Then
        assertEquals("New Title", updatedReport.getTitle());
        assertEquals("New Comments", updatedReport.getComments());
        assertEquals(CriticalityLevel.LOW, updatedReport.getCriticality());
        assertEquals(ReportStatus.IN_PROGRESS, updatedReport.getStatus());
        assertTrue(updatedReport.isUserProvided());
        assertNull(updatedReport.getDeveloper());
        assertNull(updatedReport.getProject());
    }

    @Test
    void updateFromDashboard_shouldUpdateAllFields() {
        // Given
        Report existingReport = new Report();
        existingReport.setTitle("Old Title");
        existingReport.setUserProvided(false);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTitle("New Title");
        request.setTags(List.of("INTERFACE_ISSUE", "MOBILE_VIEW"));
        request.setReportedAt(Instant.now());
        request.setComments("New Comments");
        request.setLevel(CriticalityLevel.MEDIUM);
        request.setStatus(ReportStatus.DONE);
        request.setUserProvided(true);

        Project project = new Project();
        project.setId(1L);
        Developer developer = new Developer();
        developer.setId(1L);
        developer.setUsername("dev1");

        // When: Update all fields
        Set<String> fields = Set.of("title", "tags", "reportedAt", "comments",
                "developerName", "level", "status", "userProvided", "projectId");
        Report updatedReport = reportMapper.updateFromDashboard(
                existingReport, request, fields, project, developer);

        // Then
        assertEquals("New Title", updatedReport.getTitle());
        assertEquals(2, updatedReport.getTags().size());
        assertEquals(request.getReportedAt(), updatedReport.getReportedAt());
        assertEquals("New Comments", updatedReport.getComments());
        assertEquals(developer, updatedReport.getDeveloper());
        assertEquals(CriticalityLevel.MEDIUM, updatedReport.getCriticality());
        assertEquals(ReportStatus.DONE, updatedReport.getStatus());
        assertTrue(updatedReport.isUserProvided());
        assertEquals(project, updatedReport.getProject());
    }

    @Test
    void updateFromDashboard_withNullReportedAt_shouldThrowException() {
        // Given
        Report existingReport = new Report();
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setReportedAt(null);

        // When & Then
        assertThrows(BusinessValidationException.class, () ->
                reportMapper.updateFromDashboard(
                        existingReport, request, Set.of("reportedAt"),
                        null, null)
        );
    }

    @Test
    void updateFromDashboard_withNullStatus_shouldThrowException() {
        // Given
        Report existingReport = new Report();
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setStatus(null);

        // When & Then
        assertThrows(BusinessValidationException.class, () ->
                reportMapper.updateFromDashboard(
                        existingReport, request, Set.of("status"),
                        null, null)
        );
    }

    @Test
    void updateFromDashboard_withNullUserProvided_shouldThrowException() {
        // Given
        Report existingReport = new Report();
        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setUserProvided(null);

        // When & Then
        assertThrows(BusinessValidationException.class, () ->
                reportMapper.updateFromDashboard(
                        existingReport, request, Set.of("userProvided"),
                        null, null)
        );
    }

    @Test
    void updateFromDashboard_withNullTags_shouldSetNullTags() {
        // Given
        Report existingReport = new Report();
        existingReport.setTags(List.of(Tag.INTERFACE_ISSUE));

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTags(null);

        // When
        Report updatedReport = reportMapper.updateFromDashboard(
                existingReport, request, Set.of("tags"), null, null);

        // Then
        assertNull(updatedReport.getTags());
    }

    @Test
    void updateFromDashboard_withEmptyTagsList_shouldSetEmptyList() {
        // Given
        Report existingReport = new Report();
        existingReport.setTags(List.of(Tag.INTERFACE_ISSUE));

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTags(List.of());

        // When
        Report updatedReport = reportMapper.updateFromDashboard(
                existingReport, request, Set.of("tags"), null, null);

        // Then
        assertNotNull(updatedReport.getTags());
        assertTrue(updatedReport.getTags().isEmpty());
    }

    @Test
    void updateFromDashboard_withInvalidTags_shouldFilterThemOut() {
        // Given
        Report existingReport = new Report();
        existingReport.setTags(List.of(Tag.INTERFACE_ISSUE));

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setTags(List.of("INTERFACE_ISSUE", "INVALID_TAG", "MOBILE_VIEW"));

        // When
        Report updatedReport = reportMapper.updateFromDashboard(
                existingReport, request, Set.of("tags"), null, null);

        // Then
        assertNotNull(updatedReport.getTags());
        assertEquals(2, updatedReport.getTags().size());
        assertTrue(updatedReport.getTags().contains(Tag.INTERFACE_ISSUE));
        assertTrue(updatedReport.getTags().contains(Tag.MOBILE_VIEW));
    }

    @Test
    void updateFromDashboard_withDeveloperNameFieldButNullDeveloper_shouldSetNullDeveloper() {
        // Given
        Report existingReport = new Report();
        Developer existingDev = new Developer();
        existingDev.setUsername("oldDev");
        existingReport.setDeveloper(existingDev);

        ReportUpdateRequestDashboard request = new ReportUpdateRequestDashboard();
        request.setDeveloperName(null);

        // When
        Report updatedReport = reportMapper.updateFromDashboard(
                existingReport, request, Set.of("developerName"), null, null);

        // Then
        assertNull(updatedReport.getDeveloper());
    }

    @Test
    void attachEvents_shouldSetRelatedEventIds() {
        // Given
        Report report = new Report();
        Event event1 = new Event();
        event1.setId(1L);
        Event event2 = new Event();
        event2.setId(2L);
        List<Event> events = List.of(event1, event2);

        // When
        reportMapper.attachEvents(report, events);

        // Then
        assertNotNull(report.getRelatedEventIds());
        assertEquals(2, report.getRelatedEventIds().size());
        assertTrue(report.getRelatedEventIds().contains(1L));
        assertTrue(report.getRelatedEventIds().contains(2L));
    }

    @Test
    void attachEvents_withEmptyList_shouldSetEmptyEventIds() {
        // Given
        Report report = new Report();
        report.setRelatedEventIds(List.of(1L, 2L));
        List<Event> events = List.of();

        // When
        reportMapper.attachEvents(report, events);

        // Then
        assertNotNull(report.getRelatedEventIds());
        assertTrue(report.getRelatedEventIds().isEmpty());
    }

    @Test
    void testTrimMethod_shouldReturnNullForNullInput() throws Exception {
        Method trimMethod = ReportMapper.class.getDeclaredMethod("trim", String.class, int.class);
        trimMethod.setAccessible(true);

        String result = (String) trimMethod.invoke(reportMapper, null, 10);
        assertNull(result);
    }

    @Test
    void testTrimMethod_shouldReturnOriginalIfWithinLimit() throws Exception {
        Method trimMethod = ReportMapper.class.getDeclaredMethod("trim", String.class, int.class);
        trimMethod.setAccessible(true);

        String input = "Hello";
        String result = (String) trimMethod.invoke(reportMapper, input, 10);
        assertEquals("Hello", result);
    }

    @Test
    void testTrimMethod_shouldTrimIfExceedsLimit() throws Exception {
        Method trimMethod = ReportMapper.class.getDeclaredMethod("trim", String.class, int.class);
        trimMethod.setAccessible(true);

        String input = "Hello World";
        String result = (String) trimMethod.invoke(reportMapper, input, 5);
        assertEquals("Hello", result);
    }

    @Test
    void testTrimMethod_withExactLength_shouldReturnOriginal() throws Exception {
        Method trimMethod = ReportMapper.class.getDeclaredMethod("trim", String.class, int.class);
        trimMethod.setAccessible(true);

        String input = "Hello";
        String result = (String) trimMethod.invoke(reportMapper, input, 5);
        assertEquals("Hello", result);
    }

    @Test
    void testIsValidEnum_shouldReturnTrueForValidEnum() throws Exception {
        Method isValidEnumMethod = ReportMapper.class.getDeclaredMethod("isValidEnum", Class.class, String.class);
        isValidEnumMethod.setAccessible(true);

        boolean result = (boolean) isValidEnumMethod.invoke(reportMapper, Tag.class, "INTERFACE_ISSUE");
        assertTrue(result);
    }

    @Test
    void testIsValidEnum_shouldReturnFalseForInvalidEnum() throws Exception {
        Method isValidEnumMethod = ReportMapper.class.getDeclaredMethod("isValidEnum", Class.class, String.class);
        isValidEnumMethod.setAccessible(true);

        boolean result = (boolean) isValidEnumMethod.invoke(reportMapper, Tag.class, "INVALID_TAG");
        assertFalse(result);
    }

    @Test
    void testIsValidTag_shouldReturnTrueForValidTag() throws Exception {
        Method isValidTagMethod = ReportMapper.class.getDeclaredMethod("isValidTag", String.class);
        isValidTagMethod.setAccessible(true);

        boolean result = (boolean) isValidTagMethod.invoke(reportMapper, "INTERFACE_ISSUE");
        assertTrue(result);
    }

    @Test
    void testIsValidTag_shouldReturnFalseForInvalidTag() throws Exception {
        Method isValidTagMethod = ReportMapper.class.getDeclaredMethod("isValidTag", String.class);
        isValidTagMethod.setAccessible(true);

        boolean result = (boolean) isValidTagMethod.invoke(reportMapper, "INVALID_TAG");
        assertFalse(result);
    }

    @Test
    void testIsValidTag_withEmptyString_shouldReturnFalse() throws Exception {
        Method isValidTagMethod = ReportMapper.class.getDeclaredMethod("isValidTag", String.class);
        isValidTagMethod.setAccessible(true);

        boolean result = (boolean) isValidTagMethod.invoke(reportMapper, "");
        assertFalse(result);
    }

    @Test
    void fromCreateOnWidget_withNullTitle_shouldSetNullTitle() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle(null);
        request.setTags(List.of("INTERFACE_ISSUE"));
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertNull(report.getTitle());
    }

    @Test
    void fromCreateOnWidget_withEmptyTags_shouldSetEmptyTagsList() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(List.of());
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertNotNull(report.getTags());
        assertTrue(report.getTags().isEmpty());
    }

    @Test
    void fromCreateOnWidget_withUpperCaseConversion() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(List.of("interface_issue", "mobile_view"));
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertEquals(2, report.getTags().size());
        assertTrue(report.getTags().contains(Tag.INTERFACE_ISSUE));
        assertTrue(report.getTags().contains(Tag.MOBILE_VIEW));
    }

    @Test
    void fromCreateOnWidget_withTrimmedTags() {
        // Given
        ReportCreationRequestWidget request = new ReportCreationRequestWidget();
        request.setTitle("Test");
        request.setTags(List.of("  INTERFACE_ISSUE  ", "  MOBILE_VIEW  "));
        request.setReportedAt(Instant.now());
        request.setComments("Comments");
        request.setUserEmail("test@example.com");
        request.setScreen("screen");
        request.setCurrentUrl("https://example.com");
        request.setUserProvided(true);

        Project project = new Project();
        Session session = new Session();

        // When
        Report report = reportMapper.fromCreateOnWidget(request, project, session);

        // Then
        assertNotNull(report);
        assertEquals(2, report.getTags().size());
        assertTrue(report.getTags().contains(Tag.INTERFACE_ISSUE));
        assertTrue(report.getTags().contains(Tag.MOBILE_VIEW));
    }
}