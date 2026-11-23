package org.evoionosp.abstrak.domain.audiobook.sync

interface ProgressSyncScheduler {
    /** Delivers queued progress once the network allows, leaving a delivery already pending as it is. */
    fun schedule()

    /**
     * Delivers queued progress as soon as the network allows, replacing a pending delivery that may
     * be waiting out a retry backoff. For when the reason deliveries failed has just gone away.
     */
    fun scheduleNow()
}
