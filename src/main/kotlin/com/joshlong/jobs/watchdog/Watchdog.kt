package com.joshlong.jobs.watchdog

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.event.EventListener
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * Starts a monitor thread that, when it detects that there have not been any recent renewal events,
 * shuts down the {@link GenericApplicationContext} allowing the JVM to exit gracefully.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Component
class Watchdog(
		private val watchdogProperties: WatchdogProperties,
		private val executor: TaskExecutor,
		private val applicationContext: GenericApplicationContext) : InitializingBean {

	private val window = Duration.ofSeconds(
			watchdogProperties.inactivityThresholdInSeconds).toMillis()

	private val log = LogFactory.getLog(javaClass)

	private val lastTick = AtomicLong(System.currentTimeMillis())

	@EventListener(HeartbeatEvent::class)
	fun onHeartbeatEvent(hbe: HeartbeatEvent) {
		this.watch()
	}

	/*
	 * TODO support contributing beans of well-known types that are
	  * qualified with a custom qualifier annotation (@WatchdogDispose?) to customize shutdown behavior.
	 */
	fun stop() {
		this.applicationContext.close()
	}

	fun watch() {
		this.lastTick.set(System.currentTimeMillis())
	}

	override fun afterPropertiesSet() {
		this.executor.execute({
			this.log.debug("About to begin ${javaClass.name} thread.")
			while (true) {
				// sleep a number of seconds
				Thread.sleep(this.watchdogProperties.inactivityHeartbeatInSeconds * 1000)
				val now = System.currentTimeMillis()
				val then = this.lastTick.get()
				val diff = now - then
				// test that we haven't slept $window seconds without getting a renewal.
				// If we have, then close the application context.
				if (diff > window) {
					this.log.debug("There has been ${window}s of inactivity. " +
							"Calling ${applicationContext.javaClass.name}#close()")
					stop()
				}
			}
		})
	}
}