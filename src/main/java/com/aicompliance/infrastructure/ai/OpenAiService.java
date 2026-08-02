package com.aicompliance.infrastructure.ai;

import com.aicompliance.application.port.AiService;
import com.aicompliance.infrastructure.ai.prompt.CertificateExtractionPrompts;
import com.aicompliance.infrastructure.ai.prompt.ComplianceChatPrompts;
import com.aicompliance.infrastructure.ai.prompt.ComplianceScorePrompts;
import com.aicompliance.infrastructure.ai.prompt.DuplicateDetectionPrompts;
import com.aicompliance.infrastructure.ai.prompt.RenewalPredictionPrompts;
import com.aicompliance.infrastructure.ai.prompt.RiskAssessmentPrompts;
import com.aicompliance.infrastructure.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService implements AiService {

    private final OpenAiClient openAiClient;
    private final String extractionModel;
    private final String chatModel;

    public OpenAiService(OpenAiClient openAiClient, AppProperties appProperties) {
        this.openAiClient = openAiClient;
        this.extractionModel = appProperties.getOpenai().getExtractionModel();
        this.chatModel = appProperties.getOpenai().getChatModel();
    }

    @Override
    public CertificateFieldExtraction extractCertificateFields(String ocrText, List<String> existingCategoryNames) {
        JsonNode result = openAiClient.completeAsJson(
                extractionModel,
                CertificateExtractionPrompts.systemPrompt(),
                CertificateExtractionPrompts.userPrompt(ocrText, existingCategoryNames));

        return new CertificateFieldExtraction(
                textOrNull(result, "certificateName"),
                textOrNull(result, "certificateNumber"),
                textOrNull(result, "issuingAuthority"),
                dateOrNull(result, "issueDate"),
                dateOrNull(result, "expiryDate"),
                textOrNull(result, "licenseNumber"),
                result.path("hasQrCode").asBoolean(false),
                result.path("hasDigitalSignature").asBoolean(false),
                textOrNull(result, "suggestedCategoryName"),
                result.path("confidenceScore").asDouble(0.0));
    }

    @Override
    public String answerComplianceQuestion(String question, String certificateDataJson) {
        JsonNode result = openAiClient.completeAsJson(
                chatModel,
                ComplianceChatPrompts.systemPrompt(),
                ComplianceChatPrompts.userPrompt(question, certificateDataJson));
        String answer = textOrNull(result, "answer");
        return answer != null ? answer : "I wasn't able to generate an answer from the available data.";
    }

    @Override
    public String explainComplianceScore(String scoreBreakdownJson) {
        JsonNode result = openAiClient.completeAsJson(
                chatModel,
                ComplianceScorePrompts.systemPrompt(),
                ComplianceScorePrompts.userPrompt(scoreBreakdownJson));
        String explanation = textOrNull(result, "explanation");
        return explanation != null ? explanation : "No explanation available.";
    }

    @Override
    public String explainRiskSignals(String riskSignalsJson) {
        JsonNode result = openAiClient.completeAsJson(
                chatModel,
                RiskAssessmentPrompts.systemPrompt(),
                RiskAssessmentPrompts.userPrompt(riskSignalsJson));
        String explanation = textOrNull(result, "explanation");
        return explanation != null ? explanation : "No risk explanation available.";
    }

    @Override
    public List<Integer> confirmDuplicateIndices(String candidatePairsJson) {
        JsonNode result = openAiClient.completeAsJson(
                chatModel,
                DuplicateDetectionPrompts.systemPrompt(),
                DuplicateDetectionPrompts.userPrompt(candidatePairsJson));
        List<Integer> indices = new ArrayList<>();
        JsonNode array = result.get("duplicatePairIndices");
        if (array != null && array.isArray()) {
            array.forEach(node -> indices.add(node.asInt()));
        }
        return indices;
    }

    @Override
    public RenewalPrediction predictRenewalLeadTime(String categoryName, Integer historicalAverageLeadTimeDays) {
        JsonNode result = openAiClient.completeAsJson(
                chatModel,
                RenewalPredictionPrompts.systemPrompt(),
                RenewalPredictionPrompts.userPrompt(categoryName, historicalAverageLeadTimeDays));
        int leadTime = result.path("recommendedLeadTimeDays").asInt(30);
        String explanation = textOrNull(result, "explanation");
        return new RenewalPrediction(leadTime, explanation != null ? explanation : "");
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private LocalDate dateOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
