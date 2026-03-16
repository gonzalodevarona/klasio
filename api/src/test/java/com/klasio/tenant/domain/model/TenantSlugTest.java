package com.klasio.tenant.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantSlugTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should accept a valid slug like 'futbol-bogota'")
        void shouldAcceptValidSlug() {
            TenantSlug slug = new TenantSlug("futbol-bogota");
            assertEquals("futbol-bogota", slug.value());
        }

        @Test
        @DisplayName("should accept minimum length slug of 3 characters")
        void shouldAcceptMinLengthSlug() {
            TenantSlug slug = new TenantSlug("abc");
            assertEquals("abc", slug.value());
        }

        @Test
        @DisplayName("should accept maximum length slug of 60 characters")
        void shouldAcceptMaxLengthSlug() {
            String sixtyCharSlug = "a".repeat(60);
            TenantSlug slug = new TenantSlug(sixtyCharSlug);
            assertEquals(sixtyCharSlug, slug.value());
        }

        @Test
        @DisplayName("should reject slug shorter than 3 characters")
        void shouldRejectTooShortSlug() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("ab"));
        }

        @Test
        @DisplayName("should reject slug longer than 60 characters")
        void shouldRejectTooLongSlug() {
            String tooLong = "a".repeat(61);
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug(tooLong));
        }

        @Test
        @DisplayName("should reject slug with uppercase letters")
        void shouldRejectUppercase() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("Futbol"));
        }

        @Test
        @DisplayName("should reject slug with special characters")
        void shouldRejectSpecialChars() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("futbol_bogota"));
        }

        @Test
        @DisplayName("should reject slug with leading hyphen")
        void shouldRejectLeadingHyphen() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("-futbol"));
        }

        @Test
        @DisplayName("should reject slug with trailing hyphen")
        void shouldRejectTrailingHyphen() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("futbol-"));
        }

        @Test
        @DisplayName("should reject slug with consecutive hyphens")
        void shouldRejectConsecutiveHyphens() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("futbol--bogota"));
        }

        @Test
        @DisplayName("should reject blank slug")
        void shouldRejectBlankSlug() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug("   "));
        }

        @Test
        @DisplayName("should reject null slug")
        void shouldRejectNullSlug() {
            assertThrows(IllegalArgumentException.class, () -> new TenantSlug(null));
        }
    }

    @Nested
    @DisplayName("fromName() factory")
    class FromNameFactory {

        @Test
        @DisplayName("should convert 'Futbol Bogota' to 'futbol-bogota'")
        void shouldConvertSimpleName() {
            TenantSlug slug = TenantSlug.fromName("Futbol Bogota");
            assertEquals("futbol-bogota", slug.value());
        }

        @Test
        @DisplayName("should strip diacritics from 'Futbol Bogota' with accents")
        void shouldStripDiacritics() {
            TenantSlug slug = TenantSlug.fromName("Fútbol Bogotá");
            assertEquals("futbol-bogota", slug.value());
        }

        @Test
        @DisplayName("should handle multiple consecutive spaces")
        void shouldHandleMultipleSpaces() {
            TenantSlug slug = TenantSlug.fromName("Futbol   Bogota");
            assertEquals("futbol-bogota", slug.value());
        }

        @Test
        @DisplayName("should handle special characters in name")
        void shouldHandleSpecialChars() {
            TenantSlug slug = TenantSlug.fromName("Fútbol & Básquet (Bogotá)");
            assertEquals("futbol-basquet-bogota", slug.value());
        }

        @Test
        @DisplayName("should trim leading and trailing non-alphanumeric from result")
        void shouldTrimLeadingTrailingNonAlpha() {
            TenantSlug slug = TenantSlug.fromName("  Futbol Bogota  ");
            assertEquals("futbol-bogota", slug.value());
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThrows(IllegalArgumentException.class, () -> TenantSlug.fromName("   "));
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThrows(IllegalArgumentException.class, () -> TenantSlug.fromName(null));
        }
    }
}
