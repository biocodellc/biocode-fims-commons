package biocode.fims.api.services;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple request limiter based on Thread.sleep method.
 * Create limiter instance via {@link #create(float)} and call {@link #consume()} before making any request.
 * If the limit is exceeded cosume method locks and waits for current call rate to fall down below the limit
 */
public class RateLimiter {

    private long minTime;

    private long lastSchedAction;
    private double avgSpent = 0;

    private ArrayList<RatePeriod> periods;


    public static class RatePeriod {
        private LocalTime start;
        private LocalTime end;
        private float maxRate;

        private RatePeriod(LocalTime start, LocalTime end, float maxRate) {
            this.start = start;
            this.end = end;
            this.maxRate = maxRate;
        }
    }


    /**
     * Create request limiter with maxRate - maximum number of requests per second
     *
     * @param maxRate - maximum number of requests per second
     * @return
     */
    public static RateLimiter create(float maxRate) {
        return new RateLimiter(Arrays.asList(new RatePeriod(LocalTime.of(0, 0, 0),
                LocalTime.of(23, 59, 59), maxRate)));
    }

    /**
     * Create request limiter with ratePeriods calendar - maximum number of requests per second in every period
     *
     * @param ratePeriods - rate calendar
     * @return
     */
    public static RateLimiter create(List<RatePeriod> ratePeriods) {
        return new RateLimiter(ratePeriods);
    }

    private void checkArgs(List<RatePeriod> ratePeriods) {

        for (RatePeriod rp : ratePeriods) {
            if (null == rp || rp.maxRate <= 0.0f || null == rp.start || null == rp.end)
                throw new IllegalArgumentException("list contains null or rate is less then zero or period is zero length");
        }
    }

    private float getCurrentRate() {

        LocalTime now = LocalTime.now();

        for (RatePeriod rp : periods) {
            if (now.isAfter(rp.start) && now.isBefore(rp.end))
                return rp.maxRate;
        }

        return Float.MAX_VALUE;
    }


    private RateLimiter(List<RatePeriod> ratePeriods) {

        checkArgs(ratePeriods);
        periods = new ArrayList<>(ratePeriods.size());
        periods.addAll(ratePeriods);

        this.minTime = (long) (1000.0f / getCurrentRate());
        this.lastSchedAction = System.currentTimeMillis() - minTime;
    }

    /**
     * Call this method before making actual request.
     * Method call locks until current rate falls down below the limit
     *
     * @throws InterruptedException
     */
    public void consume() throws InterruptedException {

        long timeLeft;

        synchronized (this) {
            long curTime = System.currentTimeMillis();

            minTime = (long) (1000.0f / getCurrentRate());
            timeLeft = lastSchedAction + minTime - curTime;

            long timeSpent = curTime - lastSchedAction + timeLeft;
            avgSpent = (avgSpent + timeSpent) / 2;

            if (timeLeft <= 0) {
                lastSchedAction = curTime;
                return;
            }

            lastSchedAction = curTime + timeLeft;
        }

        Thread.sleep(timeLeft);
    }

    public synchronized float getCurRate() {
        return (float) (1000d / avgSpent);
    }
}

