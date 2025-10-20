package com.gymcrm.gym_crm_spring.facade;

import com.gymcrm.gym_crm_spring.domain.Trainee;
import com.gymcrm.gym_crm_spring.domain.Trainer;
import com.gymcrm.gym_crm_spring.domain.Training;
import com.gymcrm.gym_crm_spring.domain.TrainingType;
import com.gymcrm.gym_crm_spring.domain.User;
import com.gymcrm.gym_crm_spring.dto.ChangePasswordRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeActivationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeProfileResponse;
import com.gymcrm.gym_crm_spring.dto.TraineeProfileUpdateRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeProfileUpdateResponse;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationResponse;
import com.gymcrm.gym_crm_spring.dto.TraineeTrainerListUpdateRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeTrainerListUpdateResponse;
import com.gymcrm.gym_crm_spring.dto.TraineeTrainingResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerActivationRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerProfileResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerProfileUpdateRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerProfileUpdateResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerShortResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerTrainingsListResponse;
import com.gymcrm.gym_crm_spring.dto.TrainingCreateRequest;
import com.gymcrm.gym_crm_spring.dto.TrainingTypeResponse;
import com.gymcrm.gym_crm_spring.exception.InvalidCredentialsException;
import com.gymcrm.gym_crm_spring.exception.TraineeNotFoundException;
import com.gymcrm.gym_crm_spring.exception.TrainerNotFoundException;
import com.gymcrm.gym_crm_spring.exception.TrainingTypeNotFoundException;
import com.gymcrm.gym_crm_spring.exception.UserAlreadyExistsException;
import com.gymcrm.gym_crm_spring.service.TraineeService;
import com.gymcrm.gym_crm_spring.service.TrainerService;
import com.gymcrm.gym_crm_spring.service.TrainingService;
import com.gymcrm.gym_crm_spring.service.TrainingTypeService;
import com.gymcrm.gym_crm_spring.service.UserService;
import com.gymcrm.gym_crm_spring.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Component
@RequiredArgsConstructor
public class GymFacade {

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final UserService userService;
    private final TrainingTypeService trainingTypeService;
    private final BCryptPasswordEncoder encoder;
    private final TrainingService trainingService;

    @Transactional
    public TraineeRegistrationResponse registerTrainee(TraineeRegistrationRequest request) {
        checkIfUserAlreadyExists(request.firstName(), request.lastName());

        var userWithPassword = createAndPrepareUser(request.firstName(), request.lastName());
        var user = userWithPassword.user();
        var rawPassword = userWithPassword.rawPassword();

        Trainee trainee = Trainee.builder()
                .user(user)
                .dateOfBirth(request.dateOfBirth().orElse(null))
                .address(request.address().orElse(null))
                .build();

        traineeService.save(trainee);

        return new TraineeRegistrationResponse(user.getUsername(), rawPassword);
    }

    @Transactional
    public TrainerRegistrationResponse registerTrainer(TrainerRegistrationRequest request) {
        checkIfUserAlreadyExists(request.firstName(), request.lastName());

        TrainingType specialization = trainingTypeService.findByName(request.specializationName())
                .orElseThrow(() -> new TrainingTypeNotFoundException(request.specializationName()));

        var userWithPassword = createAndPrepareUser(request.firstName(), request.lastName());
        var user = userWithPassword.user();
        var rawPassword = userWithPassword.rawPassword();

        Trainer trainer = Trainer.builder()
                .user(user)
                .specialization(specialization)
                .build();

        trainerService.save(trainer);

        return new TrainerRegistrationResponse(user.getUsername(), rawPassword);
    }


    private void checkIfUserAlreadyExists(String firstName, String lastName) {
        boolean traineeExists = traineeService.existsByFirstAndLastName(firstName, lastName);
        boolean trainerExists = trainerService.existsByFirstAndLastName(firstName, lastName);
        if (traineeExists || trainerExists) {
            throw new UserAlreadyExistsException("User is already registered as Trainee or Trainer");
        }
    }


    private record UserWithPassword(User user, String rawPassword) {}


