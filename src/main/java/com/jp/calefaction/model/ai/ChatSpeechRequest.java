package com.jp.calefaction.model.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSpeechRequest {

    private String model;
    private String input;
    private String voice;
    private String responseFormat;
    private Double speed;
}
