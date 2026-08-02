package com.aicompliance.application.port;

import java.util.Map;

public interface PdfReportGenerator {

    /**
     * Renders {@code templateName} (a Thymeleaf template under classpath:/templates/reports/)
     * with the given model, then converts the result to PDF bytes.
     */
    byte[] generate(String templateName, Map<String, Object> model);
}
