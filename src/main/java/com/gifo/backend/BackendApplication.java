package com.gifo.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

	private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		log.info("[Timezone] 기본 타임존: {}", TimeZone.getDefault().getID());
		log.info("[Timezone] 현재 서버 시각: {}", LocalDateTime.now());
	}

}
