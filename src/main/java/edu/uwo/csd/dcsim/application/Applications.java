package edu.uwo.csd.dcsim.application;


import edu.uwo.csd.dcsim.application.workload.Workload;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * Provides factory methods to construct common Service configurations. Additional factory methods
 * will be added in future releases.
 *
 * @author Michael Tighe
 */
public final class Applications {

  private Applications() {
  }

  /**
   * Creates a new single tiered Service
   */
  public static InteractiveApplication singleTaskInteractiveApplication(Simulation simulation,
                                                                        Workload workload,
                                                                        int coreCapacity,
                                                                        int memory,
                                                                        int bandwidth,
                                                                        int storage,
                                                                        double serviceTime) {

    InteractiveApplication.Builder builder = new InteractiveApplication.Builder(simulation)
        .workload(workload)
        .thinkTime(4)
        .task(1, 1,
              Resource.createLumped(coreCapacity, memory, bandwidth, storage),
              serviceTime, 1);
    return builder.build();
  }

}
