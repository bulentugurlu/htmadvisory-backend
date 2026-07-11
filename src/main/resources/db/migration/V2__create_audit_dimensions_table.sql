CREATE TABLE audit_dimensions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id        UUID NOT NULL REFERENCES audits(id) ON DELETE CASCADE,
    audit_type      VARCHAR(50) NOT NULL,
    dimension_name  VARCHAR(255) NOT NULL,
    score           INTEGER NOT NULL,
    grade           VARCHAR(2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX idx_audit_dimensions_audit_id ON audit_dimensions(audit_id);
