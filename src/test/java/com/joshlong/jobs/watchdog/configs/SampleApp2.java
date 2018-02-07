package com.joshlong.jobs.watchdog.configs;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableAutoConfiguration
@Configuration
public class SampleApp2 {

	@Bean
	public Executor te1() {
		return Executors.newSingleThreadExecutor();
	}

	@Bean
	@Primary
	public Executor te2() {
		return Executors.newCachedThreadPool();
	}
}