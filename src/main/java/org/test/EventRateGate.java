package org.test;

import java.util.concurrent.TimeUnit;

/**
 * Rate-based event gate that controls the number of events per specified time period.
 *
 * Not thread-safe implementation.
 */
public class EventRateGate implements EventGate {

    private static final int AUTO_GRANULARITY = -1;

    private static final long MAX_PERIOD_HOURS = 1;

    private static final long MIN_PERIOD_MILLIS = 10;

    private static final long MIN_AUTO_GRANULARITY_PERIOD_MS = 20;

    private static final long MIN_AUTO_GRANULARITY_RATE = 3;

    private final Chronometer chronometer;

    private long targetPeriodNs;

    private long targetEventRate;

    private long deadlineNs;

    private int count;

    /**
     * Constructs the rate-bases gate
     * @param rate The number of allowed events per period
     * @param time Time period
     * @param timeUnit Time unit of specified period
     */
    public EventRateGate(long rate, long time, TimeUnit timeUnit) {
        this(rate, time, timeUnit, AUTO_GRANULARITY);
    }

    /**
     * Constructs the rate-bases gate
     * @param rate The number of allowed events per period
     * @param time Time period
     * @param timeUnit Time unit of specified period
     * @param granularity Divider for the period. Specify -1 to auto-choose the divider.
     */
    public EventRateGate(long rate, long time, TimeUnit timeUnit, int granularity) {
        this(rate, time, timeUnit, granularity, SystemChronometer.INSTANCE);
    }

    /**
     * Constructs the rate-bases gate
     * @param rate The number of allowed events per period
     * @param time Time period
     * @param timeUnit Time unit of specified period
     * @param granularity Divider for the period. Specify -1 to auto-choose the divider.
     * @param chronometer Chronometer implementation
     */
    public EventRateGate(long rate, long time, TimeUnit timeUnit, int granularity, Chronometer chronometer) {
        this.chronometer = chronometer;

        this.deadlineNs = chronometer.getTickNs();
        this.count = 0;

        this.init(rate, time, timeUnit, granularity);
    }

    /**
     * Change the parameters of the gate
     *
     * @param rate The number of allowed events per period
     * @param time Time period
     * @param timeUnit Time unit of specified period
     * @param granularity Divider for the period. Specify -1 to auto-choose the divider.
     */
    public void init(long rate, long time, TimeUnit timeUnit, int granularity) {
        final int effectiveGranularity = granularity > 0 ?
                granularity : calculateAutoGranularity(rate, time, timeUnit);

        // Try to lower granularity of measure period
        final long effectiveRate = rate / effectiveGranularity;
        final long effectivePeriodNs = timeUnit.toNanos(time) / effectiveGranularity;

        if (effectiveRate > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rate value is invalid");
        }

        if (effectivePeriodNs > TimeUnit.HOURS.toNanos(MAX_PERIOD_HOURS)) {
            throw new IllegalArgumentException("Period is too high");
        }
        if (effectivePeriodNs < TimeUnit.MILLISECONDS.toNanos(MIN_PERIOD_MILLIS)) {
            throw new IllegalArgumentException("Period is too small");
        }

        this.targetPeriodNs = effectivePeriodNs;
        this.targetEventRate = effectiveRate;
    }

    @Override
    public boolean open() {
        return chronometer.getTickNs() >= deadlineNs;
    }

    @Override
    public void register() {
        final long nowNs = chronometer.getTickNs();
        final long elapsedNs = nowNs - deadlineNs;

        count++;

        if (count >= targetEventRate || elapsedNs >= targetPeriodNs) {
            final double eventsRegistered = count;
            final double eventsAllowed = 1.0 * targetEventRate * elapsedNs / targetPeriodNs;

            if (eventsRegistered > eventsAllowed) {
                final double eventsExcess = eventsRegistered - eventsAllowed;
                final long delayNs = Math.round(targetPeriodNs * eventsExcess / targetEventRate);
                deadlineNs = nowNs + delayNs;
            } else {
                deadlineNs = nowNs;
            }

            count = 0;
        }
    }

    @Override
    public void reset() {
        this.deadlineNs = chronometer.getTickNs();
        this.count = 0;
    }

    private static int calculateAutoGranularity(long rate, long time, TimeUnit timeUnit) {
        // safe divider for rate
        final int dividerByRate = (int) (rate / MIN_AUTO_GRANULARITY_RATE);

        // safe divider for period
        final int dividerByPeriod = (int) (timeUnit.toMillis(time) / MIN_AUTO_GRANULARITY_PERIOD_MS);

        // choose the minimum of both
        final int dividerMin = Math.min(dividerByRate, dividerByPeriod);

        return Math.max(1, dividerMin);
    }

}
