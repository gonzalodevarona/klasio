package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.service.RegisterStudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenants")
public class RegistrationController {

    private final RegisterStudentService registerStudentService;

    public RegistrationController(RegisterStudentService registerStudentService) {
        this.registerStudentService = registerStudentService;
    }

    public record RegistrationRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull LocalDate dateOfBirth,
            @NotBlank @Pattern(regexp = "^(CC|TI|CE|PA|RC)$", message = "Invalid document type") String identityDocumentType,
            @NotBlank @Size(max = 30) String identityNumber,
            @NotBlank String eps,
            @NotBlank @Email String email,
            String bloodType,
            String phone,
            String tutorFirstName,
            String tutorLastName,
            String tutorRelationship,
            String tutorPhone,
            String tutorEmail
    ) {}

    @PostMapping("/{tenantSlug}/register")
    public ResponseEntity<Map<String, String>> register(
            @PathVariable String tenantSlug,
            @Valid @RequestBody RegistrationRequest request) {

        RegisterStudentCommand command = new RegisterStudentCommand(
                tenantSlug,
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.identityDocumentType(),
                request.identityNumber(),
                request.eps(),
                request.email(),
                request.bloodType(),
                request.phone(),
                request.tutorFirstName(),
                request.tutorLastName(),
                request.tutorRelationship(),
                request.tutorPhone(),
                request.tutorEmail()
        );

        registerStudentService.register(command);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Registration successful. Please check your email to set your password and activate your account."));
    }
}
