package com.aicompliance.infrastructure.ai.prompt;

public final class ComplianceChatPrompts {

    private ComplianceChatPrompts() {
    }

    public static String systemPrompt() {
        return """
                You are a compliance assistant for a platform used across many industries \
                (manufacturing, software, healthcare, and others). Answer the user's question \
                using ONLY the certificate data provided below - do not invent certificates, \
                dates, or statuses that are not present in the data. If the data doesn't contain \
                enough information to answer, say so plainly rather than guessing.

                Respond with a single JSON object: {"answer": string}. The answer should be a \
                clear, concise natural-language response (plain text, not JSON, inside the \
                answer field), referencing specific certificate names/dates/statuses from the \
                data when relevant.
                """;
    }

    public static String userPrompt(String question, String certificateDataJson) {
        return """
                Company's certificate data (JSON array):
                %s

                Question: %s
                """.formatted(certificateDataJson, question);
    }
}
