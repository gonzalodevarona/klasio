package com.klasio.student.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.student.domain.event.StudentCreated;
import com.klasio.student.domain.event.StudentDeactivated;
import com.klasio.student.domain.event.StudentReactivated;
import com.klasio.student.domain.event.StudentUpdated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String FIRST_NAME = "Carlos";
    private static final String LAST_NAME = "Garcia";
    private static final String EMAIL = "carlos.garcia@example.com";
    private static final LocalDate ADULT_DATE_OF_BIRTH = LocalDate.of(2000, 1, 15);
    private static final LocalDate MINOR_DATE_OF_BIRTH = LocalDate.now().minusYears(10);
    private static final String EPS = "Sura";
    private static final String IDENTITY_NUMBER = "1234567890";
    private static final IdentityDocumentType IDENTITY_DOC_TYPE = IdentityDocumentType.CC;
    private static final BloodType BLOOD_TYPE = BloodType.O_POSITIVE;
    private static final String PHONE = "3001234567";
    private static final UUID CREATED_BY = UUID.randomUUID();

    private static Student createAdultStudent() {
        return Student.create(
                TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                BLOOD_TYPE, PHONE,
                null, null, null, null, null,
                CREATED_BY
        );
    }

    private static Student createMinorStudent() {
        return Student.create(
                TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                MINOR_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IdentityDocumentType.TI,
                BLOOD_TYPE, PHONE,
                "Pedro", "Garcia", "Father", "3009876543", "pedro@example.com",
                CREATED_BY
        );
    }

    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("should create student with ACTIVE status")
        void shouldCreateWithActiveStatus() {
            Student student = createAdultStudent();

            assertThat(student.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should generate a non-null id")
        void shouldGenerateId() {
            Student student = createAdultStudent();

            assertThat(student.getId()).isNotNull();
            assertThat(student.getId().value()).isNotNull();
        }

        @Test
        @DisplayName("should set createdAt to a non-null instant")
        void shouldSetCreatedAt() {
            Student student = createAdultStudent();

            assertThat(student.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should store all provided fields")
        void shouldStoreAllFields() {
            Student student = createAdultStudent();

            assertThat(student.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(student.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(student.getLastName()).isEqualTo(LAST_NAME);
            assertThat(student.getEmail()).isEqualTo(EMAIL);
            assertThat(student.getDateOfBirth()).isEqualTo(ADULT_DATE_OF_BIRTH);
            assertThat(student.getEps()).isEqualTo(EPS);
            assertThat(student.getIdentityNumber()).isEqualTo(IDENTITY_NUMBER);
            assertThat(student.getIdentityDocumentType()).isEqualTo(IDENTITY_DOC_TYPE);
            assertThat(student.getBloodType()).isEqualTo(BLOOD_TYPE);
            assertThat(student.getPhone()).isEqualTo(PHONE);
            assertThat(student.getCreatedBy()).isEqualTo(CREATED_BY);
            assertThat(student.getUpdatedAt()).isNull();
            assertThat(student.getUpdatedBy()).isNull();
            assertThat(student.getDeactivatedAt()).isNull();
            assertThat(student.getDeactivatedBy()).isNull();
        }

        @Test
        @DisplayName("should store tutor fields for minor student")
        void shouldStoreTutorFieldsForMinor() {
            Student student = createMinorStudent();

            assertThat(student.getTutorFirstName()).isEqualTo("Pedro");
            assertThat(student.getTutorLastName()).isEqualTo("Garcia");
            assertThat(student.getTutorRelationship()).isEqualTo("Father");
            assertThat(student.getTutorPhone()).isEqualTo("3009876543");
            assertThat(student.getTutorEmail()).isEqualTo("pedro@example.com");
        }

        @Test
        @DisplayName("should allow null tutor fields for adult student")
        void shouldAllowNullTutorFieldsForAdult() {
            Student student = createAdultStudent();

            assertThat(student.getTutorFirstName()).isNull();
            assertThat(student.getTutorLastName()).isNull();
            assertThat(student.getTutorRelationship()).isNull();
            assertThat(student.getTutorPhone()).isNull();
            assertThat(student.getTutorEmail()).isNull();
        }

        @Test
        @DisplayName("should allow null bloodType and phone")
        void shouldAllowNullOptionalFields() {
            Student student = Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    null, null,
                    null, null, null, null, null,
                    CREATED_BY
            );

            assertThat(student.getBloodType()).isNull();
            assertThat(student.getPhone()).isNull();
        }

        @Test
        @DisplayName("should publish StudentCreated domain event")
        void shouldPublishStudentCreatedEvent() {
            Student student = createAdultStudent();

            List<DomainEvent> events = student.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(StudentCreated.class);

            StudentCreated created = (StudentCreated) events.get(0);
            assertThat(created.studentId()).isEqualTo(student.getId().value());
            assertThat(created.tenantId()).isEqualTo(TENANT_ID);
            assertThat(created.firstName()).isEqualTo(FIRST_NAME);
            assertThat(created.lastName()).isEqualTo(LAST_NAME);
            assertThat(created.email()).isEqualTo(EMAIL);
            assertThat(created.createdBy()).isEqualTo(CREATED_BY);
            assertThat(created.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("should capitalize firstName")
        void shouldCapitalizeFirstName() {
            Student student = Student.create(
                    TENANT_ID, "carlos", LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            );

            assertThat(student.getFirstName()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("should capitalize lastName")
        void shouldCapitalizeLastName() {
            Student student = Student.create(
                    TENANT_ID, FIRST_NAME, "garcia", EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            );

            assertThat(student.getLastName()).isEqualTo("Garcia");
        }

        @Test
        @DisplayName("should lowercase email")
        void shouldLowercaseEmail() {
            Student student = Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, "Carlos.Garcia@Example.COM",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            );

            assertThat(student.getEmail()).isEqualTo("carlos.garcia@example.com");
        }

        @Test
        @DisplayName("should trim and capitalize firstName with leading/trailing spaces")
        void shouldTrimAndCapitalizeFirstName() {
            Student student = Student.create(
                    TENANT_ID, "  carlos  ", LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            );

            assertThat(student.getFirstName()).isEqualTo("Carlos");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null tenantId")
        void shouldRejectNullTenantId() {
            assertThatThrownBy(() -> Student.create(
                    null, FIRST_NAME, LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Tenant id must not be null");
        }

        @Test
        @DisplayName("should reject blank firstName")
        void shouldRejectBlankFirstName() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, "  ", LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("First name");
        }

        @Test
        @DisplayName("should reject null firstName")
        void shouldRejectNullFirstName() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, null, LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("First name");
        }

        @Test
        @DisplayName("should reject blank lastName")
        void shouldRejectBlankLastName() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, "  ", EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Last name");
        }

        @Test
        @DisplayName("should reject null lastName")
        void shouldRejectNullLastName() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, null, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Last name");
        }

        @Test
        @DisplayName("should reject blank email")
        void shouldRejectBlankEmail() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, "  ",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("should reject null email")
        void shouldRejectNullEmail() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, null,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("should reject invalid email format")
        void shouldRejectInvalidEmailFormat() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, "not-an-email",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email format is invalid");
        }

        @Test
        @DisplayName("should reject email without domain extension")
        void shouldRejectEmailWithoutDomainExtension() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, "user@domain",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email format is invalid");
        }

        @Test
        @DisplayName("should reject null dateOfBirth")
        void shouldRejectNullDateOfBirth() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    null, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Date of birth must not be null");
        }

        @Test
        @DisplayName("should reject date of birth in the future")
        void shouldRejectFutureDateOfBirth() {
            LocalDate futureDate = LocalDate.now().plusDays(1);

            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    futureDate, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Date of birth cannot be in the future");
        }

        @Test
        @DisplayName("should reject blank eps")
        void shouldRejectBlankEps() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, "  ", IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EPS");
        }

        @Test
        @DisplayName("should reject blank identityNumber")
        void shouldRejectBlankIdentityNumber() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, "  ", IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Identity number");
        }

        @Test
        @DisplayName("should reject null identityDocumentType")
        void shouldRejectNullIdentityDocumentType() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, null,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Identity document type must not be null");
        }

        @Test
        @DisplayName("should reject minor without tutor first name")
        void shouldRejectMinorWithoutTutorFirstName() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    MINOR_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IdentityDocumentType.TI,
                    BLOOD_TYPE, PHONE,
                    null, "Garcia", "Father", "3009876543", null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tutor first name is required for minors");
        }

        @Test
        @DisplayName("should reject minor without tutor last name")
        void shouldRejectMinorWithoutTutorLastName() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    MINOR_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IdentityDocumentType.TI,
                    BLOOD_TYPE, PHONE,
                    "Pedro", null, "Father", "3009876543", null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tutor last name is required for minors");
        }

        @Test
        @DisplayName("should reject minor without tutor relationship")
        void shouldRejectMinorWithoutTutorRelationship() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    MINOR_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IdentityDocumentType.TI,
                    BLOOD_TYPE, PHONE,
                    "Pedro", "Garcia", null, "3009876543", null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tutor relationship is required for minors");
        }

        @Test
        @DisplayName("should reject minor without tutor phone")
        void shouldRejectMinorWithoutTutorPhone() {
            assertThatThrownBy(() -> Student.create(
                    TENANT_ID, FIRST_NAME, LAST_NAME, EMAIL,
                    MINOR_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IdentityDocumentType.TI,
                    BLOOD_TYPE, PHONE,
                    "Pedro", "Garcia", "Father", null, null,
                    CREATED_BY
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tutor phone is required for minors");
        }

        @Test
        @DisplayName("should accept minor with all required tutor fields")
        void shouldAcceptMinorWithAllTutorFields() {
            Student student = createMinorStudent();

            assertThat(student).isNotNull();
            assertThat(student.isMinor()).isTrue();
            assertThat(student.getTutorFirstName()).isEqualTo("Pedro");
        }
    }

    @Nested
    @DisplayName("calculateAge() and isMinor()")
    class AgeCalculation {

        @Test
        @DisplayName("should calculate correct age for adult")
        void shouldCalculateCorrectAgeForAdult() {
            Student student = createAdultStudent();

            int expectedAge = java.time.Period.between(ADULT_DATE_OF_BIRTH, LocalDate.now()).getYears();
            assertThat(student.calculateAge()).isEqualTo(expectedAge);
        }

        @Test
        @DisplayName("should return false for isMinor when adult")
        void shouldReturnFalseForIsMinorWhenAdult() {
            Student student = createAdultStudent();

            assertThat(student.isMinor()).isFalse();
        }

        @Test
        @DisplayName("should calculate correct age for minor")
        void shouldCalculateCorrectAgeForMinor() {
            Student student = createMinorStudent();

            assertThat(student.calculateAge()).isEqualTo(10);
        }

        @Test
        @DisplayName("should return true for isMinor when under 18")
        void shouldReturnTrueForIsMinorWhenUnder18() {
            Student student = createMinorStudent();

            assertThat(student.isMinor()).isTrue();
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private Student createActiveStudent() {
            Student student = createAdultStudent();
            student.clearDomainEvents();
            return student;
        }

        @Test
        @DisplayName("should update fields and publish StudentUpdated event")
        void shouldUpdateFieldsAndPublishEvent() {
            Student student = createActiveStudent();
            UUID updatedBy = UUID.randomUUID();
            LocalDate newDateOfBirth = LocalDate.of(1995, 6, 20);

            student.update(
                    "Maria", "Lopez", "maria.lopez@example.com",
                    newDateOfBirth, "Nueva EPS", "9876543210", IdentityDocumentType.CC,
                    BloodType.A_POSITIVE, "3005555555",
                    null, null, null, null, null,
                    updatedBy
            );

            assertThat(student.getFirstName()).isEqualTo("Maria");
            assertThat(student.getLastName()).isEqualTo("Lopez");
            assertThat(student.getEmail()).isEqualTo("maria.lopez@example.com");
            assertThat(student.getDateOfBirth()).isEqualTo(newDateOfBirth);
            assertThat(student.getEps()).isEqualTo("Nueva EPS");
            assertThat(student.getIdentityNumber()).isEqualTo("9876543210");
            assertThat(student.getIdentityDocumentType()).isEqualTo(IdentityDocumentType.CC);
            assertThat(student.getBloodType()).isEqualTo(BloodType.A_POSITIVE);
            assertThat(student.getPhone()).isEqualTo("3005555555");
            assertThat(student.getUpdatedAt()).isNotNull();
            assertThat(student.getUpdatedBy()).isEqualTo(updatedBy);

            List<DomainEvent> events = student.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(StudentUpdated.class);

            StudentUpdated updated = (StudentUpdated) events.get(0);
            assertThat(updated.studentId()).isEqualTo(student.getId().value());
            assertThat(updated.firstName()).isEqualTo("Maria");
            assertThat(updated.lastName()).isEqualTo("Lopez");
            assertThat(updated.email()).isEqualTo("maria.lopez@example.com");
            assertThat(updated.updatedBy()).isEqualTo(updatedBy);
        }

        @Test
        @DisplayName("should capitalize firstName and lastName on update")
        void shouldCapitalizeNamesOnUpdate() {
            Student student = createActiveStudent();
            UUID updatedBy = UUID.randomUUID();

            student.update(
                    "maria", "lopez", "maria.lopez@example.com",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    updatedBy
            );

            assertThat(student.getFirstName()).isEqualTo("Maria");
            assertThat(student.getLastName()).isEqualTo("Lopez");
        }

        @Test
        @DisplayName("should lowercase email on update")
        void shouldLowercaseEmailOnUpdate() {
            Student student = createActiveStudent();
            UUID updatedBy = UUID.randomUUID();

            student.update(
                    "Maria", "Lopez", "Maria.Lopez@Example.COM",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    updatedBy
            );

            assertThat(student.getEmail()).isEqualTo("maria.lopez@example.com");
        }

        @Test
        @DisplayName("should reject blank firstName on update")
        void shouldRejectBlankFirstNameOnUpdate() {
            Student student = createActiveStudent();

            assertThatThrownBy(() -> student.update(
                    "  ", LAST_NAME, EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    UUID.randomUUID()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("First name");
        }

        @Test
        @DisplayName("should reject blank lastName on update")
        void shouldRejectBlankLastNameOnUpdate() {
            Student student = createActiveStudent();

            assertThatThrownBy(() -> student.update(
                    FIRST_NAME, "  ", EMAIL,
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    UUID.randomUUID()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Last name");
        }

        @Test
        @DisplayName("should reject invalid email on update")
        void shouldRejectInvalidEmailOnUpdate() {
            Student student = createActiveStudent();

            assertThatThrownBy(() -> student.update(
                    FIRST_NAME, LAST_NAME, "bad-email",
                    ADULT_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    UUID.randomUUID()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email format is invalid");
        }

        @Test
        @DisplayName("should reject future date of birth on update")
        void shouldRejectFutureDateOfBirthOnUpdate() {
            Student student = createActiveStudent();
            LocalDate futureDate = LocalDate.now().plusDays(1);

            assertThatThrownBy(() -> student.update(
                    FIRST_NAME, LAST_NAME, EMAIL,
                    futureDate, EPS, IDENTITY_NUMBER, IDENTITY_DOC_TYPE,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    UUID.randomUUID()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Date of birth cannot be in the future");
        }

        @Test
        @DisplayName("should reject minor without tutor on update")
        void shouldRejectMinorWithoutTutorOnUpdate() {
            Student student = createActiveStudent();

            assertThatThrownBy(() -> student.update(
                    FIRST_NAME, LAST_NAME, EMAIL,
                    MINOR_DATE_OF_BIRTH, EPS, IDENTITY_NUMBER, IdentityDocumentType.TI,
                    BLOOD_TYPE, PHONE,
                    null, null, null, null, null,
                    UUID.randomUUID()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tutor first name is required for minors");
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class DeactivateTests {

        private Student createActiveStudent() {
            Student student = createAdultStudent();
            student.clearDomainEvents();
            return student;
        }

        @Test
        @DisplayName("should transition to INACTIVE and publish StudentDeactivated event")
        void shouldDeactivateAndPublishEvent() {
            Student student = createActiveStudent();
            UUID deactivatedBy = UUID.randomUUID();

            student.deactivate(deactivatedBy);

            assertThat(student.getStatus()).isEqualTo("INACTIVE");
            assertThat(student.getDeactivatedAt()).isNotNull();
            assertThat(student.getDeactivatedBy()).isEqualTo(deactivatedBy);
            assertThat(student.getUpdatedAt()).isNotNull();
            assertThat(student.getUpdatedBy()).isEqualTo(deactivatedBy);

            List<DomainEvent> events = student.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(StudentDeactivated.class);

            StudentDeactivated deactivated = (StudentDeactivated) events.get(0);
            assertThat(deactivated.studentId()).isEqualTo(student.getId().value());
            assertThat(deactivated.tenantId()).isEqualTo(TENANT_ID);
            assertThat(deactivated.deactivatedBy()).isEqualTo(deactivatedBy);
            assertThat(deactivated.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw IllegalStateException if already INACTIVE")
        void shouldThrowWhenAlreadyInactive() {
            Student student = createActiveStudent();
            UUID deactivatedBy = UUID.randomUUID();

            student.deactivate(deactivatedBy);

            assertThatThrownBy(() -> student.deactivate(deactivatedBy))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Student is already inactive");
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class ReactivateTests {

        private Student createInactiveStudent() {
            Student student = createAdultStudent();
            student.clearDomainEvents();
            student.deactivate(UUID.randomUUID());
            student.clearDomainEvents();
            return student;
        }

        @Test
        @DisplayName("should transition to ACTIVE and publish StudentReactivated event")
        void shouldReactivateAndPublishEvent() {
            Student student = createInactiveStudent();
            UUID reactivatedBy = UUID.randomUUID();

            student.reactivate(reactivatedBy);

            assertThat(student.getStatus()).isEqualTo("ACTIVE");
            assertThat(student.getDeactivatedAt()).isNull();
            assertThat(student.getDeactivatedBy()).isNull();
            assertThat(student.getUpdatedAt()).isNotNull();
            assertThat(student.getUpdatedBy()).isEqualTo(reactivatedBy);

            List<DomainEvent> events = student.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(StudentReactivated.class);

            StudentReactivated reactivated = (StudentReactivated) events.get(0);
            assertThat(reactivated.studentId()).isEqualTo(student.getId().value());
            assertThat(reactivated.tenantId()).isEqualTo(TENANT_ID);
            assertThat(reactivated.reactivatedBy()).isEqualTo(reactivatedBy);
            assertThat(reactivated.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw IllegalStateException if student is not INACTIVE")
        void shouldThrowWhenNotInactive() {
            Student student = createAdultStudent();

            assertThatThrownBy(() -> student.reactivate(UUID.randomUUID()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Student is not inactive");
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        @DisplayName("should return unmodifiable list from getDomainEvents")
        void shouldReturnUnmodifiableList() {
            Student student = createAdultStudent();

            List<DomainEvent> events = student.getDomainEvents();
            assertThatThrownBy(events::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should clear all domain events when clearDomainEvents is called")
        void shouldClearDomainEvents() {
            Student student = createAdultStudent();

            student.clearDomainEvents();

            assertThat(student.getDomainEvents()).isEmpty();
        }
    }
}
