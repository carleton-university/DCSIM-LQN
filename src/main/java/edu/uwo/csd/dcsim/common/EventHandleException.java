package edu.uwo.csd.dcsim.common;

import org.apache.log4j.Logger;

import edu.uwo.csd.dcsim.core.Event;

/**
 * @author Derek Hawker
 */
public class EventHandleException extends RuntimeException {

  public EventHandleException(Event ev,
                              String reason,
                              Logger logger) {
    logger.error("Event failure: " + reason);
    logger.error(ev.toString());
  }
}
