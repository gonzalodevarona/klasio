package com.klasio.student.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.student.domain.event.StudentCreated;
import com.klasio.student.domain.event.StudentDeactivated;
import com.klasio.student.domain.event.StudentReactivated;
import com.klasio.student.domain.event.StudentUpdated;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class Student {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final StudentId id;
    private final UUID tenantId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate dateOfBirth;
    private String eps;
    private String identityNumber;
    private IdentityDocumentType identityDocumentType;
    private BloodType bloodType;
    private String phone;
    private String tutorFirstName;
    private String tutorLastName;
    private String tutorRelationship;
    private String tutorPhone;
    private String tutorEmail;
    private String status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deactivatedAt;
    private UUID deactivatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Student(StudentId id,
                    UUID tenantId,
                    String firstName,
                    String lastName,
                    String email,
                    LocalDate dateOfBirth,
                    String eps,
                    String identityNumber,
                    IdentityDocumentType identityDocumentType,
                    BloodType bloodType,
                    String phone,
                    String tutorFirstName,
                    String tutorLastName,
                    String tutorRelationship,
                    String tutorPhone,
                    String tutorEmail,
                    String status,
                    Instant createdAt,
                    UUID createdBy,
                    Instant updatedAt,
                    UUID updatedBy,
                    Instant deactivatedAt,
                    UUID deactivatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.eps = eps;
        this.identityNumber = identityNumber;
        this.identityDocumentType = identityDocumentType;
        this.bloodType = bloodType;
        this.phone = phone;
        this.tutorFirstName = tutorFirstName;
        this.tutorLastName = tutorLastName;
        this.tutorRelationship = tutorRelationship;
        this.tutorPhone = tutorPhone;
        this.tutorEmail = tutorEmail;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.deactivatedAt = deactivatedAt;
        this.deactivatedBy = deactivatedBy;
    }

    public static Student create(UUID tenantId,
                                 String firstName,
                                 String lastName,
                                 String email,
                                 LocalDate dateOfBirth,
                                 String eps,
                                 String identityNumber,
                                 IdentityDocumentType identityDocumentType,
                                 BloodType bloodType,
                                 String phone,
                                 String tutorFirstName,
                                 String tutorLastName,
                                 String tutorRelationship,
                                 String tutorPhone,
                                 String tutorEmail,
                                 UUID createdBy) {
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(createdBy, "Created by must not be null");
        Objects.requireNonNull(dateOfBirth, "Date of birth must not be null");
        Objects.requireNonNull(identityDocumentType, "Identity document type must not be null");
        validateNotBlank(firstName, "First name");
        validateNotBlank(lastName, "Last name");
        validateNotBlank(email, "Email");
        validateNotBlank(eps, "EPS");
        validateNotBlank(identityNumber, "Identity number");
        validateEmail(email);
        validateDateOfBirth(dateOfBirth);
        validateTutorData(dateOfBirth, tutorFirstName, tutorLastName, tutorRelationship, tutorPhone);

        String normalizedFirstName = capitalize(firstName);
        String normalizedLastName = capitalize(lastName);
        String normalizedEmail = email.trim().toLowerCase();
        String trimmedIdentityNumber = identityNumber.trim();

        Instant now = Instant.now();
        StudentId id = StudentId.generate();

        Student student = new Student(
                id, tenantId, normalizedFirstName, normalizedLastName, normalizedEmail,
                dateOfBirth, eps.trim(), trimmedIdentityNumber, identityDocumentType,
                bloodType, trimNullable(phone),
                trimNullable(tutorFirstName), trimNullable(tutorLastName),
                trimNullable(tutorRelationship), trimNullable(tutorPhone), trimNullable(tutorEmail),
                STATUS_ACTIVE, now, createdBy, null, null, null, null
        );

        student.domainEvents.add(new StudentCreated(
                id.value(), tenantId, normalizedFirstName, normalizedLastName,
                normalizedEmail, createdBy, now));

        return student;
    }

    public static Student reconstitute(StudentId id,
                                       UUID tenantId,
                                       String firstName,
                                       String lastName,
                                       String email,
                                       LocalDate dateOfBirth,
                                       String eps,
                                       String identityNumber,
                                       IdentityDocumentType identityDocumentType,
                                       BloodType bloodType,
                                       String phone,
                                       String tutorFirstName,
                                       String tutorLastName,
                                       String tutorRelationship,
                                       String tutorPhone,
                                       String tutorEmail,
                                       String status,
                                       Instant createdAt,
                                       UUID createdBy,
                                       Instant updatedAt,
                                       UUID updatedBy,
                                       Instant deactivatedAt,
                                       UUID deactivatedBy) {
        return new Student(id, tenantId, firstName, lastName, email,
                dateOfBirth, eps, identityNumber, identityDocumentType,
                bloodType, phone,
                tutorFirstName, tutorLastName, tutorRelationship, tutorPhone, tutorEmail,
                status, createdAt, createdBy, updatedAt, updatedBy, deactivatedAt, deactivatedBy);
    }

    public void update(String firstName,
                       String lastName,
                       String email,
                       LocalDate dateOfBirth,
                       String eps,
                       String identityNumber,
                       IdentityDocumentType identityDocumentType,
                       BloodType bloodType,
                       String phone,
                       String tutorFirstName,
                       String tutorLastName,
                       String tutorRelationship,
                       String tutorPhone,
                       String tutorEmail,
                       UUID updatedBy) {
        Objects.requireNonNull(updatedBy, "Updated by must not be null");
        Objects.requireNonNull(dateOfBirth, "Date of birth must not be null");
        Objects.requireNonNull(identityDocumentType, "Identity document type must not be null");
        validateNotBlank(firstName, "First name");
        validateNotBlank(lastName, "Last name");
        validateNotBlank(email, "Email");
        validateNotBlank(eps, "EPS");
        validateNotBlank(identityNumber, "Identity number");
        validateEmail(email);
        validateDateOfBirth(dateOfBirth);
        validateTutorData(dateOfBirth, tutorFirstName, tutorLastName, tutorRelationship, tutorPhone);

        this.firstName = capitalize(firstName);
        this.lastName = capitalize(lastName);
        this.email = email.trim().toLowerCase();
        this.dateOfBirth = dateOfBirth;
        this.eps = eps.trim();
        this.identityNumber = identityNumber.trim();
        this.identityDocumentType = identityDocumentType;
        this.bloodType = bloodType;
        this.phone = trimNullable(phone);
        this.tutorFirstName = trimNullable(tutorFirstName);
        this.tutorLastName = trimNullable(tutorLastName);
        this.tutorRelationship = trimNullable(tutorRelationship);
        this.tutorPhone = trimNullable(tutorPhone);
        this.tutorEmail = trimNullable(tutorEmail);
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;

        domainEvents.add(new StudentUpdated(
                id.value(), tenantId, this.firstName, this.lastName, this.email,
                updatedBy, this.updatedAt));
    }

    public void deactivate(UUID deactivatedBy) {
        Objects.requireNonNull(deactivatedBy, "Deactivated by must not be null");
        if (STATUS_INACTIVE.equals(this.status)) {
            throw new IllegalStateException("Student is already inactive");
        }

        Instant now = Instant.now();
        this.status = STATUS_INACTIVE;
        this.deactivatedAt = now;
        this.deactivatedBy = deactivatedBy;
        this.updatedAt = now;
        this.updatedBy = deactivatedBy;

        domainEvents.add(new StudentDeactivated(id.value(), tenantId, deactivatedBy, now));
    }

    public void reactivate(UUID reactivatedBy) {
        Objects.requireNonNull(reactivatedBy, "Reactivated by must not be null");
        if (!STATUS_INACTIVE.equals(this.status)) {
            throw new IllegalStateException("Student is not inactive");
        }

        Instant now = Instant.now();
        this.status = STATUS_ACTIVE;
        this.deactivatedAt = null;
        this.deactivatedBy = null;
        this.updatedAt = now;
        this.updatedBy = reactivatedBy;

        domainEvents.add(new StudentReactivated(id.value(), tenantId, reactivatedBy, now));
    }

    public int calculateAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isMinor() {
        return calculateAge() < 18;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public StudentId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getEps() { return eps; }
    public String getIdentityNumber() { return identityNumber; }
    public IdentityDocumentType getIdentityDocumentType() { return identityDocumentType; }
    public BloodType getBloodType() { return bloodType; }
    public String getPhone() { return phone; }
    public String getTutorFirstName() { return tutorFirstName; }
    public String getTutorLastName() { return tutorLastName; }
    public String getTutorRelationship() { return tutorRelationship; }
    public String getTutorPhone() { return tutorPhone; }
    public String getTutorEmail() { return tutorEmail; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public UUID getDeactivatedBy() { return deactivatedBy; }

    private static String capitalize(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return trimmed;
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static String trimNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }

    private static void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }

    private static void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
    }

    private static void validateTutorData(LocalDate dateOfBirth,
                                          String tutorFirstName,
                                          String tutorLastName,
                                          String tutorRelationship,
                                          String tutorPhone) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 18) {
            if (tutorFirstName == null || tutorFirstName.isBlank()) {
                throw new IllegalArgumentException("Tutor first name is required for minors");
            }
            if (tutorLastName == null || tutorLastName.isBlank()) {
                throw new IllegalArgumentException("Tutor last name is required for minors");
            }
            if (tutorRelationship == null || tutorRelationship.isBlank()) {
                throw new IllegalArgumentException("Tutor relationship is required for minors");
            }
            if (tutorPhone == null || tutorPhone.isBlank()) {
                throw new IllegalArgumentException("Tutor phone is required for minors");
            }
        }
    }
}
