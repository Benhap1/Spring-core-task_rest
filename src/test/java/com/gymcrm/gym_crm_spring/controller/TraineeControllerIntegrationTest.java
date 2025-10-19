package com.gymcrm.gym_crm_spring.controller;

import com.gymcrm.gym_crm_spring.dao.TraineeDao;
import com.gymcrm.gym_crm_spring.dao.TrainerDao;
import com.gymcrm.gym_crm_spring.dao.TrainingDao;
import com.gymcrm.gym_crm_spring.dao.TrainingTypeDao;
import com.gymcrm.gym_crm_spring.dao.UserDao;
import com.gymcrm.gym_crm_spring.domain.TrainingType;
import com.gymcrm.gym_crm_spring.dto.TraineeActivationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeProfileUpdateRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationResponse;
import com.gymcrm.gym_crm_spring.dto.TraineeTrainerListUpdateRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationResponse;
import com.gymcrm.gym_crm_spring.dto.TrainingCreateRequest;
import com.gymcrm.gym_crm_spring.facade.GymFacade;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TraineeControllerIntegrationTest {

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

    @Autowired
    private GymFacade gymFacade;

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
    @DisplayName("GET /api/trainee/profile — get Trainee profile successfully")
    void getProfile_Success() throws Exception {
        mockMvc.perform(get("/api/trainee/profile")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers").isArray())
                .andExpect(jsonPath("$.trainers.length()").value(0));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
    }

    @Test
    @DisplayName("GET /api/trainee/profile — fail on invalid token")
    void getProfile_InvalidToken_Fails() throws Exception {
        mockMvc.perform(get("/api/trainee/profile")
                        .header("X-Auth-Token", "invalid-token")
                        .param("username", traineeUsername))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"));
    }

    @Test
    @DisplayName("GET /api/trainee/profile — fail on missing token")
    void getProfile_MissingToken_Fails() throws Exception {
        mockMvc.perform(get("/api/trainee/profile")
                        .param("username", traineeUsername))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing authentication token in header: X-Auth-Token"));
    }

    @Test
    @DisplayName("GET /api/trainee/profile — fail on wrong role (trainer token)")
    void getProfile_WrongRole_Fails() throws Exception {
        mockMvc.perform(get("/api/trainee/profile")
                        .header("X-Auth-Token", trainerToken)
                        .param("username", trainerUsername))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trainee with username '" + trainerUsername + "' not found"));
    }

    @Test
    @DisplayName("PUT /api/trainee/profile — update Trainee profile successfully")
    void updateProfile_Success() throws Exception {
        var request = new TraineeProfileUpdateRequest(
                traineeUsername,
                "Johnny",
                "Doe Jr",
                Optional.of(LocalDate.of(1990, 1, 1)),
                Optional.of("Updated Address"),
                true
        );

        mockMvc.perform(put("/api/trainee/profile")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Doe Jr"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"))
                .andExpect(jsonPath("$.address").value("Updated Address"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers.length()").value(0));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
        var trainee = traineeOpt.get();
        var user = trainee.getUser();
        assertThat(user.getFirstName()).isEqualTo("Johnny");
        assertThat(user.getLastName()).isEqualTo("Doe Jr");
        assertThat(user.getActive()).isTrue();
        assertThat(trainee.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(trainee.getAddress()).isEqualTo("Updated Address");
    }

    @Test
    @DisplayName("PUT /api/trainee/profile — fail on invalid token")
    void updateProfile_InvalidToken_Fails() throws Exception {
        var request = new TraineeProfileUpdateRequest(
                traineeUsername,
                "New",
                "Name",
                Optional.empty(),
                Optional.empty(),
                true
        );

        mockMvc.perform(put("/api/trainee/profile")
                        .header("X-Auth-Token", "invalid-token")
                        .param("username", traineeUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
        var user = traineeOpt.get().getUser();
        assertThat(user.getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("DELETE /api/trainee/profile — delete Trainee profile successfully")
    void deleteProfile_Success() throws Exception {
        mockMvc.perform(delete("/api/trainee/profile")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername))
                .andExpect(status().isOk());

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isEmpty();
        var userOpt = userDao.findByUsername(traineeUsername);
        assertThat(userOpt).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/trainee/profile — fail on invalid token")
    void deleteProfile_InvalidToken_Fails() throws Exception {
        mockMvc.perform(delete("/api/trainee/profile")
                        .header("X-Auth-Token", "invalid-token")
                        .param("username", traineeUsername))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
    }

    @Test
    @DisplayName("PUT /api/trainee/trainers — update Trainee trainer list successfully")
    void updateTrainerList_Success() throws Exception {
        var request = new TraineeTrainerListUpdateRequest(
                traineeUsername,
                List.of(trainerUsername)
        );

        mockMvc.perform(put("/api/trainee/trainers")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[0].username").value(trainerUsername))
                .andExpect(jsonPath("$.trainers.length()").value(1));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
        var trainee = traineeOpt.get();
        assertThat(trainee.getAssignedTrainers()).hasSize(1);
        var assignedTrainer = trainee.getAssignedTrainers().iterator().next();
        assertThat(assignedTrainer.getUser().getUsername()).isEqualTo(trainerUsername);
    }

    @Test
    @DisplayName("PUT /api/trainee/trainers — fail on invalid trainer")
    void updateTrainerList_InvalidTrainer_Fails() throws Exception {
        var request = new TraineeTrainerListUpdateRequest(
                traineeUsername,
                List.of("invalid-trainer")
        );

        mockMvc.perform(put("/api/trainee/trainers")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trainer with username 'invalid-trainer' not found"));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
        assertThat(traineeOpt.get().getAssignedTrainers()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/trainee/trainings — get Trainee trainings successfully")
    void getTraineeTrainings_Success() throws Exception {
        var createRequest = new TrainingCreateRequest(
                traineeUsername,
                trainerUsername,
                "Sample Training",
                LocalDate.now(),
                60
        );
        gymFacade.addTraining(createRequest);

        mockMvc.perform(get("/api/trainee/trainings")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Sample Training"))
                .andExpect(jsonPath("$[0].trainingDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$[0].trainerUsername").value(trainerUsername))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        var trainings = trainingDao.findByCriteriaForTrainee(traineeUsername, null, null, null, null);
        assertThat(trainings).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/trainee/trainings — get Trainee trainings with filters")
    void getTraineeTrainings_WithFilters() throws Exception {
        var createRequest = new TrainingCreateRequest(
                traineeUsername,
                trainerUsername,
                "Filtered Training",
                LocalDate.now(),
                60
        );
        gymFacade.addTraining(createRequest);

        mockMvc.perform(get("/api/trainee/trainings")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername)
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .param("trainerName", "Mike")
                        .param("trainingType", "Strength"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Filtered Training"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/trainee/trainings — no trainings found")
    void getTraineeTrainings_NoTrainings() throws Exception {
        mockMvc.perform(get("/api/trainee/trainings")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PATCH /api/trainee/activate — activate Trainee successfully")
    void activateTrainee_Success() throws Exception {
        var request = new TraineeActivationRequest(traineeUsername, false);

        mockMvc.perform(patch("/api/trainee/activate")
                        .header("X-Auth-Token", traineeToken)
                        .param("username", traineeUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
        assertThat(traineeOpt.get().getUser().getActive()).isFalse();
    }

    @Test
    @DisplayName("PATCH /api/trainee/activate — fail on invalid token")
    void activateTrainee_InvalidToken_Fails() throws Exception {
        var request = new TraineeActivationRequest(traineeUsername, false);

        mockMvc.perform(patch("/api/trainee/activate")
                        .header("X-Auth-Token", "invalid-token")
                        .param("username", traineeUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired authentication token"));

        var traineeOpt = traineeDao.findByUsername(traineeUsername);
        assertThat(traineeOpt).isPresent();
        assertThat(traineeOpt.get().getUser().getActive()).isTrue();
    }
}