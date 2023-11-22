package com.jp.calefaction.model.dogapi;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DogDto {
    private String id;
    private String url;
    private int width;
    private int height;
    private List breeds;
    private Map<String, String> favourite;
}
