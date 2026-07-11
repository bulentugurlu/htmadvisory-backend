package org.htmadvisory.platform.audit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AuditRequest {

    @NotBlank
    private String url;

    private String companyName;

    private String requestedByEmail;

    @Size(min = 1)
    private List<String> auditTypes;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getRequestedByEmail() { return requestedByEmail; }
    public void setRequestedByEmail(String requestedByEmail) { this.requestedByEmail = requestedByEmail; }
    public List<String> getAuditTypes() { return auditTypes; }
    public void setAuditTypes(List<String> auditTypes) { this.auditTypes = auditTypes; }
}
