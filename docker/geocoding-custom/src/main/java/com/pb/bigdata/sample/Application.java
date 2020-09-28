package com.pb.bigdata.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	public static void main(final String[] args) {
		new SpringApplication(Application.class).run(args);
	}
}