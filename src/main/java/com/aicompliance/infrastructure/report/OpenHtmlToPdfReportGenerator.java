package com.aicompliance.infrastructure.report;

import com.aicompliance.application.port.PdfReportGenerator;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class OpenHtmlToPdfReportGenerator implements PdfReportGenerator {

    private final TemplateEngine reportTemplateEngine;

    public OpenHtmlToPdfReportGenerator(TemplateEngine reportTemplateEngine) {
        this.reportTemplateEngine = reportTemplateEngine;
    }

    @Override
    public byte[] generate(String templateName, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        String html = reportTemplateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render PDF report: " + templateName, e);
        }
    }
}
