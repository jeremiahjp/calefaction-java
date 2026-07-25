package com.calefaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import net.dv8tion.jda.api.JDA;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @MockitoBean
    private JDA jda; // Mock JDA so we don't need a real token for tests

    @Test
    void contextLoads() {
    }
}
