package edu.uwo.csd.dcsim.host.resourcemanager;

import java.util.Collection;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.vm.VmAllocation;
import edu.uwo.csd.dcsim.vm.VmAllocationRequest;
import edu.uwo.csd.dcsim.vm.VmDescription;

public abstract class ResourceManager {

  protected Host host; //the host that this ResourceManager is managing

  /**
   * Get the Host that this ResourceManager is managing CPU the resources of
   *
   * @return Host
   */
  public final Host getHost() {
    return host;
  }

  /**
   * Set the Host that this ResourceManager is managing the resources of
   */
  public final void setHost(Host host) {
    this.host = host;
  }

	/*
         * CPU
	 */

  /**
   * Get the total physical CPU capacity of the host (total capacity of all CPUs and cores)
   */
  public final int getTotalCpu() {
    return getHost().totalCpu();
  }

  /**
   * Get the amount of physical CPU capacity in use (real usage, not allocation)
   */
  public final int getCpuInUse() {
    int cpuInUse = 0;

    if (host.getPrivDomainAllocation() != null) {
      cpuInUse += host.getPrivDomainAllocation().getResourcesInUse().cpu();
    }

    for (VmAllocation allocation : host.getVMAllocations()) {
      cpuInUse += allocation.getResourcesInUse().cpu();
    }

    return cpuInUse;
  }

  /**
   * Get the fraction of physical CPU capacity that is current in use (real usage, not allocation)
   */
  public final float getCpuUtilization() {
    return (float) getCpuInUse() / (float) getTotalCpu();
  }

  /**
   * Get the amount of CPU not being used (real usage, not allocation)
   */
  public final int getUnusedCpu() {
    return getTotalCpu() - getCpuInUse();
  }

  /**
   * Get the total amount of CPU that has been allocated. This value may be larger than the physical
   * CPU capacity due to oversubscription, but will always be <= the total allocation size
   */
  public final int getAllocatedCpu() {
    int allocatedCpu = 0;

    if (host.getPrivDomainAllocation() != null) {
			/*
			 * CPU Allocation methods
			 */
      allocatedCpu += host.getPrivDomainAllocation().getCpu();
    }

    for (VmAllocation vmAllocation : host.getVMAllocations()) {
      allocatedCpu += vmAllocation.getCpu();
    }

    return allocatedCpu;
  }

  /**
   * Get the amount of allocation space not yet allocated
   */
  public final int getAvailableCPUAllocation() {
    return getTotalCpu() - getAllocatedCpu();
  }
	
	
	/*
	 * Memory
	 */

  /**
   * Get the amount of memory that has been allocated
   */
  public final int getAllocatedMemory() {
    int memory = 0;

    if (host.getPrivDomainAllocation() != null) {
      memory += host.getPrivDomainAllocation().memory();
    }

    for (VmAllocation vmAllocation : host.getVMAllocations()) {
      memory += vmAllocation.memory();
    }
    return memory;
  }

  /**
   * Get the amount of memory still available to be allocated
   */
  public final int getAvailableMemory() {
    return getTotalMemory() - getAllocatedMemory();
  }

  /**
   * Get the total amount of memory on the Host
   */
  public final int getTotalMemory() {
    return getHost().memory();
  }
	
	
	/*
	 * Bandwidth
	 */

  /**
   * Get the total bandwidth available on the host
   */
  public final int getTotalBandwidth() {
    return getHost().bandwidth();
  }

  /**
   * Get the amount of bandwidth that has been allocated to VMs
   */
  public final int getAllocatedBandwidth() {
    int bandwidth = 0;

    if (host.getPrivDomainAllocation() != null) {
      bandwidth += host.getPrivDomainAllocation().bandwidth();
    }

    for (VmAllocation allocation : host.getVMAllocations()) {
      bandwidth += allocation.bandwidth();
    }
    return bandwidth;
  }

  /**
   * Get the amount of bandwidth still available to be allocated
   */
  public final int getAvailableBandwidth() {
    return getTotalBandwidth() - getAllocatedBandwidth();
  }
	
	
	/*
	 * Storage
	 */

  /**
   * Get the total amount of storage on the Host
   */
  public final int getTotalStorage() {
    return getHost().storage();
  }

  /**
   * Get the amount of storage that has been allocated to VMs
   */
  public final int getAllocatedStorage() {
    int storage = 0;

    if (host.getPrivDomainAllocation() != null) {
      storage += host.getPrivDomainAllocation().storage();
    }

    for (VmAllocation vmAllocation : host.getVMAllocations()) {
      storage += vmAllocation.storage();
    }
    return storage;
  }

  /**
   * Get the amount of storage still available to be allocated to VMs
   */
  public final int getAvailableStorage() {
    return getTotalStorage() - getAllocatedStorage();
  }
	
	
	/*
	 * Capability and Capacity checks
	 */

  /**
   * Verify whether this Host possesses the required capabilities to Host a VM with the specified
   * VMDescription. Does not consider current allocation of other VMs running on the host.
   */
  public final boolean isCapable(VmDescription vmDescription) {
    //check cores and core capacity
    if (vmDescription.numCores() > this.getHost().numCores()) {
      return false;
    }
    if (vmDescription.coreCapacity() > this.getHost().coreCapacity()) {
      return false;
    }

    //check total memory
    if (vmDescription.memory() > getTotalMemory()) {
      return false;
    }

    //check total bandwidth
    if (vmDescription.bandwidth() > getTotalBandwidth()) {
      return false;
    }

    //check total storage
    if (vmDescription.storage() > getTotalStorage()) {
      return false;
    }

    return true;
  }

  /**
   * Determine if the Host has enough remaining capacity to host a VM or set of VMs requiring the
   * specified amount of resource.
   */
  public abstract boolean hasCapacity(int cpu, int memory, int bandwidth, int storage);

  /**
   * Determine if the Host has enough remaining capacity to host the VM.
   */
  public abstract boolean hasCapacity(VmAllocationRequest vmAllocationRequest);

  /**
   * Determine if the Host has enough remaining capacity to host a set of VMs
   */
  public abstract boolean hasCapacity(Collection<VmAllocationRequest> vmAllocationRequests);

  /**
   * Allocate resources to a VMAllocation based on requested resources in the VMAllocationRequest
   *
   * @param vmAllocationRequest Requested resource allocation
   * @param vmAllocation        Actual allocation object to grant request resources to
   */
  public abstract boolean allocateResource(VmAllocationRequest vmAllocationRequest,
                                           VmAllocation vmAllocation);

  /**
   * Deallocate resources from the VMAllocation
   */
  public abstract void deallocateResource(VmAllocation vmAllocation);

  /**
   * Allocate resources to the privileged domain
   */
  public abstract void allocatePrivDomain(VmAllocation privDomainAllocation, int cpu, int memory,
                                          int bandwidth, int storage);

}
