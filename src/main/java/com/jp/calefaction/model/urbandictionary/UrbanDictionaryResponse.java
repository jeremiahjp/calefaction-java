package com.jp.calefaction.model.urbandictionary;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class UrbanDictionaryResponse {
    private List<Entry> list;

    @Data
    public static class Entry {
        private String definition;
        private String permalink;
        private int thumbs_up;
        private String author;
        private String word;
        private long defid;
        private String current_vote;
        private Date written_on;
        private String example;
        private int thumbs_down;
    }
}
