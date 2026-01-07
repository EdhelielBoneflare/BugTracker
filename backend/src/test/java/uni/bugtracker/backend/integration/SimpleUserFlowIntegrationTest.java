package uni.bugtracker.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.security.model.Role;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 1. Register a new user
 * 2. Upgrade user to ADMIN role (simulating admin creation)
 * 3. Login with the admin user
 * 4. Create a project
 * 5. Verify the project exists
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimpleUserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeveloperRepository developerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Clean up before each test
        projectRepository.deleteAll();
        developerRepository.deleteAll();
    }

    @Test
    void completeUserFlow_RegisterLoginCreateProjectAndVerify() throws Exception {
        String username = "admin_user";
        String password = "password123";
        String projectName = "Test Project Name";

        // Step 1: Register a new user
        String registerJson = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, username, password);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // Step 2: Upgrade user to ADMIN role (simulating admin user creation)
        // In a real scenario, this would be done by another admin
        Developer user = developerRepository.findByUsername(username).orElseThrow();
        user.setRole(Role.ADMIN);
        developerRepository.save(user);

        // Step 3: Login with the admin user
        String loginJson = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, username, password);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Step 4: Create a project using the JWT token
        String projectJson = String.format("""
            {
                "projectName": "%s"
            }
            """, projectName);

        MvcResult createProjectResult = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").exists())
                .andExpect(jsonPath("$.message").value("Project created"))
                .andReturn();

        String createProjectResponse = createProjectResult.getResponse().getContentAsString();
        String projectId = objectMapper.readTree(createProjectResponse).get("projectId").asText();

        // Step 5: Verify the project exists by fetching all projects
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(projectId))
                .andExpect(jsonPath("$[0].name").value(projectName));
    }
}

