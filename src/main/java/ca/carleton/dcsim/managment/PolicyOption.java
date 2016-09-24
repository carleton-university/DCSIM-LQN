package ca.carleton.dcsim.managment;

import com.google.auto.value.AutoValue;

import edu.uwo.csd.dcsim.management.Policy;

/**
 * @author Derek Hawker
 */
@AutoValue
public abstract class PolicyOption {

  public abstract Policy policy();

  public abstract long executionInterval();

  public abstract long startTime();

  public static PolicyOption create(Policy policy,
                                    long executionInterval,
                                    long startTime) {
    return new AutoValue_PolicyOption(policy, executionInterval, startTime);
  }
}
