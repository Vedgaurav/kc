package com.one.kc.common.utils;


public final class SnowflakeIdGenerator {

    // ============================== Bits allocation ==============================
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    // ============================== Max values ==============================
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);        // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);          // 4095

    // ============================== Shift bits ==============================
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT =
            SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // ============================== Custom epoch (2024-01-01) ==============================
    private static final long EPOCH = 1704067200000L;

    // ============================== Instance fields ==============================
    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // ============================== Constructor ==============================
    public SnowflakeIdGenerator(long workerId, long datacenterId) {

        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
        }

        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(
                    String.format("Datacenter ID must be between 0 and %d", MAX_DATACENTER_ID));
        }

        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // ============================== Public API ==============================

    /**
     * Generates the next unique Snowflake ID.
     *
     * @return unique 64-bit ID
     */
    public synchronized long nextId() {

        long currentTimestamp = currentTime();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate id for "
                            + (lastTimestamp - currentTimestamp) + " ms");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    // ============================== Helper methods ==============================

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}

