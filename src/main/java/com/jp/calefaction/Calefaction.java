package com.jp.calefaction;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Calefaction {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Calefaction.class).build().run(args);
    }
}
