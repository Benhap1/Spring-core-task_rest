package com.gymcrm.gym_crm_spring.controller;

import com.gymcrm.gym_crm_spring.dto.ChangePasswordRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TraineeRegistrationResponse;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationRequest;
import com.gymcrm.gym_crm_spring.dto.TrainerRegistrationResponse;
import com.gymcrm.gym_crm_spring.facade.GymFacade;
import com.gymcrm.gym_crm_spring.security.TokenStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Registration")
@Validated
public class AuthController {

    private final GymFacade gymFacade;
    private final TokenStore tokenStore;

    @Operation(summary = "Registration of new Trainee", description = "Create Trainee and return username")
    @ApiResponse(responseCode = "201", description = "Trainee registered successfully!")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register/trainee")
    public TraineeRegistrationResponse register(@Valid @RequestBody TraineeRegistrationRequest request) {
        return gymFacade.registerTrainee(request);

    }

    @Operation(summary = "Registration of new Trainer", description = "Create Trainer and return username")
    @ApiResponse(responseCode = "201", description = "Trainer registered successfully!")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register/trainer")
    public TrainerRegistrationResponse register(@Valid @RequestBody TrainerRegistrationRequest request) {
        return gymFacade.registerTrainer(request);

    }

    @Operation(summary = "User login", description = "Authenticate user by username and password")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/login")
    public String login(
            @RequestParam @NotBlank String username,
            @RequestParam @NotBlank String password
    ) {
        gymFacade.login(username, password);
        return tokenStore.createToken(username);
    }

    @Operation(summary = "Logout (invalidate token)")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/logout")
    public void logout(@RequestHeader("X-Auth-Token") String token) {
        tokenStore.invalidateToken(token);
    }

    @Operation(summary = "Change user password", description = "Change password for existing user")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/change-login")
    public void changeLogin(@Valid @RequestBody ChangePasswordRequest request) {
        gymFacade.changeLogin(request);

    }
}
