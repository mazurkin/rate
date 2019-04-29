package org.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class EventRateGateTest {

    private MockChronometer chronometer;

    @Before
    public void setUp() {
        chronometer = MockChronometer.createFrozen(0, 0);
    }

    @Test
    public void testUnderFlow() throws Exception {
        // 2000 events per 1 second results to 40 events per 20 milliseconds
        EventGate gate = new EventRateGate(2000, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        // sending 1000 events per second for the period of 10 seconds
        for (int i = 0; i < 10000; i++) {
            balance += gate.open() ? 1 : 0;
            gate.register();
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
            balance += gate.open() ? 1 : 0;
            gate.register();
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
            boolean shouldPass = gate.open();
            balance += shouldPass ? 1 : 0;
            if (shouldPass) {
                gate.register();
            }
            chronometer.shiftBy(1);
        }

        Assert.assertEquals(8000, balance);
    }

    @Test
    public void testOverFlow2() throws Exception {
        EventGate gate = new EventRateGate(1, 1, TimeUnit.SECONDS, -1, chronometer);

        Assert.assertTrue(gate.open());
        gate.register();
        chronometer.shiftBy(999);
        Assert.assertFalse(gate.open());
    }

    @Test
    public void testOverFlow3() throws Exception {
        EventGate gate = new EventRateGate(1, 1, TimeUnit.SECONDS, -1, chronometer);

        Assert.assertTrue(gate.open());
        gate.register();
        chronometer.shiftBy(1001);
        Assert.assertTrue(gate.open());
    }

    @Test
    public void testOverFlow4() throws Exception {
        // 800 events per 1 second results to 16 events per 20 milliseconds
        EventGate gate = new EventRateGate(800, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        Random random = new Random(1);

        for (int i = 0; i < 10000; i++) {
            for (int j = 0, limit = 1 + random.nextInt(10); j < limit; j++) {
                boolean shouldPass = gate.open();
                balance += shouldPass ? 1 : 0;
                if (shouldPass) {
                    gate.register();
                }
            }

            chronometer.shiftBy(1);
        }

        Assert.assertEquals(8000, balance);
    }

    @Test
    public void testOverFlow5() throws Exception {
        // 800 events per 1 second results to 16 events per 20 milliseconds
        EventGate gate = new EventRateGate(800, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        // sending 800 events in the first nanosecond (crazy but let's assume that)
        for (int i = 0; i < 800; i++) {
            balance += gate.open() ? 1 : 0;
            gate.register();
        }

        // after that spike only 16 events are registered (effectiveRate=16)
        // 0 ms gone
        Assert.assertEquals(16, balance);

        // Shall be closed after 999 ms (the whole second is already spent)
        chronometer.shiftBy(999);
        Assert.assertFalse(gate.open());

        // Shall be opened after 1000 ms
        chronometer.shiftBy(1);
        Assert.assertTrue(gate.open());
    }

    @Test
    public void testOverFlow6() throws Exception {
        // 800 events per 1 second results to 16 events per 20 milliseconds
        EventGate gate = new EventRateGate(800, 1, TimeUnit.SECONDS, -1, chronometer);

        int balance = 0;

        // sending 800 events in the first 8 milliseconds
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 100; i++) {
                balance += gate.open() ? 1 : 0;
                gate.register();
            }

            chronometer.shiftBy(1);
        }

        // after that spike only 16 events are registered (effectiveRate=16)
        // 8 ms gone
        Assert.assertEquals(16, balance);

        // Shall be closed after 991 ms (the whole second is already spent)
        chronometer.shiftBy(991);
        Assert.assertFalse(gate.open());

        // Shall be opened after 1000 ms
        chronometer.shiftBy(1);
        Assert.assertTrue(gate.open());
    }

    @Test
    public void testNormalSteps() {
        // 800 events per 1 second results to 16 events per 20 milliseconds
        EventGate gate = new EventRateGate(800, 1, TimeUnit.SECONDS, -1, chronometer);

        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 800; i++) {
                gate.register();
            }
            Assert.assertFalse(gate.open());

            chronometer.shiftBy(100);
            Assert.assertFalse(gate.open());

            chronometer.shiftBy(900);
            Assert.assertTrue(gate.open());
        }
    }

}