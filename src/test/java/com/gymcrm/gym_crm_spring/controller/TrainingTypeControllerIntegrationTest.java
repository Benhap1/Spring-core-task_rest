package com.gymcrm.gym_crm_spring.controller;

import com.gymcrm.gym_crm_spring.dao.TrainingTypeDao;
import com.gymcrm.gym_crm_spring.dao.UserDao;
import com.gymcrm.gym_crm_spring.domain.TrainingType;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TrainingTypeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        userDao.findAll().forEach(user -> userDao.delete(user.getId()));
        trainingTypeDao.findAll().forEach(tt -> trainingTypeDao.delete(tt.getId()));

        var regRequest = new TraineeRegistrationRequest("John", "Doe", Optional.empty(), Optional.empty());
        MvcResult regResult = mockMvc.perform(post("/api/auth/register/trainee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var regResponse = objectMapper.readValue(regResult.getResponse().getContentAsString(), TraineeRegistrationResponse.class);
        this.username = regResponse.username();
        String userPassword = regResponse.password();

        MvcResult loginResult = mockMvc.perform(get("/api/auth/login")
                        .param("username", username)
                        .param("password", userPassword))
                .andExpect(status().isOk())
                .andReturn();
        this.token = loginResult.getResponse().getContentAsString().trim();

        TrainingType tt = new TrainingType();
        tt.setTrainingTypeName("Strength");
        trainingTypeDao.save(tt);
    }

    @Test
    @DisplayName("GET /api/training-types — get all training types successfully")
    void getAllTrainingTypes_Success() throws Exception {
        mockMvc.perform(get("/api/training-types")
                        .header("X-Auth-Token", token)
                        .param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trainingTypeName").value("Strength"));

        var types = trainingTypeDao.findAll();
        assertThat(types).hasSize(1);
        assertThat(types.get(0).getTrainingTypeName()).isEqualTo("Strength");
    }

    @Test
    @DisplayName("GET /api/training-types — get empty training types list successfully")
    void getAllTrainingTypes_Empty_Success() throws Exception {
        trainingTypeDao.findAll().forEach(tt -> trainingTypeDao.delete(tt.getId()));

        mockMvc.perform(get("/api/training-types")
                        .header("X-Auth-Token", token)
                        .param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        var types = trainingTypeDao.findAll();
        assertThat(types).isEmpty();
    }

    @Test
    @DisplayName("GET /api/training-types — fail on invalid token")
    void getAllTrainingTypes_InvalidToken_Fails() throws Exception {
        mockMvc.perform(get("/api/training-types")
                        .header("X-Auth-Token", "invalid-token")
                        .param("username", username))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"));

        var types = trainingTypeDao.findAll();
        assertThat(types).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/training-types — fail on missing token")
    void getAllTrainingTypes_MissingToken_Fails() throws Exception {
        mockMvc.perform(get("/api/training-types")
                        .param("username", username))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing authentication token in header: X-Auth-Token"));
    }
}