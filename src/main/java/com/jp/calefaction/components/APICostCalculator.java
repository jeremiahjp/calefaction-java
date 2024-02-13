package com.jp.calefaction.components;

import com.jp.calefaction.model.ai.ChatCompletionResponse;
import java.text.DecimalFormat;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class APICostCalculator {

    public static double calculateCost(String jsonResponse) {
        JSONObject response = new JSONObject(jsonResponse);
        JSONObject usage = response.getJSONObject("usage");
        int promptTokens = usage.getInt("prompt_tokens");
        int completionTokens = usage.getInt("completion_tokens");

        // Cost rates per 1k tokens
        double costPerThousandInputTokens = 0.01;
        double costPerThousandOutputTokens = 0.03;

        // Calculate cost
        double inputCost = (promptTokens / 1000.0) * costPerThousandInputTokens;
        double outputCost = (completionTokens / 1000.0) * costPerThousandOutputTokens;

        // Total cost
        return inputCost + outputCost;
    }

    public static double calculateCost(ChatCompletionResponse response) {
        int promptTokens = response.getUsage().getPrompt_tokens();
        int completionTokens = response.getUsage().getCompletion_tokens();

        // Cost rates per 1k tokens
        double costPerThousandInputTokens = 0.01;
        double costPerThousandOutputTokens = 0.03;

        // Calculate cost
        double inputCost = (promptTokens / 1000.0) * costPerThousandInputTokens;
        double outputCost = (completionTokens / 1000.0) * costPerThousandOutputTokens;

        // Total cost
        return inputCost + outputCost;
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
