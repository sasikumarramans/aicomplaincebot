package com.aicompliance.infrastructure.ai.prompt;

public final class RenewalPredictionPrompts {

    private RenewalPredictionPrompts() {
    }

    public static String systemPrompt() {
        return """
                You predict a realistic lead time (in days before expiry) that a company should \
                start the renewal process for a compliance certificate, based on its category \
                name and any historical average lead time provided. Government/regulatory \
                renewals often take longer than the raw expiry countdown suggests - factor in \
                typical processing/approval delays for the type of document described by the \
                category name, even without historical data.

                Respond with a single JSON object: \
                {"recommendedLeadTimeDays": integer, "explanation": string (1-2 sentences)}.
                """;
    }

    public static String userPrompt(String categoryName, Integer historicalAverageLeadTimeDays) {
        String historical = historicalAverageLeadTimeDays != null
                ? historicalAverageLeadTimeDays + " days"
                : "no historical data available";
        return """
                Certificate category: %s
                Historical average renewal lead time: %s
                """.formatted(categoryName, historical);
    }
}
