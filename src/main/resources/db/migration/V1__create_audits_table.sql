CREATE TABLE audits (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url                 VARCHAR(2048) NOT NULL,
    company_name        VARCHAR(255),
    requested_by_email  VARCHAR(255),
    person_id           VARCHAR(255),
    overall_score       INTEGER,
    overall_grade       VARCHAR(2),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audit_types         TEXT[],
    claude_summary      TEXT,
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at        TIMESTAMP WITH TIME ZONE
);
