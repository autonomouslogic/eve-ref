package com.autonomouslogic.everef.dataserver;

import com.autonomouslogic.everef.config.Configs;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

/**
 * Cuts off response streams that have made no write progress for {@code DATA_SERVER_WRITE_STALL_TIMEOUT}, so a
 * client that stops reading can't hold a virtual thread, buffers and an upstream connection forever. A slow
 * client that keeps reading, however long the download takes, is never cut off: only lack of progress counts.
 * See data-server-plan.md step 12a.
 */
@Singleton
@Log4j2
public class StallWatchdog {
	private final long timeoutNanos;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		var t = new Thread(r, "data-server-stall-watchdog");
		t.setDaemon(true);
		return t;
	});
	private final ConcurrentHashMap<Long, StreamState> streams = new ConcurrentHashMap<>();
	private final AtomicLong idGenerator = new AtomicLong();

	@Inject
	protected StallWatchdog() {
		this(Configs.DATA_SERVER_WRITE_STALL_TIMEOUT.getRequired());
	}

	protected StallWatchdog(java.time.Duration timeout) {
		this.timeoutNanos = timeout.toNanos();
		scheduler.scheduleAtFixedRate(this::checkStreams, 1, 1, TimeUnit.SECONDS);
	}

	public long register(Thread handlerThread) {
		var id = idGenerator.incrementAndGet();
		streams.put(id, new StreamState(handlerThread));
		return id;
	}

	public void progress(long id) {
		var state = streams.get(id);
		if (state != null) {
			state.lastProgressNanos = System.nanoTime();
		}
	}

	public void unregister(long id) {
		streams.remove(id);
	}

	public int activeStreamCount() {
		return streams.size();
	}

	public void stop() {
		scheduler.shutdownNow();
	}

	private void checkStreams() {
		var cutoff = System.nanoTime() - timeoutNanos;
		streams.forEach((id, state) -> {
			if (state.lastProgressNanos < cutoff) {
				log.warn("Stream {} stalled, interrupting handler thread", id);
				state.handlerThread.interrupt();
			}
		});
	}

	private static class StreamState {
		final Thread handlerThread;
		volatile long lastProgressNanos;

		StreamState(Thread handlerThread) {
			this.handlerThread = handlerThread;
			this.lastProgressNanos = System.nanoTime();
		}
	}
}
