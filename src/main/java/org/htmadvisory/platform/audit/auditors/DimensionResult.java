package org.htmadvisory.platform.audit.auditors;

import java.util.List;

public record DimensionResult(String dimensionName, String auditType, int score, List<FindingResult> findings) {

    public String grade() {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
