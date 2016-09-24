package edu.uwo.csd.dcsim.vm;

/**
 * A request sent to a Host asking it to create a VMAllocation to host a VM, with specified
 * properties.
 *
 * @author Michael Tighe
 */
public class VmAllocationRequest {

  private final VmDescription vmDescription;

  public VmAllocationRequest(VmDescription vmDescription) {
    this.vmDescription = vmDescription;
  }

  public VmDescription vmDescription() {
    return vmDescription;
  }

  public int coreCapacity() {
    return vmDescription.coreCapacity();
  }

  public int memory() {
    return vmDescription.memory();
  }

  public int bandwidth() {
    return vmDescription.bandwidth();
  }

  public int storage() {
    return vmDescription.storage();
  }

  public int numCores() {
    return vmDescription.numCores();
  }
}
