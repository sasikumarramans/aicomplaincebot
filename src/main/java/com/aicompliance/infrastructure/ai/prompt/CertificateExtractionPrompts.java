package com.aicompliance.infrastructure.ai.prompt;

import java.util.List;

public final class CertificateExtractionPrompts {

    private CertificateExtractionPrompts() {
    }

    public static String systemPrompt() {
        return """
                You are a document analysis assistant for a compliance management platform used across \
                many industries (manufacturing, software, healthcare, and others). You extract structured \
                fields from the raw OCR'd text of a compliance certificate, license, or regulatory document. \
                The document could be from ANY industry - do not assume manufacturing-specific terminology.

                Respond with a single JSON object with exactly these fields:
                {
                  "certificateName": string or null,
                  "certificateNumber": string or null,
                  "issuingAuthority": string or null,
                  "issueDate": string in YYYY-MM-DD format or null,
                  "expiryDate": string in YYYY-MM-DD format or null,
                  "licenseNumber": string or null,
                  "hasQrCode": boolean,
                  "hasDigitalSignature": boolean,
                  "suggestedCategoryName": string or null,
                  "confidenceScore": number between 0 and 1
                }

                For suggestedCategoryName, prefer matching one of the company's existing category names \
                (provided below) if the document clearly fits one; otherwise suggest a short, generic \
                category name describing the document type. Set confidenceScore based on how certain you \
                are of the overall extraction, not just the category match. If a field cannot be \
                determined from the text, use null rather than guessing.
                """;
    }

    public static String userPrompt(String ocrText, List<String> existingCategoryNames) {
        String categoriesBlock = existingCategoryNames.isEmpty()
                ? "(none yet - this company has no existing categories)"
                : String.join(", ", existingCategoryNames);

        return """
                Existing categories for this company: %s

                Document text:
                ---
                %s
                ---
                """.formatted(categoriesBlock, ocrText);
    }
}
