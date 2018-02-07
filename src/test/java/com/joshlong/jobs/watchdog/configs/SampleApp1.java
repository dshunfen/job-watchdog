package com.joshlong.jobs.watchdog.configs;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@Configuration
public class SampleApp1 {

	@Bean("testTaskExecutor")
	public Executor executor() {
		return Executors.newSingleThreadExecutor();
	}
}