package com.joshlong.jobs.watchdog;

import static org.mockito.Mockito.times;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.joshlong.jobs.watchdog.configs.SampleApp1;
import com.joshlong.jobs.watchdog.configs.SampleApp2;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Component
public class WatchdogTest {
	
	private int window;
	private Executor executor;
	private GenericApplicationContext applicationContext;
	private WatchdogProperties watchdogProperties;
	private Watchdog watchdog;

	@Before
	public void init() {
		window = 5;
		executor = new SimpleAsyncTaskExecutor();
		applicationContext = Mockito.mock(GenericApplicationContext.class);
		watchdogProperties = new WatchdogProperties();
		watchdogProperties.setInactivityThresholdInSeconds(window - 1L);
		watchdog = new Watchdog(watchdogProperties, executor, applicationContext);
	}
	
	@Test
	public void watchAndStopAfterInactivity() {
		long start = System.currentTimeMillis();
		this.watchdog.afterPropertiesSet();
		int renewals = 3;
		IntStream.range(0, renewals).forEach($ -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			this.watchdog.watch();
		});
		try {
			Thread.sleep(window * 1000L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		Mockito.verify(this.applicationContext, times(1)).close();
		Assertions.assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo((window + renewals) * 1000L);
	}

	@Test
	public void configWithContextProvidedExecutor() {
		ConfigurableApplicationContext ac = new SpringApplicationBuilder()
				.sources(SampleApp1.class)
				.run();
		Executor executor = ac.getBean("testTaskExecutor", Executor.class);
		Assertions.assertThat(executor);
		Watchdog wd = ac.getBean(Watchdog.class);
		Assertions.assertThat(Whitebox.getInternalState(wd, "executor") == executor);
		ac.close();
	}

	@Test
	public void configWithMultipleExecutors() {
		ConfigurableApplicationContext ac = new SpringApplicationBuilder()
				.sources(SampleApp2.class)
				.run();
		Map<String, Executor> executors = ac.getBeansOfType(Executor.class);
		Watchdog wd = ac.getBean(Watchdog.class);
		Assertions.assertThat(executors.values().contains(Whitebox.getInternalState(wd, "executor"))).isTrue();
		ac.close();
	}
}