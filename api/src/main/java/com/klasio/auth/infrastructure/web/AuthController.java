package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.dto.LoginCommand;
import com.klasio.auth.application.dto.LoginResult;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.LoginService;
import com.klasio.auth.application.service.LogoutService;
import com.klasio.auth.application.service.RefreshTokenService;
import com.klasio.auth.application.service.RequestPasswordResetService;
import com.klasio.auth.application.service.ResendVerificationEmailService;
import com.klasio.auth.application.service.ResetPasswordService;
import com.klasio.auth.application.service.VerifyEmailService;
import com.klasio.auth.domain.model.User;
import com.klasio.shared.infrastructure.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginService loginService;
    private final LogoutService logoutService;
    private final RefreshTokenService refreshTokenService;
    private final VerifyEmailService verifyEmailService;
    private final ResendVerificationEmailService resendVerificationEmailService;
    private final RequestPasswordResetService requestPasswordResetService;
    private final ResetPasswordService resetPasswordService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    public AuthController(LoginService loginService,
                          LogoutService logoutService,
                          RefreshTokenService refreshTokenService,
                          VerifyEmailService verifyEmailService,
                          ResendVerificationEmailService resendVerificationEmailService,
                          RequestPasswordResetService requestPasswordResetService,
                          ResetPasswordService resetPasswordService,
                          JwtProperties jwtProperties,
                          UserRepository userRepository) {
        this.loginService = loginService;
        this.logoutService = logoutService;
        this.refreshTokenService = refreshTokenService;
        this.verifyEmailService = verifyEmailService;
        this.resendVerificationEmailService = resendVerificationEmailService;
        this.requestPasswordResetService = requestPasswordResetService;
        this.resetPasswordService = resetPasswordService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record ResendVerificationRequest(@NotBlank @Email String email, @NotBlank String tenantSlug) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMe(Authentication authentication) {
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String userIdStr = (String) details.get("userId");

        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletResponse response) {
        LoginResult result = loginService.login(new LoginCommand(request.email(), request.password()));

        addCookie(response, "accessToken", result.accessToken(),
                (int) (jwtProperties.accessTokenExpiration() / 1000));
        addCookie(response, "refreshToken", result.refreshToken(),
                (int) (jwtProperties.refreshTokenExpiration() / 1000));

        return ResponseEntity.ok(Map.of(
                "userId", result.userId().toString(),
                "role", result.role().name(),
                "dashboardUrl", result.dashboardUrl(),
                "tenantId", result.tenantId() != null ? result.tenantId().toString() : ""
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        String refreshToken = extractCookieValue(request, "refreshToken");
        UUID userId = null;
        UUID tenantId = null;

        if (authentication != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
            if (details != null) {
                String uid = (String) details.get("userId");
                if (uid != null) userId = UUID.fromString(uid);
                String tid = (String) details.get("tenantId");
                if (tid != null) tenantId = UUID.fromString(tid);
            }
        }

        logoutService.logout(refreshToken, userId, tenantId);

        clearCookie(response, "accessToken");
        clearCookie(response, "refreshToken");

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request,
                                                        HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", Map.of("code", "REFRESH_TOKEN_INVALID",
                            "message", "No refresh token provided")));
        }

        LoginResult result = refreshTokenService.refresh(refreshToken);

        addCookie(response, "accessToken", result.accessToken(),
                (int) (jwtProperties.accessTokenExpiration() / 1000));
        addCookie(response, "refreshToken", result.refreshToken(),
                (int) (jwtProperties.refreshTokenExpiration() / 1000));

        return ResponseEntity.ok(Map.of(
                "userId", result.userId().toString(),
                "role", result.role().name()
        ));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        verifyEmailService.verify(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationEmailService.resend(request.email(), request.tenantSlug());
        return ResponseEntity.accepted().body(Map.of(
                "message", "If an unverified account exists with this email, a new verification email has been sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetService.requestReset(request.email());
        return ResponseEntity.accepted().body(Map.of(
                "message", "If an account exists with this email, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordService.reset(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // false for local dev; true in production
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
