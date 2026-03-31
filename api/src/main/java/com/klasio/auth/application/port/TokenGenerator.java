package com.klasio.auth.application.port;

public interface TokenGenerator {

    String generateRawToken();

    String hashToken(String rawToken);
}
