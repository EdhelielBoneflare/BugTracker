//package uni.bugtracker.backend.integration;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import uni.bugtracker.backend.repository.ProjectRepository;
//
//import static org.hamcrest.Matchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Testcontainers
//class ProjectManagerFlowIntegrationTest {
//
//    @Container
//    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", postgres::getJdbcUrl);
//        registry.add("spring.datasource.username", postgres::getUsername);
//        registry.add("spring.datasource.password", postgres::getPassword);
//    }
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ProjectRepository projectRepository;
//
//    @Test
//    @WithMockUser(username = "manager", roles = {"PM"})
//    void managerCreatesProjectAndSeesItInList() throws Exception {
//        // Очищаем базу перед тестом (опционально)
//        projectRepository.deleteAll();
//
//        // 1. Создаем новый проект
//        String projectJson = """
//            {
//                "name": "Новый мобильный проект",
//                "description": "Разработка мобильного приложения для iOS и Android"
//            }
//            """;
//
//        String createdProjectId = mockMvc.perform(post("/api/projects")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(projectJson))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").exists())
//                .andExpect(jsonPath("$.name").value("Новый мобильный проект"))
//                .andReturn()
//                .getResponse()
//                .getContentAsString()
//                .split("\"id\":")[1].split(",")[0].trim();
//
//        // 2. Получаем список всех проектов
//        mockMvc.perform(get("/api/projects"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
//                .andExpect(jsonPath("$[*].name", hasItem("Новый мобильный проект")))
//                .andExpect(jsonPath("$[*].id", hasItem(Integer.parseInt(createdProjectId))));
//    }
//
//    @Test
//    @WithMockUser(username = "developer", roles = {"DEVELOPER"})
//    void developerCannotCreateProject() throws Exception {
//        // Проверяем, что разработчик не может создавать проекты
//        String projectJson = """
//            {
//                "name": "Неавторизованный проект"
//            }
//            """;
//
//        mockMvc.perform(post("/api/projects")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(projectJson))
//                .andExpect(status().isForbidden()); // Доступ запрещен
//    }
//}