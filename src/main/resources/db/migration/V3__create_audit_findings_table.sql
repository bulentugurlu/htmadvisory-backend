CREATE TABLE audit_findings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dimension_id        UUID NOT NULL REFERENCES audit_dimensions(id) ON DELETE CASCADE,
    severity            VARCHAR(10) NOT NULL,
    finding             TEXT NOT NULL,
    recommendation      TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX idx_audit_findings_dimension_id ON audit_findings(dimension_id);