    private UserWithPassword createAndPrepareUser(String firstName, String lastName) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .active(true)
                .build();

        String username = UserUtils.generateUsername(firstName, lastName, userService.findAll());
        String rawPassword = UserUtils.generatePassword();
        String encodedPassword = encoder.encode(rawPassword);

        user.setUsername(username);
        user.setPassword(encodedPassword);

        return new UserWithPassword(user, rawPassword);
    }


    @Transactional(readOnly = true)
    public void login(String username, String password) {
        var user = userService.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!encoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public void changeLogin(ChangePasswordRequest request) {
        userService.changePassword(
                request.username(),
                request.oldPassword(),
                request.newPassword()
        );
    }

    @Transactional(readOnly = true)
    public TraineeProfileResponse getTraineeProfile(String username) {
        return traineeService.getProfile(username);
    }

    @Transactional
    public TraineeProfileUpdateResponse updateTraineeProfile(TraineeProfileUpdateRequest request) {
        return traineeService.updateProfile(request);
    }

    @Transactional
    public void deleteTraineeProfile(String username) {
        traineeService.deleteByUsername(username);
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse getTrainerProfile(String username) {
        return trainerService.getProfile(username);
    }

    @Transactional
    public TrainerProfileUpdateResponse updateTrainerProfile(TrainerProfileUpdateRequest request) {
        return trainerService.updateProfile(request);
    }
    @Transactional(readOnly = true)
    public List<TrainerShortResponse> getNotAssignedActiveTrainers(String traineeUsername) {
        return trainerService.getNotAssignedActiveTrainers(traineeUsername);
    }

    @Transactional
    public TraineeTrainerListUpdateResponse updateTraineeTrainerList(TraineeTrainerListUpdateRequest request) {
        return traineeService.updateTrainerList(request);
    }

    @Transactional(readOnly = true)
    public List<TraineeTrainingResponse> getTraineeTrainings(
            String username,
            LocalDate from,
            LocalDate to,
            String trainerName,
            String trainingType
    ) {
        return traineeService.getTraineeTrainings(username, from, to, trainerName, trainingType);
    }

    @Transactional(readOnly = true)
    public TrainerTrainingsListResponse getTrainerTrainingsList(
            String username,
            LocalDate from,
            LocalDate to,
            String traineeName
    ) {
        var trainer = trainerService.findByUsername(username)
                .orElseThrow(() -> new TrainerNotFoundException(username));

        var trainings = trainingService.findByCriteriaForTrainer(username, from, to, traineeName)
                .stream()
                .map(training -> new TrainerTrainingsListResponse.TrainerTrainingResponse(
                        training.getTrainingName(),
                        training.getTrainingDate(),
                        training.getTrainingType().getTrainingTypeName(),
                        training.getTrainingDuration(),
                        training.getTrainee().getUser().getUsername()
                ))
                .toList();

        return new TrainerTrainingsListResponse(trainings);
    }

    @Transactional
    public void addTraining(TrainingCreateRequest request) {
        var trainee = traineeService.findByUsername(request.traineeUsername())
                .orElseThrow(() -> new TraineeNotFoundException(request.traineeUsername()));

        var trainer = trainerService.findByUsername(request.trainerUsername())
                .orElseThrow(() -> new TrainerNotFoundException(request.trainerUsername()));

        var training = new Training();
        training.setTrainingName(request.trainingName());
        training.setTrainingDate(request.trainingDate());
        training.setTrainingDuration(request.trainingDuration());
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingType(trainer.getSpecialization());

        trainingService.saveTraining(training);
    }

    @Transactional
    public void activateTrainee(TraineeActivationRequest request) {
        traineeService.updateActivationStatus(request.username(), request.isActive());
    }

    @Transactional
    public void activateTrainer(TrainerActivationRequest request) {
        trainerService.updateActivationStatus(request.username(), request.isActive());
    }

    @Transactional(readOnly = true)
    public List<TrainingTypeResponse> getAllTrainingTypes() {
        return trainingTypeService.findAll().stream()
                .map(tt -> new TrainingTypeResponse(tt.getId(), tt.getTrainingTypeName()))
                .toList();
    }
}
