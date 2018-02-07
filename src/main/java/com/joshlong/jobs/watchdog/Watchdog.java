package com.joshlong.jobs.watchdog;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Starts a monitor thread that, when it detects that there have not been any
 * recent renewal events, shuts down the {@link GenericApplicationContext}
 * allowing the JVM to exit gracefully.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Component
public class Watchdog implements InitializingBean {

	private WatchdogProperties watchdogProperties;
	private Executor executor;
	private GenericApplicationContext applicationContext;

	private AtomicLong lastTick;
	private long window;

	private final Log log = LogFactory.getLog(getClass());

	public Watchdog(WatchdogProperties watchdogProperties, Executor executor,
			GenericApplicationContext applicationContext) {
		this.watchdogProperties = watchdogProperties;
		this.executor = executor;
		this.applicationContext = applicationContext;

		window = Duration.ofSeconds(watchdogProperties.getInactivityThresholdInSeconds()).toMillis();
		lastTick = new AtomicLong(System.currentTimeMillis());
	}

	@EventListener(HeartbeatEvent.class)
	public void onHeartbeatEvent(HeartbeatEvent hbe) {
		this.watch();
	}

	private void logMemory() {
		if (this.log.isDebugEnabled()) {
			Map<String, String> myMap = Stream
					.of(new SimpleEntry<>("free memory", Runtime.getRuntime().freeMemory()),
							new SimpleEntry<>("max memory", Runtime.getRuntime().maxMemory()),
							new SimpleEntry<>("total memory", Runtime.getRuntime().totalMemory()))
					.collect(Collectors.toMap(key -> key.toString(), value -> value.toString()));
			this.log.debug(myMap);
		}
	}

	public void stop() {
		this.log.debug(
				"There have been ${window}s of inactivity. " + "Calling ${applicationContext.javaClass.name}#close()");
		this.logMemory();
		this.applicationContext.close();
	}

	public void watch() {
		this.logMemory();
		this.lastTick.set(System.currentTimeMillis());
	}

	@Override
	public void afterPropertiesSet() {

		this.executor.execute(() -> {
			this.logMemory();
			this.log.debug("Starting ${javaClass.name} thread.");
			while (true) {
				// sleep a number of seconds
				// TODO - Figure out how Kotlin handles exceptions
				try {
					TimeUnit.SECONDS.sleep(this.watchdogProperties.getInactivityHeartbeatInSeconds());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				long now = System.currentTimeMillis();
				long then = this.lastTick.get();
				long diff = now - then;
				// test that we haven't slept $window seconds without getting a renewal.
				// If we have, then close the application context.
				if (diff > window) {
					stop();
					break;
				}
			}
			this.log.debug("Finishing ${javaClass.name} thread.");
		});
	}
}