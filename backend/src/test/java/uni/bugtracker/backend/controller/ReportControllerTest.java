package uni.bugtracker.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uni.bugtracker.backend.dto.report.*;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.CriticalityLevel;
import uni.bugtracker.backend.model.ReportStatus;
import uni.bugtracker.backend.service.ReportService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureJsonTesters
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<ReportCreationRequestWidget> reportCreationWidgetJson;

    @Autowired
    private JacksonTester<ReportUpdateRequestDashboard> reportUpdateDashboardJson;

    @MockitoBean
    private ReportService reportService;

    private ReportCreationRequestWidget createWidgetRequest;
    private ReportUpdateRequestDashboard updateDashboardRequest;
    private ReportCardDTO mockReportCardDTO;
    private ReportDashboardDTO mockReportDashboardDTO;

    @BeforeEach
    void setUp() {
        createWidgetRequest = new ReportCreationRequestWidget();
        createWidgetRequest.setProjectId(1L);
        createWidgetRequest.setSessionId(1L);
        createWidgetRequest.setTitle("Test Report");
        createWidgetRequest.setTags(List.of("UI", "Bug"));
        createWidgetRequest.setReportedAt(Instant.now());
        createWidgetRequest.setComments("Test comments");
        createWidgetRequest.setUserEmail("test@example.com");
        createWidgetRequest.setScreen("screenshot.png");
        createWidgetRequest.setCurrentUrl("http://example.com");
        createWidgetRequest.setUserProvided(true);

        updateDashboardRequest = new ReportUpdateRequestDashboard();
        updateDashboardRequest.setTitle("Updated Title");
        updateDashboardRequest.setTags(List.of("Critical"));
        updateDashboardRequest.setLevel(CriticalityLevel.HIGH);
        updateDashboardRequest.setStatus(ReportStatus.IN_PROGRESS);
        updateDashboardRequest.setDeveloperName("developer1");

        mockReportCardDTO = mock(ReportCardDTO.class);
        when(mockReportCardDTO.getId()).thenReturn(1L);
        when(mockReportCardDTO.getProjectId()).thenReturn(1L);
        when(mockReportCardDTO.getSessionId()).thenReturn(1L);
        when(mockReportCardDTO.getTitle()).thenReturn("Test Report");
        when(mockReportCardDTO.getTags()).thenReturn(List.of("UI", "Bug"));
        when(mockReportCardDTO.getReportedAt()).thenReturn(Instant.now());
        when(mockReportCardDTO.getComments()).thenReturn("Test comments");
        when(mockReportCardDTO.getUserEmail()).thenReturn("test@example.com");
        when(mockReportCardDTO.getScreen()).thenReturn("screenshot.png");
        when(mockReportCardDTO.getCurrentUrl()).thenReturn("http://example.com");
        when(mockReportCardDTO.getUserProvided()).thenReturn(true);
        when(mockReportCardDTO.getLevel()).thenReturn("MEDIUM");
        when(mockReportCardDTO.getStatus()).thenReturn("NEW");
        when(mockReportCardDTO.getDeveloperName()).thenReturn("developer1");

        mockReportDashboardDTO = mock(ReportDashboardDTO.class);
        when(mockReportDashboardDTO.getId()).thenReturn(1L);
        when(mockReportDashboardDTO.getProjectId()).thenReturn(1L);
        when(mockReportDashboardDTO.getTitle()).thenReturn("Test Report");
        when(mockReportDashboardDTO.getTags()).thenReturn(List.of("UI", "Bug"));
        when(mockReportDashboardDTO.getReportedAt()).thenReturn(Instant.now());
        when(mockReportDashboardDTO.getLevel()).thenReturn("MEDIUM");
        when(mockReportDashboardDTO.getStatus()).thenReturn("NEW");
    }

    @Test
    void createReportWidget_ShouldReturnCreated() throws Exception {
        // Arrange
        Long expectedReportId = 1L;
        when(reportService.createReport(any(ReportCreationRequestWidget.class)))
                .thenReturn(expectedReportId);

        // Act & Assert
        mockMvc.perform(post("/api/reports/widget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportCreationWidgetJson.write(createWidgetRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Report created"))
                .andExpect(jsonPath("$.reportId").value(expectedReportId));
    }

    @Test
    void createReportWidget_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        createWidgetRequest.setTitle("");

        // Act & Assert
        mockMvc.perform(post("/api/reports/widget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportCreationWidgetJson.write(createWidgetRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReportDashboard_ShouldReturnOk() throws Exception {
        // Arrange
        Long reportId = 1L;

        when(reportService.updateReportFromDashboard(
                eq(reportId),
                any(ReportUpdateRequestDashboard.class),
                anyString()))
                .thenReturn(mockReportCardDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/reports/{id}/dashboard", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportUpdateDashboardJson.write(updateDashboardRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId))
                .andExpect(jsonPath("$.title").value("Test Report"));
    }

    @Test
    void getAllReportsByProject_ShouldReturnOk() throws Exception {
        // Arrange
        Long projectId = 1L;
        Page<ReportDashboardDTO> page = new PageImpl<>(
                List.of(mockReportDashboardDTO),
                PageRequest.of(0, 30),
                1
        );

        when(reportService.getAllReportsOfProject(eq(projectId), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/reports/byProject/{projectId}", projectId)
                        .param("page", "0")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].projectId").value(projectId));
    }

    @Test
    void getAllReportsByProject_WhenNoReports_ShouldReturnNoContent() throws Exception {
        // Arrange
        Long projectId = 1L;
        Page<ReportDashboardDTO> emptyPage = Page.empty();

        when(reportService.getAllReportsOfProject(eq(projectId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/reports/byProject/{projectId}", projectId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getReportCard_ShouldReturnOk() throws Exception {
        // Arrange
        Long reportId = 1L;
        when(reportService.getReportCard(reportId))
                .thenReturn(mockReportCardDTO);

        // Act & Assert
        mockMvc.perform(get("/api/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId))
                .andExpect(jsonPath("$.title").value("Test Report"));
    }

    @Test
    void getAllSolvedReports_ShouldReturnOk() throws Exception {
        // Arrange
        Long projectId = 1L;
        ReportDashboardDTO mockSolvedReportDashboardDTO = mock(ReportDashboardDTO.class);
        when(mockSolvedReportDashboardDTO.getId()).thenReturn(1L);
        when(mockSolvedReportDashboardDTO.getProjectId()).thenReturn(projectId);
        when(mockSolvedReportDashboardDTO.getTitle()).thenReturn("Solved Report");
        when(mockSolvedReportDashboardDTO.getTags()).thenReturn(List.of("Critical"));
        when(mockSolvedReportDashboardDTO.getReportedAt()).thenReturn(Instant.now());
        when(mockSolvedReportDashboardDTO.getLevel()).thenReturn("HIGH");
        when(mockSolvedReportDashboardDTO.getStatus()).thenReturn("DONE");

        Page<ReportDashboardDTO> page = new PageImpl<>(
                List.of(mockSolvedReportDashboardDTO),
                PageRequest.of(0, 30),
                1
        );

        when(reportService.getAllReportsSolvedOnProject(eq(projectId), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/reports/byProject/{projectId}/solved", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DONE"));
    }

    @Test
    void deleteReport_ShouldReturnOk() throws Exception {
        // Arrange
        Long reportId = 1L;
        when(reportService.deleteReport(reportId))
                .thenReturn(mockReportCardDTO);

        // Act & Assert
        mockMvc.perform(delete("/api/reports/delete/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId));
    }

    @Test
    void updateReportDashboard_WithNullFields_ShouldReturnOk() throws Exception {
        // Arrange
        Long reportId = 1L;
        updateDashboardRequest.setTitle(null);
        updateDashboardRequest.setTags(null);

        when(reportService.updateReportFromDashboard(
                eq(reportId),
                any(ReportUpdateRequestDashboard.class),
                anyString()))
                .thenReturn(mockReportCardDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/reports/{id}/dashboard", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportUpdateDashboardJson.write(updateDashboardRequest).getJson()))
                .andExpect(status().isOk());
    }

    @Test
    void updateReportDashboard_WithEmptyTagsList_ShouldReturnOk() throws Exception {
        // Arrange
        Long reportId = 1L;
        updateDashboardRequest.setTags(List.of());

        when(reportService.updateReportFromDashboard(
                eq(reportId),
                any(ReportUpdateRequestDashboard.class),
                anyString()))
                .thenReturn(mockReportCardDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/reports/{id}/dashboard", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportUpdateDashboardJson.write(updateDashboardRequest).getJson()))
                .andExpect(status().isOk());
    }

    @Test
    void getReportCard_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(reportService.getReportCard(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Report doesn't exist"));

        // Act & Assert
        mockMvc.perform(get("/api/reports/{reportId}", nonExistentId))
                .andExpect(status().isNotFound());
    }
}