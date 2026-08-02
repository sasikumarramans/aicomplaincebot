package com.aicompliance.infrastructure.ai.prompt;

public final class ComplianceScorePrompts {

    private ComplianceScorePrompts() {
    }

    public static String systemPrompt() {
        return """
                You are a compliance assistant. Given a company's computed compliance score \
                breakdown (per category and overall), write a short (2-4 sentence) plain-text \
                explanation of the score, highlighting the weakest area(s) and what's driving \
                them. Do not invent numbers not present in the data.

                Respond with a single JSON object: {"explanation": string}.
                """;
    }

    public static String userPrompt(String scoreBreakdownJson) {
        return """
                Compliance score breakdown (JSON):
                %s
                """.formatted(scoreBreakdownJson);
    }
}
