package com.jp.calefaction.model.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ImageCreationRequest {
    private String prompt; // Required
    private String model; // Optional, defaults to "dall-e-2"
    private Integer n; // Optional, defaults to 1
    private String quality; // Optional, defaults to "standard"
    private String responseFormat; // Optional, defaults to "url"
    private String size; // Optional, defaults to "1024x1024"
    private String style; // Optional, defaults to "vivid"
    private String user; // Optional
}
