package com.klasio.auth.domain;

import com.klasio.auth.domain.model.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @Test
    void validPassword_noViolations() {
        assertTrue(PasswordPolicy.isSatisfiedBy("Admin123!"));
        assertTrue(PasswordPolicy.validate("Admin123!").isEmpty());
    }

    @Test
    void tooShort_returnsMinimumLength() {
        List<String> violations = PasswordPolicy.validate("Ad1!");
        assertTrue(violations.contains("MINIMUM_LENGTH"));
    }

    @Test
    void noUppercase_returnsRequiresUppercase() {
        List<String> violations = PasswordPolicy.validate("admin123!");
        assertTrue(violations.contains("REQUIRES_UPPERCASE"));
    }

    @Test
    void noDigit_returnsRequiresDigit() {
        List<String> violations = PasswordPolicy.validate("AdminPass!");
        assertTrue(violations.contains("REQUIRES_DIGIT"));
    }

    @Test
    void noSpecialChar_returnsRequiresSpecialChar() {
        List<String> violations = PasswordPolicy.validate("Admin1234");
        assertTrue(violations.contains("REQUIRES_SPECIAL_CHAR"));
    }

    @Test
    void multipleViolations_returnsAll() {
        List<String> violations = PasswordPolicy.validate("abc");
        assertEquals(4, violations.size());
    }

    @Test
    void nullPassword_returnsAllViolations() {
        List<String> violations = PasswordPolicy.validate(null);
        assertEquals(4, violations.size());
    }
}
