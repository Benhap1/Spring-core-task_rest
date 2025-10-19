package com.gymcrm.gym_crm_spring.controller;

import com.gymcrm.gym_crm_spring.dao.TrainingDao;
import com.gymcrm.gym_crm_spring.dao.TraineeDao;
import com.gymcrm.gym_crm_spring.dao.TrainerDao;
import com.gymcrm.gym_crm_spring.dao.TrainingTypeDao;
import com.gymcrm.gym_crm_spring.dao.UserDao;
import com.gymcrm.gym_crm_spring.domain.TrainingType;
import com.gymcrm.gym_crm_spring.dto.TrainingCreateRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationResponse;
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

import java.time.LocalDate;
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
class TrainingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TraineeDao traineeDao;

    @Autowired
    private TrainerDao trainerDao;

    @Autowired
    private TrainingDao trainingDao;

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    @Autowired
    private ObjectMapper objectMapper;

    private String traineeToken;
    private String trainerToken;
    private String traineeUsername;
    private String trainerUsername;

    @BeforeEach
    void setUp() throws Exception {
        if (trainingTypeDao.findByName("Strength").isEmpty()) {
            TrainingType tt = new TrainingType();
            tt.setTrainingTypeName("Strength");
            trainingTypeDao.save(tt);
        }

        userDao.findAll().forEach(user -> userDao.delete(user.getId()));
        traineeDao.findAll().forEach(trainee -> traineeDao.delete(trainee.getId()));
        trainerDao.findAll().forEach(trainer -> trainerDao.delete(trainer.getId()));
        trainingDao.findAll().forEach(training -> trainingDao.delete(training.getId()));

        var regTraineeRequest = new TraineeRegistrationRequest("John", "Doe", Optional.empty(), Optional.empty());
        MvcResult regTraineeResult = mockMvc.perform(post("/api/auth/register/trainee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regTraineeRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var regTraineeResponse = objectMapper.readValue(regTraineeResult.getResponse().getContentAsString(), TraineeRegistrationResponse.class);
        this.traineeUsername = regTraineeResponse.username();
        String traineePassword = regTraineeResponse.password();

        MvcResult loginTraineeResult = mockMvc.perform(get("/api/auth/login")
                        .param("username", traineeUsername)
                        .param("password", traineePassword))
                .andExpect(status().isOk())
                .andReturn();
        this.traineeToken = loginTraineeResult.getResponse().getContentAsString().trim();

        var regTrainerRequest = new TrainerRegistrationRequest("Mike", "Smith", "Strength");
        MvcResult regTrainerResult = mockMvc.perform(post("/api/auth/register/trainer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regTrainerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var regTrainerResponse = objectMapper.readValue(regTrainerResult.getResponse().getContentAsString(), TrainerRegistrationResponse.class);
        this.trainerUsername = regTrainerResponse.username();
        String trainerPassword = regTrainerResponse.password();

        MvcResult loginTrainerResult = mockMvc.perform(get("/api/auth/login")
                        .param("username", trainerUsername)
                        .param("password", trainerPassword))
                .andExpect(status().isOk())
                .andReturn();
        this.trainerToken = loginTrainerResult.getResponse().getContentAsString().trim();
    }

    @Test
    @DisplayName("POST /api/training/add — add training successfully")
    void addTraining_Success() throws Exception {
        var request = new TrainingCreateRequest(
                traineeUsername,
                trainerUsername,
                "Sample Training",
                LocalDate.now(),
                60
        );

        mockMvc.perform(post("/api/training/add")
                        .header("X-Auth-Token", trainerToken)
                        .param("username", trainerUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var trainings = trainingDao.findByCriteriaForTrainee(traineeUsername, null, null, null, null);
        assertThat(trainings).hasSize(1);
        var training = trainings.get(0);
        assertThat(training.getTrainingName()).isEqualTo("Sample Training");
        assertThat(training.getTrainingDate()).isEqualTo(LocalDate.now());
        assertThat(training.getTrainingDuration()).isEqualTo(60);
        assertThat(training.getTrainee().getUser().getUsername()).isEqualTo(traineeUsername);
        assertThat(training.getTrainer().getUser().getUsername()).isEqualTo(trainerUsername);
        assertThat(training.getTrainingType().getTrainingTypeName()).isEqualTo("Strength");
    }

    @Test
    @DisplayName("POST /api/training/add — fail on invalid credentials")
    void addTraining_InvalidCredentials_Fails() throws Exception {
        var request = new TrainingCreateRequest(
                traineeUsername,
                trainerUsername,
                "Sample Training",
                LocalDate.now(),
                60
        );

        mockMvc.perform(post("/api/training/add")
                        .header("X-Auth-Token", "invalid-token")
                        .param("username", trainerUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"));

        var trainings = trainingDao.findByCriteriaForTrainee(traineeUsername, null, null, null, null);
        assertThat(trainings).isEmpty();
    }

    @Test
    @DisplayName("POST /api/training/add — fail on invalid trainee")
    void addTraining_InvalidTrainee_Fails() throws Exception {
        var request = new TrainingCreateRequest(
                "invalid-trainee",
                trainerUsername,
                "Sample Training",
                LocalDate.now(),
                60
        );

        mockMvc.perform(post("/api/training/add")
                        .header("X-Auth-Token", trainerToken)
                        .param("username", trainerUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trainee with username 'invalid-trainee' not found"));

        var trainings = trainingDao.findByCriteriaForTrainer(trainerUsername, null, null, null);
        assertThat(trainings).isEmpty();
    }

    @Test
    @DisplayName("POST /api/training/add — fail on invalid trainer")
    void addTraining_InvalidTrainer_Fails() throws Exception {
        var request = new TrainingCreateRequest(
                traineeUsername,
                "invalid-trainer",
                "Sample Training",
                LocalDate.now(),
                60
        );

        mockMvc.perform(post("/api/training/add")
                        .header("X-Auth-Token", trainerToken)
                        .param("username", trainerUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trainer with username 'invalid-trainer' not found"));

        var trainings = trainingDao.findByCriteriaForTrainee(traineeUsername, null, null, null, null);
        assertThat(trainings).isEmpty();
    }

    @Test
    @DisplayName("POST /api/training/add — fail on validation error (empty name)")
    void addTraining_ValidationError_Fails() throws Exception {
        var request = new TrainingCreateRequest(
                traineeUsername,
                trainerUsername,
                "",
                LocalDate.now(),
                60
        );

        mockMvc.perform(post("/api/training/add")
                        .header("X-Auth-Token", trainerToken)
                        .param("username", trainerUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed: [trainingName: Training name is required]"));

        var trainings = trainingDao.findByCriteriaForTrainee(traineeUsername, null, null, null, null);
        assertThat(trainings).isEmpty();
    }
}