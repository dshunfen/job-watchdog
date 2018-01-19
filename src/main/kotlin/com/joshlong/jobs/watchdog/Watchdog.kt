package com.joshlong.jobs.watchdog

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.event.EventListener
import org.springframework.context.support.GenericApplicationContext
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

/**
 * Starts a monitor thread that, when it detects that there have not been any recent renewal events,
 * shuts down the {@link GenericApplicationContext} allowing the JVM to exit gracefully.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Component
class Watchdog(
		val watchdogProperties: WatchdogProperties,
		val executor: Executor,
		val applicationContext: GenericApplicationContext) : InitializingBean {

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
	 * TODO qualified with a custom qualifier annotation (@WatchdogDispose?)
	 * TODO to customize shutdown behavior.
	*/
	fun stop() {
		synchronized(this.applicationContext) {
			this.log.debug("There has been ${window}s of inactivity. " +
					"Calling ${applicationContext.javaClass.name}#close()")
			this.applicationContext.close()
		}
	}

	fun watch() {
		this.lastTick.set(System.currentTimeMillis())
	}

	override fun afterPropertiesSet() {
		this.executor.execute({
			this.log.debug("Starting ${javaClass.name} thread.")
			while (true) {
				// sleep a number of seconds
				Thread.sleep(this.watchdogProperties.inactivityHeartbeatInSeconds * 1000)
				val now = System.currentTimeMillis()
				val then = this.lastTick.get()
				val diff = now - then
				// test that we haven't slept $window seconds without getting a renewal.
				// If we have, then close the application context.
				if (diff > window) {
					stop()
					break
				}
			}
			this.log.debug("Finishing ${javaClass.name} thread.")
		})
	}
}