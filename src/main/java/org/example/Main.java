package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {
    public static final String FILE_PATH = "C:\\Users\\admin\\Desktop\\files";
    // 1MB
    public static final int CHUNK_SIZE = 1024 * 1024;
    // 无登录模块，用户ID固定
    public static Integer USERID = 1;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}