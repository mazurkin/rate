package org.test;

/**
 * Abstraction on gate that allows or forbids event processing depending on event rate, time and/or over
 * conditions
 */
public interface EventGate {

    /**
     * Register the event and get allowance to process the event
     * @return Returns <em>true</em> of further processing is allowed
     */
    boolean passed();

}
