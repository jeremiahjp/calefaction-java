package com.jp.calefaction.components;

import com.jp.calefaction.model.ai.ChatCompletionResponse;
import java.text.DecimalFormat;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class APICostCalculator {
    // GPT rates per 1k tokens
    private static final double GPT_INPUT_RATE = 0.01; // $0.01 per 1k tokens
    private static final double GPT_OUTPUT_RATE = 0.03; // $0.03 per 1k tokens

    // Grok rates per 1k tokens (converted from per million)
    private static final double GROK_INPUT_RATE = 0.0003; // $0.30 per million tokens = $0.0003 per 1k tokens
    private static final double GROK_OUTPUT_RATE = 0.0005; // $0.50 per million tokens = $0.0005 per 1k tokens

    public static double calculateCost(String jsonResponse) {
        JSONObject response = new JSONObject(jsonResponse);
        JSONObject usage = response.getJSONObject("usage");
        int promptTokens = usage.getInt("prompt_tokens");
        int completionTokens = usage.getInt("completion_tokens");
        String model = response.getString("model");

        // Select rates based on model
        double inputRate = isGrokModel(model) ? GROK_INPUT_RATE : GPT_INPUT_RATE;
        double outputRate = isGrokModel(model) ? GROK_OUTPUT_RATE : GPT_OUTPUT_RATE;

        // Calculate cost
        double inputCost = (promptTokens / 1000.0) * inputRate;
        double outputCost = (completionTokens / 1000.0) * outputRate;

        // Total cost
        return inputCost + outputCost;
    }

    public static double calculateCost(ChatCompletionResponse response) {
        int promptTokens = response.getUsage().getPrompt_tokens();
        int completionTokens = response.getUsage().getCompletion_tokens();

        // Select rates based on model
        double inputRate = isGrokModel(response.getModel()) ? GROK_INPUT_RATE : GPT_INPUT_RATE;
        double outputRate = isGrokModel(response.getModel()) ? GROK_OUTPUT_RATE : GPT_OUTPUT_RATE;

        // Calculate cost
        double inputCost = (promptTokens / 1000.0) * inputRate;
        double outputCost = (completionTokens / 1000.0) * outputRate;

        // Total cost
        return inputCost + outputCost;
    }

    private static boolean isGrokModel(String model) {
        return model != null && model.toLowerCase().contains("grok");
    }

    public static String getFormattedCost(double cost) {
        DecimalFormat df = new DecimalFormat("$0.00000000");
        return df.format(cost);
    }

    public static String getFormattedCost(ChatCompletionResponse response) {
        double cost = calculateCost(response);
        return getFormattedCost(cost);
    }
}
