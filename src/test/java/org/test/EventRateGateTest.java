package org.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class EventRateGateTest {

    private MockChronometer chronometer;

    @Before
    public void setUp() throws Exception {
        chronometer = MockChronometer.createFrozen("2017-01-01 12:00:00.000 UTC", 0);
    }

    @Test
    public void testUnderFlow() throws Exception {
        // 2000 events per 1 second results to 40 events per 20 milliseconds
        EventGate gate = new EventRateGate(2000, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        // sending 1000 events per second for the period of 10 seconds
        for (int i = 0; i < 10000; i++) {
            balance += gate.passed() ? 1 : 0;

            chronometer.shiftBy(1);
        }

        Assert.assertEquals(10000, balance);
    }

    @Test
    public void testNormalFlow() throws Exception {
        // 1000 events per 1 second results to 20 events per 20 milliseconds
        EventGate gate = new EventRateGate(1000, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        // sending 1000 events per second for the period of 10 seconds
        for (int i = 0; i < 10000; i++) {
            balance += gate.passed() ? 1 : 0;

            chronometer.shiftBy(1);
        }

        Assert.assertEquals(10000, balance);
    }

    @Test
    public void testOverFlow() throws Exception {
        // 800 events per 1 second results to 16 events per 20 milliseconds
        EventGate gate = new EventRateGate(800, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        // sending 1000 events per second for the period of 10 seconds
        for (int i = 0; i < 10000; i++) {
            balance += gate.passed() ? 1 : 0;

            chronometer.shiftBy(1);
        }

        Assert.assertEquals(8000, balance);
    }

    @Test
    public void testOverFlow2() throws Exception {
        EventGate gate = new EventRateGate(1, 1, TimeUnit.SECONDS, -1, chronometer);

        Assert.assertTrue(gate.passed());
        chronometer.shiftBy(999);
        Assert.assertFalse(gate.passed());
    }

    @Test
    public void testOverFlow3() throws Exception {
        EventGate gate = new EventRateGate(1, 1, TimeUnit.SECONDS, -1, chronometer);

        Assert.assertTrue(gate.passed());
        chronometer.shiftBy(1001);
        Assert.assertTrue(gate.passed());
    }

    @Test
    public void testOverFlow4() throws Exception {
        // 800 events per 1 second results to 16 events per 20 milliseconds
        EventGate gate = new EventRateGate(800, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        Random random = new Random(1);

        for (int i = 0; i < 10000; i++) {
            for (int j = 0, limit = 1 + random.nextInt(10); j < limit; j++) {
                balance += gate.passed() ? 1 : 0;
            }

            chronometer.shiftBy(1);
        }

        Assert.assertEquals(8000, balance);
    }

}