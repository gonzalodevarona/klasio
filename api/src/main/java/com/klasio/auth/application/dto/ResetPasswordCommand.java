package com.klasio.auth.application.dto;

public record ResetPasswordCommand(String token, String newPassword) {}
