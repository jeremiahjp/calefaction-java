package com.jp.calefaction.service;

import com.jp.calefaction.entity.OriginalMessages;
import com.jp.calefaction.entity.Users;
import com.jp.calefaction.repository.OriginalMessagesRepository;
import com.jp.calefaction.repository.UsersRepository;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RepostService {

    private final UsersRepository usersRepository;
    private final OriginalMessagesRepository originalMessagesRepository;

    public Optional<OriginalMessages> getMessage(String urlKey) {
        return originalMessagesRepository.findById(urlKey);
    }

    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }

    public OriginalMessages getByIdAndGuild(String urlKey, String guildId) {
        return originalMessagesRepository.findByUrlKeyAndGuildId(urlKey, guildId);
    }

    public String extractVideoId(String url) {
        // 1. Extract after "v="
        Pattern pattern = Pattern.compile("v=([a-zA-Z0-9_-]{11})");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 2. Extract after "youtu.be/"
        pattern = Pattern.compile("youtu\\.be\\/([a-zA-Z0-9_-]{11})");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 3. Extract after "/v/"
        pattern = Pattern.compile("youtube\\.com/v/([a-zA-Z0-9_-]{11})");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 4. Extract after "embed/"
        pattern = Pattern.compile("/embed/([a-zA-Z0-9_-]{11})");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 5. Extract from YouTube short links
        pattern = Pattern.compile("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null; // Return null if no match is found
    }
}
