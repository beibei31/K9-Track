package com.xue.k9track;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xue.k9track.mapper")
public class K9TrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(K9TrackApplication.class, args);
    }
}
