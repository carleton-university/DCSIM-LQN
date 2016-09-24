package edu.uwo.csd.dcsim.host;

import com.google.auto.value.AutoValue;

/**
 * @author Derek Hawker
 */
@AutoValue
public abstract class HostSpecification {

  public abstract int numCpu();

  public abstract int numCores();

  public abstract int coreCapacity();

  /** In MB */
  public abstract int memory();

  /** In Kb */
  public abstract int bandwidth();

  /** In MB */
  public abstract int storage();

  public static HostSpecification create(int numCpu,
                                         int numCores,
                                         int coreCapacity,
                                         int memory,
                                         int bandwidth,
                                         int storage) {
    return new AutoValue_HostSpecification(numCpu, numCores, coreCapacity, memory, bandwidth,
                                           storage);
  }

}
