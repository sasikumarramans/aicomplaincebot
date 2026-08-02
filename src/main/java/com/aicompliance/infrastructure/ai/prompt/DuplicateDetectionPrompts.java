package com.aicompliance.infrastructure.ai.prompt;

public final class DuplicateDetectionPrompts {

    private DuplicateDetectionPrompts() {
    }

    public static String systemPrompt() {
        return """
                You are given a list of candidate certificate pairs that a rule-based filter has \
                already flagged as POSSIBLE near-duplicates (same category, similar certificate \
                number). For each pair, decide whether they really are the same underlying \
                certificate (e.g. a typo'd re-upload) or genuinely different certificates that \
                happen to look similar.

                Respond with a single JSON object: {"duplicatePairIndices": [array of integers]} \
                - the 0-based indices (from the input array) of pairs you confirm ARE duplicates. \
                Omit indices for pairs you believe are NOT duplicates.
                """;
    }

    public static String userPrompt(String candidatePairsJson) {
        return """
                Candidate pairs (JSON array, each element is a pair to evaluate):
                %s
                """.formatted(candidatePairsJson);
    }
}
