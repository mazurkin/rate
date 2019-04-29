package org.test;

/**
 * Abstraction on gate that allows or forbids event processing depending on event rate, time and/or over
 * conditions
 */
public interface EventGate {

    /**
     * <p>Reset the gate to open state</p>
     */
    void reset();

    /**
     * <p>Check the gate is open</p>
     *
     * @return Returns {@code true} if gate is open
     */
    boolean open();

    /**
     * <p>Register an event</p>
     */
    void register();

}
