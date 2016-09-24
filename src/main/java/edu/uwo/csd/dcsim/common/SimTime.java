package edu.uwo.csd.dcsim.common;

import edu.uwo.csd.dcsim.core.Simulation;

public final class SimTime {

  public static long seconds(long seconds) {
    return seconds * 1000;
  }

  public static long minutes(long minutes) {
    return seconds(minutes * 60);
  }

  public static long hours(long hours) {
    return minutes(hours * 60);
  }

  public static long days(long days) {
    return hours(days * 24);
  }

  public static long weeks(long weeks) {
    return days(weeks * 7);
  }

  public static double toSeconds(long time) {
    return time / 1000;
  }

  public static double toMinutes(long time) {
    return toSeconds(time) / 60;
  }

  public static double toHours(long time) {
    return toMinutes(time) / 60;
  }

  public static double toDays(long time) {
    return toHours(time) / 24;
  }

  public static double toWeeks(long time) {
    return toDays(time) / 7;
  }

  public static String toHumanReadable(long time) {
    if (time < seconds(1)) {
      //output as ms
      return time + "ms";
    } else if (time < minutes(2)) {
      //output as seconds
      return Simulation.roundToMetricPrecision(toSeconds(time)) + "s";
    } else if (time < hours(2)) {
      //output as minutes
      return Simulation.roundToMetricPrecision(toMinutes(time)) + " min";
    } else if (time < days(2)) {
      //output as hours
      return Simulation.roundToMetricPrecision(toHours(time)) + "hrs";
    } else {
      //output as days
      return Simulation.roundToMetricPrecision(toDays(time)) + " days";
    }
  }
}
