package com.joshlong.jobs.watchdog

import org.springframework.context.ApplicationContext
import org.springframework.context.event.ApplicationContextEvent


/**
 * Signals that a given JVM process should be maintained. This ultimately calls
 * {@link com.joshlong.twitter.InactivityMonitor#watch()} which has a daemon process running.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
class HeartbeatEvent(source: ApplicationContext) : ApplicationContextEvent(source)