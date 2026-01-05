package uni.bugtracker.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTest {

    private Validator validator;
    private Report report;
    private Project project;
    private Session session;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        session = new Session();
        session.setId(1L);
        session.setProject(project);
        session.setStartTime(Instant.now());
        session.setIsActive(true);

        report = new Report();
        report.setProject(project);
        report.setSession(session);
        report.setReportedAt(Instant.now());
        report.setTitle("Test Report Title");
        report.setStatus(ReportStatus.NEW);
    }

    @Test
    void createReport_WithValidData_ShouldBeValid() {
        // Arrange
        report.setComments("Some comments");
        report.setUserEmail("user@example.com");
        report.setCurrentUrl("https://example.com/page");
        report.setUserProvided(true);
        report.setTags(Arrays.asList(Tag.BROKEN_LINK, Tag.SLOW_LOADING));

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "valid@email.com",
            "test.user@company.co.uk",
            "user+tag@domain.org"
    })
    void createReport_WithValidEmail_ShouldBeValid(String email) {
        // Arrange
        report.setUserEmail(email);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("userEmail"))
                .isEmpty();
    }

    @Test
    void createReport_WithInvalidEmail_ShouldBeInvalid() {
        // Arrange
        report.setUserEmail("invalid-email");

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("userEmail"))
                .isNotEmpty();
    }

    @Test
    void createReport_WithTitleExactlyAtLimit_ShouldBeValid() {
        // Arrange
        String titleAtLimit = "A".repeat(255);
        report.setTitle(titleAtLimit);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("title"))
                .isEmpty();
    }

    @Test
    void createReport_WithTitleTooLong_ShouldBeInvalid() {
        // Arrange
        String longTitle = "A".repeat(256);
        report.setTitle(longTitle);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("title"))
                .isNotEmpty();
    }

    @Test
    void createReport_WithCommentsExactlyAtLimit_ShouldBeValid() {
        // Arrange
        String commentsAtLimit = "A".repeat(5000);
        report.setComments(commentsAtLimit);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("comments"))
                .isEmpty();
    }

    @Test
    void createReport_WithCommentsTooLong_ShouldBeInvalid() {
        // Arrange
        String longComments = "A".repeat(5001);
        report.setComments(longComments);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("comments"))
                .isNotEmpty();
    }

    @Test
    void createReport_WithCurrentUrlExactlyAtLimit_ShouldBeValid() {
        // Arrange
        String urlAtLimit = "https://" + "a".repeat(2031) + ".com"; // 2048 символов
        report.setCurrentUrl(urlAtLimit);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("currentUrl"))
                .isEmpty();
    }

    @Test
    void createReport_WithCurrentUrlTooLong_ShouldBeInvalid() {
        // Arrange
        String longUrl = "https://" + "a".repeat(2041) + ".com"; // > 2048 символов
        report.setCurrentUrl(longUrl);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("currentUrl"))
                .isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(ReportStatus.class)
    void createReport_WithAllStatusValues_ShouldBeValid(ReportStatus status) {
        // Arrange
        report.setStatus(status);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("status"))
                .isEmpty();
    }

    @ParameterizedTest
    @EnumSource(CriticalityLevel.class)
    void createReport_WithAllCriticalityLevels_ShouldBeValid(CriticalityLevel criticality) {
        // Arrange
        report.setCriticality(criticality);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("criticality"))
                .isEmpty();
    }

    @Test
    void createReport_WithNullCriticality_ShouldBeValid() {
        // Arrange
        report.setCriticality(null);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("criticality"))
                .isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Tag.class)
    void createReport_WithAllTagValues_ShouldBeValid(Tag tag) {
        // Arrange
        report.setTags(List.of(tag));

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("tags"))
                .isEmpty();
    }

    @Test
    void createReport_WithMultipleTags_ShouldStoreCorrectly() {
        // Arrange
        List<Tag> tags = Arrays.asList(Tag.BROKEN_LINK, Tag.SLOW_LOADING, Tag.MOBILE_VIEW);
        report.setTags(tags);

        // Act & Assert
        assertThat(report.getTags())
                .hasSize(3)
                .containsExactlyInAnyOrder(Tag.BROKEN_LINK, Tag.SLOW_LOADING, Tag.MOBILE_VIEW);
    }

    @Test
    void createReport_WithEmptyTagsList_ShouldBeValid() {
        // Arrange
        report.setTags(Collections.emptyList());

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
        assertThat(report.getTags()).isEmpty();
    }

    @Test
    void createReport_WithNullTags_ShouldBeValid() {
        // Arrange
        report.setTags(null);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
        assertThat(report.getTags()).isNull();
    }

    @Test
    void createReport_WithRelatedEventIds_ShouldStoreIds() {
        // Arrange
        List<Long> eventIds = Arrays.asList(1L, 2L, 3L, 4L);
        report.setRelatedEventIds(eventIds);

        // Act & Assert
        assertThat(report.getRelatedEventIds())
                .hasSize(4)
                .containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void createReport_WithEmptyRelatedEventIds_ShouldBeValid() {
        // Arrange
        report.setRelatedEventIds(Collections.emptyList());

        // Act & Assert
        assertThat(report.getRelatedEventIds()).isEmpty();
    }

    @Test
    void createReport_WithLargeScreen_ShouldBeValid() {
        // Arrange
        String largeScreen = "data:image/png;base64," + "A".repeat(10000);
        report.setScreen(largeScreen);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createReport_WithNullScreen_ShouldBeValid() {
        // Arrange
        report.setScreen(null);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createReport_WithDeveloper_ShouldStoreDeveloper() {
        // Arrange
        Developer dev = new Developer();
        dev.setId(1L);
        dev.setUsername("dev1");
        dev.setPassword("pass");
        report.setDeveloper(dev);

        // Act & Assert
        assertThat(report.getDeveloper())
                .isNotNull()
                .extracting(Developer::getUsername)
                .isEqualTo("dev1");
    }

    @Test
    void createReport_WithNullDeveloper_ShouldBeValid() {
        // Arrange
        report.setDeveloper(null);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createReport_DefaultValues() {
        Report newReport = new Report();

        assertThat(newReport.isUserProvided()).isFalse();
        assertThat(newReport.getStatus()).isEqualTo(ReportStatus.NEW);
    }

    @Test
    void createReport_WithUserProvidedTrue_ShouldBeValid() {
        // Arrange
        report.setUserProvided(true);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void createReport_WithUserProvidedFalse_ShouldBeValid() {
        // Arrange
        report.setUserProvided(false);

        // Act
        var violations = validator.validate(report);

        // Assert
        assertThat(violations).isEmpty();
    }
}