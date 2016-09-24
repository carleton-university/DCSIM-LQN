package edu.uwo.csd.dcsim.vm;

import java.util.Comparator;

/**
 * This class compares two VM allocation requests in terms of overall resource capacity requested.
 * Resource needs are compared in the following order: memory, CPU cores, CPU core capacity, and
 * bandwidth.
 *
 * @author Gaston Keller
 */
public class VmAllocationRequestCapacityComparator implements Comparator<VmAllocationRequest> {

  @Override
  public int compare(VmAllocationRequest var1, VmAllocationRequest var2) {
    if (var1.memory() != var2.memory()) {
      return Integer.compare(var1.memory(), var2.memory());
    } else if (var1.numCores() != var2.numCores()) {
      return Integer.compare(var1.numCores(), var2.numCores());
    } else if (var1.coreCapacity() != var2.coreCapacity()) {
      return Integer.compare(var1.coreCapacity(), var2.coreCapacity());
    } else if (var1.bandwidth() != var2.bandwidth()) {
      return Integer.compare(var1.bandwidth(), var2.bandwidth());
    } else {
      return 0;
    }
  }
}
