package com.klasio.shared.infrastructure.exception;

import com.klasio.auth.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        var error = new ErrorResponse.ErrorDetail("VALIDATION_ERROR", "Validation failed", fieldErrors);
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        var error = new ErrorResponse.ErrorDetail("MAX_UPLOAD_SIZE_EXCEEDED", "File size exceeds the maximum allowed limit");
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        var error = new ErrorResponse.ErrorDetail("ACCESS_DENIED", "Insufficient permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("TENANT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSlugAlreadyExists(SlugAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("SLUG_ALREADY_EXISTS", ex.getMessage(), null, ex.getSuggestedSlug());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(TenantAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantAlreadyInactive(TenantAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("TENANT_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProgramNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProgramNotFound(ProgramNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("PROGRAM_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProgramNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProgramNameAlreadyExists(ProgramNameAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("PROGRAM_NAME_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProgramAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleProgramAlreadyInactive(ProgramAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("PROGRAM_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProgramAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleProgramAlreadyActive(ProgramAlreadyActiveException ex) {
        var error = new ErrorResponse.ErrorDetail("PROGRAM_ALREADY_ACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProgramPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProgramPlanNotFound(ProgramPlanNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("PLAN_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProgramPlanNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProgramPlanNameAlreadyExists(ProgramPlanNameAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("PLAN_NAME_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProfessorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfessorNotFound(ProfessorNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("PROFESSOR_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProfessorEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProfessorEmailAlreadyExists(ProfessorEmailAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("PROFESSOR_EMAIL_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProfessorIdentityNumberAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProfessorIdentityNumberAlreadyExists(
            ProfessorIdentityNumberAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("PROFESSOR_IDENTITY_NUMBER_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProfessorAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleProfessorAlreadyActive(ProfessorAlreadyActiveException ex) {
        var error = new ErrorResponse.ErrorDetail("PROFESSOR_ALREADY_ACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ProfessorAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleProfessorAlreadyInactive(ProfessorAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("PROFESSOR_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ClassNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClassNotFound(ClassNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("CLASS_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ClassNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleClassNameAlreadyExists(ClassNameAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("CLASS_NAME_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ClassAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleClassAlreadyActive(ClassAlreadyActiveException ex) {
        var error = new ErrorResponse.ErrorDetail("CLASS_ALREADY_ACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ClassAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleClassAlreadyInactive(ClassAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("CLASS_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ClassNoProfessorAssignedException.class)
    public ResponseEntity<ErrorResponse> handleClassNoProfessorAssigned(ClassNoProfessorAssignedException ex) {
        var error = new ErrorResponse.ErrorDetail("CLASS_NO_PROFESSOR_ASSIGNED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotFound(StudentNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("STUDENT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(StudentEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleStudentEmailAlreadyExists(StudentEmailAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("STUDENT_EMAIL_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(StudentAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleStudentAlreadyActive(StudentAlreadyActiveException ex) {
        var error = new ErrorResponse.ErrorDetail("STUDENT_ALREADY_ACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(StudentAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleStudentAlreadyInactive(StudentAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("STUDENT_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(EnrollmentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentAlreadyExists(EnrollmentAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("ENROLLMENT_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentNotFound(EnrollmentNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("ENROLLMENT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(EnrollmentAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentAlreadyInactive(EnrollmentAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("ENROLLMENT_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(MembershipNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMembershipNotFound(MembershipNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("MEMBERSHIP_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(MembershipAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleMembershipAlreadyActive(MembershipAlreadyActiveException ex) {
        var error = new ErrorResponse.ErrorDetail("MEMBERSHIP_ALREADY_ACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        var error = new ErrorResponse.ErrorDetail("INVALID_STATUS_TRANSITION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(NegativeBalanceException.class)
    public ResponseEntity<ErrorResponse> handleNegativeBalance(NegativeBalanceException ex) {
        var error = new ErrorResponse.ErrorDetail("NEGATIVE_BALANCE", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(MembershipNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleMembershipNotActive(MembershipNotActiveException ex) {
        var error = new ErrorResponse.ErrorDetail("MEMBERSHIP_NOT_ACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(PaymentProofNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProofNotFound(PaymentProofNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("PAYMENT_PROOF_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(InvalidMembershipStatusForUploadException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMembershipStatusForUpload(
            InvalidMembershipStatusForUploadException ex) {
        var error = new ErrorResponse.ErrorDetail("INVALID_MEMBERSHIP_STATUS_FOR_UPLOAD", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(error));
    }

    @ExceptionHandler(ManagerProgramMismatchException.class)
    public ResponseEntity<ErrorResponse> handleManagerProgramMismatch(ManagerProgramMismatchException ex) {
        var error = new ErrorResponse.ErrorDetail("MANAGER_PROGRAM_MISMATCH", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileType(InvalidFileTypeException ex) {
        var error = new ErrorResponse.ErrorDetail("INVALID_FILE_TYPE", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        var error = new ErrorResponse.ErrorDetail("VALIDATION_ERROR",
                "Required parameter '%s' is missing".formatted(ex.getParameterName()));
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        var error = new ErrorResponse.ErrorDetail("INVALID_CREDENTIALS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(error));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", Map.of(
                        "code", "ACCOUNT_LOCKED",
                        "message", ex.getMessage(),
                        "lockedUntil", ex.getLockedUntil().toString()
                )
        ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex) {
        var error = new ErrorResponse.ErrorDetail("EMAIL_NOT_VERIFIED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        var error = new ErrorResponse.ErrorDetail("EMAIL_ALREADY_REGISTERED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(IdentityNumberAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleIdentityAlreadyRegistered(IdentityNumberAlreadyRegisteredException ex) {
        var error = new ErrorResponse.ErrorDetail("IDENTITY_NUMBER_ALREADY_REGISTERED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(VerificationTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleVerificationExpired(VerificationTokenExpiredException ex) {
        var error = new ErrorResponse.ErrorDetail("VERIFICATION_TOKEN_EXPIRED", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(VerificationTokenAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleVerificationUsed(VerificationTokenAlreadyUsedException ex) {
        var error = new ErrorResponse.ErrorDetail("VERIFICATION_TOKEN_ALREADY_USED", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(ResetTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleResetExpired(ResetTokenExpiredException ex) {
        var error = new ErrorResponse.ErrorDetail("RESET_TOKEN_EXPIRED", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(ResetTokenAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleResetUsed(ResetTokenAlreadyUsedException ex) {
        var error = new ErrorResponse.ErrorDetail("RESET_TOKEN_ALREADY_USED", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordPolicy(PasswordPolicyViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", Map.of(
                        "code", "PASSWORD_POLICY_VIOLATION",
                        "message", ex.getMessage(),
                        "violations", ex.getViolations()
                )
        ));
    }

    @ExceptionHandler(RoleElevationForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleRoleElevation(RoleElevationForbiddenException ex) {
        var error = new ErrorResponse.ErrorDetail("ROLE_ELEVATION_FORBIDDEN", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        var error = new ErrorResponse.ErrorDetail("VALIDATION_ERROR", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        var error = new ErrorResponse.ErrorDetail("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(error));
    }
}
