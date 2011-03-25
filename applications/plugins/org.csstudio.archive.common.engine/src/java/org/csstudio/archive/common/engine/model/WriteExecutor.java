/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.common.engine.model;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.archive.common.engine.service.IServiceProvider;
import org.csstudio.archive.common.service.engine.ArchiveEngineId;
import org.csstudio.domain.desy.system.IAlarmSystemVariable;
import org.csstudio.domain.desy.time.TimeInstant;
import org.csstudio.platform.logging.CentralLogger;
import org.joda.time.Duration;

import com.google.common.collect.Maps;

/**
 * Executor wrapper to handle the archive writer pool.
 * The write worker is periodically scheduled to submit all samples from the channels' samplebuffers
 * to the persistence layer via the archive service.
 *
 *  @author Kay Kasemir
 *  @author Bastian Knerr
 */
public class WriteExecutor {

    private static final int MAX_AWAIT_TERM_TIME_S = 2;

    private static final Logger LOG = CentralLogger.getInstance().getLogger(WriteExecutor.class);

    /** Minimum write period [seconds] */
    private static final long MIN_WRITE_PERIOD_MS = 5000;

    private final ConcurrentMap<String, ArchiveChannel<Object, IAlarmSystemVariable<Object>>> _channelMap =
        Maps.newConcurrentMap();
    private final ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();
    private WriteWorker _writeWorker;
    private long _writePeriodInMS;

    private final IServiceProvider _provider;

    private final ArchiveEngineId _engineId;

    /**
     * Construct thread for writing to server
     * @param provider provider for the service
     */
    public WriteExecutor(@Nonnull final IServiceProvider provider,
                         @Nonnull final ArchiveEngineId engineId) {
        _provider = provider;
        _engineId = engineId;
    }

    @Nonnull
    private WriteWorker submitAndScheduleWriteWorker(@Nonnull final ArchiveEngineId engineId,
                                                     @Nonnull final IServiceProvider provider,
                                                     final long writePeriodInMS,
                                                     final long delayInMS) {
        final WriteWorker writeWorker = new WriteWorker(engineId,
                                                        provider,
                                                        "Periodic Archive Engine Writer",
                                                        _channelMap.values(),
                                                        writePeriodInMS);
        _executor.scheduleAtFixedRate(writeWorker,
                                      delayInMS,
                                      writeWorker.getPeriodInMS(),
                                      TimeUnit.MILLISECONDS);
        return writeWorker;
    }

    /** Add a channel's buffer that this thread reads */
    public void addChannel(@Nonnull final ArchiveChannel<Object, IAlarmSystemVariable<Object>> channel) {
        _channelMap.putIfAbsent(channel.getName(), channel);
    }

    /** Start the write worker.
     *  @param pWritePeriod Period between writes in seconds
     */
    public void start(final long pWritePeriod) {
        if (_writeWorker != null) {
            LOG.warn("Worker has already been submitted with period (ms): " + _writeWorker.getPeriodInMS());
            return;
        }

        _writePeriodInMS = adjustWritePeriod(pWritePeriod, MIN_WRITE_PERIOD_MS);

        _writeWorker = submitAndScheduleWriteWorker(_engineId,
                                                    _provider,
                                                    _writePeriodInMS,
                                                    0L);

    }

    private long adjustWritePeriod(final long pWritePeriod, final long minWritePeriodMs) {
        long writePeriod = pWritePeriod;
        if (writePeriod < minWritePeriodMs) {
            LOG.warn("Adjusting write period from "
                    + pWritePeriod + " to " + minWritePeriodMs);
            writePeriod = minWritePeriodMs;
        }
        return writePeriod;
    }

    /** Reset statistics */
    public void reset() {
        if (_writeWorker != null) {
            _writeWorker.clear();
        }
    }

    /** @return Timestamp of end of last write run */
    @CheckForNull
    public TimeInstant getLastWriteTime() {
        return _writeWorker != null ? _writeWorker.getLastTimeWrite() : null;
    }

    /** @return Average number of values per write run */
    @CheckForNull
    public Double getAvgWriteCount() {
        return _writeWorker != null ? _writeWorker.getAvgWriteCount().getValue() : null;
    }

    /** @return  Average duration of write run */
    @CheckForNull
    public Duration getAvgWriteDuration() {
        final Double doubleDurMS = _writeWorker.getAvgWriteDurationInMS().getValue();
        return _writeWorker != null ? new Duration(doubleDurMS.longValue()) : null;
    }

    /**
     * Stop the write workers, after having submitted a directly executed shutdown worker that
     * shall empty the channels' sample buffers.
     * Gracefully shutdown of the executor.
     */
    public void shutdown() {
        if (!_executor.isShutdown()) {
            _executor.execute(new WriteWorker(_engineId,
                                              _provider,
                                              //this,
                                              "Shutdown worker", _channelMap.values(), 0L));
            _executor.shutdown();
            // Await termination (either the average duration or the max termination time.
            Duration dur = getAvgWriteDuration();
            if (dur == null || dur.getMillis() < Duration.standardSeconds(MAX_AWAIT_TERM_TIME_S).getMillis()) {
                dur = Duration.standardSeconds(MAX_AWAIT_TERM_TIME_S);
            }
            try {
                _executor.awaitTermination(dur.getMillis(), TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
