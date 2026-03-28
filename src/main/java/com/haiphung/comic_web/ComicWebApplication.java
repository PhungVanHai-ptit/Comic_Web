package com.haiphung.comic_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ComicWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComicWebApplication.class, args);
	}

}