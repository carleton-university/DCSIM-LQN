package edu.uwo.csd.dcsim.core;

import java.util.Comparator;

public class EventComparator implements Comparator<Event> {

  public static EventComparator eventComparator = new EventComparator();

  @Override
  public int compare(Event e1, Event e2) {
    return (e1.getTime() == e2.getTime()) ?
           Long.compare(e1.getSendOrder(), e2.getSendOrder()) :
           Long.compare(e1.getTime(), e2.getTime());
  }
}
