package uni.bugtracker.backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class EnumTests {

    @ParameterizedTest
    @EnumSource(EventType.class)
    void eventType_ShouldHaveValidValues(EventType eventType) {
        // Act & Assert
        assertThat(eventType).isNotNull();
        assertThat(eventType.name()).isNotEmpty();
    }

    @Test
    void eventType_ShouldHaveExpectedValues() {
        // Act & Assert
        assertThat(EventType.values())
                .hasSize(5)
                .containsExactly(
                        EventType.ERROR,
                        EventType.ACTION,
                        EventType.PERFORMANCE,
                        EventType.NETWORK,
                        EventType.CUSTOM
                );
    }

    @ParameterizedTest
    @EnumSource(Tag.class)
    void tag_ShouldHaveValidValues(Tag tag) {
        // Act & Assert
        assertThat(tag).isNotNull();
        assertThat(tag.name()).isNotEmpty();
    }

    @Test
    void tag_ShouldHaveExpectedValues() {
        // Act & Assert
        assertThat(Tag.values())
                .hasSize(15)
                .contains(
                        Tag.NO_SUITABLE_TAG,
                        Tag.BROKEN_LINK,
                        Tag.SLOW_LOADING,
                        Tag.BLANK_SCREEN,
                        Tag.INTERFACE_ISSUE,
                        Tag.FUNCTIONALITY_PROBLEMS,
                        Tag.BROKEN_IMAGE,
                        Tag.SEARCH_PROBLEM,
                        Tag.FORM_NOT_WORKING,
                        Tag.MOBILE_VIEW,
                        Tag.REDIRECT_LOOP,
                        Tag.LOGIN_ISSUE,
                        Tag.REGISTER_ISSUE,
                        Tag.FILTERS_NOT_WORKING,
                        Tag.PAGINATION_ISSUE
                );
    }

    @ParameterizedTest
    @EnumSource(CriticalityLevel.class)
    void criticalityLevel_ShouldHaveValidValues(CriticalityLevel level) {
        // Act & Assert
        assertThat(level).isNotNull();
        assertThat(level.name()).isNotEmpty();
    }

    @Test
    void criticalityLevel_ShouldHaveExpectedValues() {
        // Act & Assert
        assertThat(CriticalityLevel.values())
                .hasSize(4)
                .containsExactly(
                        CriticalityLevel.LOW,
                        CriticalityLevel.MEDIUM,
                        CriticalityLevel.HIGH,
                        CriticalityLevel.CRITICAL
                );
    }

    @ParameterizedTest
    @EnumSource(ReportStatus.class)
    void reportStatus_ShouldHaveValidValues(ReportStatus status) {
        // Act & Assert
        assertThat(status).isNotNull();
        assertThat(status.name()).isNotEmpty();
    }

    @Test
    void reportStatus_ShouldHaveExpectedValues() {
        // Act & Assert
        assertThat(ReportStatus.values())
                .hasSize(3)
                .containsExactly(
                        ReportStatus.NEW,
                        ReportStatus.IN_PROGRESS,
                        ReportStatus.DONE
                );
    }
}