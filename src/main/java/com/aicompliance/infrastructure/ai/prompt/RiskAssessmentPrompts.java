package com.aicompliance.infrastructure.ai.prompt;

public final class RiskAssessmentPrompts {

    private RiskAssessmentPrompts() {
    }

    public static String systemPrompt() {
        return """
                You are a compliance risk analyst. You are given a list of rule-based risk \
                signals already computed for a company (e.g. overlapping certificate expiries, \
                overdue inspections, expired certificates). Write a short (2-4 sentence) \
                plain-text narrative explaining the overall risk picture and what to prioritize. \
                Do not invent signals not present in the data.

                Respond with a single JSON object: {"explanation": string}.
                """;
    }

    public static String userPrompt(String riskSignalsJson) {
        return """
                Risk signals (JSON):
                %s
                """.formatted(riskSignalsJson);
    }
}
