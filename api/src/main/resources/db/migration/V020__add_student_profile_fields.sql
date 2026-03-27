ALTER TABLE students
    ADD COLUMN date_of_birth          DATE         NOT NULL DEFAULT '2000-01-01',
    ADD COLUMN eps                    VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN identity_number        VARCHAR(30)  NOT NULL DEFAULT '',
    ADD COLUMN identity_document_type VARCHAR(5)   NOT NULL DEFAULT 'CC',
    ADD COLUMN blood_type             VARCHAR(5),
    ADD COLUMN phone                  VARCHAR(20),
    ADD COLUMN tutor_first_name       VARCHAR(100),
    ADD COLUMN tutor_last_name        VARCHAR(100),
    ADD COLUMN tutor_relationship     VARCHAR(50),
    ADD COLUMN tutor_phone            VARCHAR(20),
    ADD COLUMN tutor_email            VARCHAR(255);

-- Remove defaults after adding (they were just for the ALTER on existing rows)
ALTER TABLE students ALTER COLUMN date_of_birth DROP DEFAULT;
ALTER TABLE students ALTER COLUMN eps DROP DEFAULT;
ALTER TABLE students ALTER COLUMN identity_number DROP DEFAULT;
ALTER TABLE students ALTER COLUMN identity_document_type DROP DEFAULT;

-- Unique identity number per tenant
CREATE UNIQUE INDEX idx_students_tenant_identity ON students(tenant_id, identity_number);

-- Constraints
ALTER TABLE students ADD CONSTRAINT chk_student_document_type
    CHECK (identity_document_type IN ('CC', 'TI', 'CE', 'PA', 'RC'));

ALTER TABLE students ADD CONSTRAINT chk_student_blood_type
    CHECK (blood_type IS NULL OR blood_type IN ('O+', 'O-', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-'));

-- Index for search by identity number
CREATE INDEX idx_students_identity_number ON students(identity_number);
