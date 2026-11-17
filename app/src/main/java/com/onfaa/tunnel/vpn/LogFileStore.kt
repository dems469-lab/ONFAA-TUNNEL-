package com.onfaa.tunnel.vpn

import android.content.Context
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Bounded, rotating diagnostic log file.
 *
 * Writes happen on a single background thread: the UI never blocks on disk and
 * events keep their order. The file is capped ([maxBytes]); when it is full it
 * is rotated to `.1` and a new file is started, so the total on-disk footprint
 * stays below roughly twice [maxBytes].
 */
class LogFileStore(
    context: Context,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val maxTailLines: Int = DEFAULT_TAIL_LINES
) {
    private val directory = File(context.applicationContext.filesDir, "logs")
    private val current = File(directory, FILE_NAME)
    private val rotated = File(directory, "$FILE_NAME.1")
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "onfaa-log").apply { isDaemon = true }
    }
    private val lock = Any()

    fun append(event: TunnelEvent) {
        val line = LogFormatter.fileLine(event)
        executor.execute {
            synchronized(lock) {
                runCatching {
                    directory.mkdirs()
                    if (current.exists() && current.length() >= maxBytes) {
                        rotated.delete()
                        current.renameTo(rotated)
                    }
                    current.appendText(line + "\n")
                }
            }
        }
    }

    /** Persisted tail, oldest first, read after flushing pending writes. */
    fun tail(): List<TunnelEvent> = try {
        flush()
        synchronized(lock) {
            val lines = buildList {
                if (rotated.exists()) addAll(rotated.readLines())
                if (current.exists()) addAll(current.readLines())
            }
            lines.takeLast(maxTailLines).mapNotNull { LogFormatter.parseFileLine(it) }
        }
    } catch (_: Throwable) {
        emptyList()
    }

    fun clear() {
        executor.execute {
            synchronized(lock) {
                runCatching {
                    current.delete()
                    rotated.delete()
                }
            }
        }
    }

    fun flush() {
        runCatching { executor.submit { }.get(2, TimeUnit.SECONDS) }
    }

    /** Test hook: stops the writer thread after flushing. */
    fun shutdown() {
        flush()
        executor.shutdown()
    }

    companion object {
        const val FILE_NAME = "onfaa.log"
        const val DEFAULT_MAX_BYTES = 256L * 1024L
        const val DEFAULT_TAIL_LINES = 200
    }
}
