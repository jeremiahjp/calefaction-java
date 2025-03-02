package com.jp.calefaction.service;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TextGenService {

    private static final Map<Character, String> emojiMap = new HashMap<>();

    static {
        for (char c = 'a'; c <= 'z'; c++) {
            emojiMap.put(c, ":regional_indicator_" + c + ":");
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            emojiMap.put(c, ":regional_indicator_" + Character.toLowerCase(c) + ":");
        }
        emojiMap.put('0', "0️⃣");
        emojiMap.put('1', "1️⃣");
        emojiMap.put('2', "2️⃣");
        emojiMap.put('3', "3️⃣");
        emojiMap.put('4', "4️⃣");
        emojiMap.put('5', "5️⃣");
        emojiMap.put('6', "6️⃣");
        emojiMap.put('7', "7️⃣");
        emojiMap.put('8', "8️⃣");
        emojiMap.put('9', "9️⃣");
        emojiMap.put(' ', "   "); // Preserve spaces
        emojiMap.put('!', "❗");
        emojiMap.put('?', "❓");
        emojiMap.put('#', "#️⃣");
        emojiMap.put('*', "*️⃣");
    }

    public String translateToEmoji(String text) {
        StringBuilder translatedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            translatedText.append(emojiMap.getOrDefault(c, "")).append(" "); // Space for readability
        }
        return translatedText.toString().trim();
    }
}
