package org.htmadvisory.platform.audit.auditors;

public record FindingResult(String severity, String finding) {

    public static FindingResult high(String finding) {
        return new FindingResult("HIGH", finding);
    }

    public static FindingResult medium(String finding) {
        return new FindingResult("MEDIUM", finding);
    }

    public static FindingResult low(String finding) {
        return new FindingResult("LOW", finding);
    }
}
