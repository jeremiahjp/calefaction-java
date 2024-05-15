package com.jp.calefaction.model.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageCreationResponse {
    private String b64Json; // The base64-encoded JSON of the generated image
    private String url; // The URL of the generated image
    private String revisedPrompt; // The revised prompt used to generate the image
}
