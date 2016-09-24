package edu.uwo.csd.dcsim.host.resourcemanager;

import java.util.Collection;

import edu.uwo.csd.dcsim.vm.VmAllocation;
import edu.uwo.csd.dcsim.vm.VmAllocationRequest;

/**
 * The DefaultResourceManager implements a resource manager that oversubscribes CPU, and uses fixed,
 * non-oversubscribed allocations for all other resources.
 *
 * @author Michael Tighe
 */
public class DefaultResourceManager extends ResourceManager {

  @Override
  public boolean hasCapacity(int cpu, int memory, int bandwidth, int storage) {

    //note that we don't check CPU, as we are oversubscribing it
    return (memory <= getAvailableMemory()) &&
           (bandwidth <= getAvailableBandwidth()) &&
           (storage <= getAvailableStorage());

  }

  @Override
  public boolean hasCapacity(VmAllocationRequest vmAllocationRequest) {
    return hasCapacity(vmAllocationRequest.coreCapacity(), vmAllocationRequest.memory(),
                       vmAllocationRequest.bandwidth(), vmAllocationRequest.storage());
  }

  @Override
  public boolean hasCapacity(Collection<VmAllocationRequest> vmAllocationRequests) {
    int totalCpu = 0;
    int totalMemory = 0;
    int totalBandwidth = 0;
    int totalStorage = 0;

    for (VmAllocationRequest allocationRequest : vmAllocationRequests) {
      totalCpu += allocationRequest.coreCapacity();
      totalMemory += allocationRequest.memory();
      totalBandwidth += allocationRequest.bandwidth();
      totalStorage += allocationRequest.storage();
    }

    return hasCapacity(totalCpu, totalMemory, totalBandwidth, totalStorage);
  }

  @Override
  public boolean allocateResource(VmAllocationRequest vmAllocationRequest,
                                  VmAllocation vmAllocation) {

    if (hasCapacity(vmAllocationRequest)) {
      vmAllocation.setLumped(vmAllocationRequest.coreCapacity(),
                             vmAllocationRequest.memory(),
                             vmAllocationRequest.bandwidth(),
                             vmAllocationRequest.storage());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void deallocateResource(VmAllocation vmAllocation) {
    vmAllocation.setLumped(0, 0, 0, 0);
  }

  @Override
  public void allocatePrivDomain(VmAllocation privDomainAllocation,
                                 int cpu, int memory, int bandwidth, int storage) {

    if (hasCapacity(cpu, memory, bandwidth, storage)) {
      privDomainAllocation.setLumped(cpu, memory, bandwidth, storage);
    } else {
      throw new RuntimeException(
          "Could not allocate privileged domain on Host #" + getHost().id());
    }
  }
}
