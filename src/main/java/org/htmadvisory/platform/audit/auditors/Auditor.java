package org.htmadvisory.platform.audit.auditors;

import org.htmadvisory.platform.audit.fetcher.PageContent;

public interface Auditor {
    String name();
    String auditType();
    DimensionResult audit(PageContent page);
}
