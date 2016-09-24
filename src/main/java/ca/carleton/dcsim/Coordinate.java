package ca.carleton.dcsim;

import com.google.auto.value.AutoValue;

/**
 * @author Derek Hawker
 */
@AutoValue
abstract public class Coordinate {

  public static Coordinate create(double x, double y) {
    return new AutoValue_Coordinate(x, y);
  }

  public abstract double x();

  public abstract double y();
}

