package com.jp.calefaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.jp.calefaction.repository.OriginalMessagesRepository;
import com.jp.calefaction.repository.UsersRepository;

public class TestRepostService {

    @Mock
    private UsersRepository usersRepository; 
    @Mock
    private OriginalMessagesRepository originalMessagesRepository;


    @Test
    void testLongUrlDefault() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "https://www.youtube.com/watch?v=GA3-AhAMS8U";
        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void testMobileUrl() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "https://m.youtube.com/v/GA3-AhAMS8U";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void testLongUrlWithV() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "https://youtube.com/v/GA3-AhAMS8U?version=3&autohide=1";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void testLongUrlWithEmbed() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "https://www.youtube.com/embed/GA3-AhAMS8U";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }


    @Test
    void testLongUrlWithFeatureEmbed() {
        RepostService repostService = new RepostService(usersRepository,originalMessagesRepository);
        String testUrl = "https://www.youtube.com/watch?feature=player_embedded&v=GA3-AhAMS8U";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void testShortUrl() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "https://youtu.be/GA3-AhAMS8U";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void contextWithUrl() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "check this out https://youtu.be/GA3-AhAMS8U";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void contextAfterUrl() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "https://youtu.be/GA3-AhAMS8U check this out ";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }

    @Test
    void contextThatIncludesMatchersRandomly() {
        RepostService repostService = new RepostService(usersRepository, originalMessagesRepository);
        String testUrl = "this is an accident i v/ery didnt mean to https://youtu.be/GA3-AhAMS8U";

        String expected = "GA3-AhAMS8U";
        String actual = repostService.extractVideoId(testUrl);
        assertEquals(expected, actual);
    }
}
