package com.joshlong.jobs.watchdog;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Configuration
@EnableConfigurationProperties(WatchdogProperties.class)
class WatchdogAutoConfiguration {

	@Bean
	public Executor executor() {
		return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@Bean
	@ConditionalOnMissingBean
	public Watchdog watchdog(WatchdogProperties properties, GenericApplicationContext applicationContext) {
		return new Watchdog(properties, this.executor(), applicationContext);
	}
}