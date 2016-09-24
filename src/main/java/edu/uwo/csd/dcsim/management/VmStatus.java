package edu.uwo.csd.dcsim.management;

import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.vm.Vm;
import edu.uwo.csd.dcsim.vm.VmDescription;

public class VmStatus {

  private final long timeStamp;
  private final int id;

  private final Resource totalResources;
  private final Resource resourceInUse;
  private final Vm vm;

  public VmStatus(Vm vm, long timeStamp) {
    this.timeStamp = timeStamp;
    this.vm = vm;

    id = vm.getId();
    VmDescription desc = vm.getVMDescription();
    totalResources = Resource.create(1, desc.numCores(),
                                     desc.coreCapacity(), desc.memory(),
                                     desc.bandwidth(), desc.storage());
    resourceInUse = vm.getResourceScheduled();
  }

  public VmStatus(VmStatus vmStatus) {
    timeStamp = vmStatus.timeStamp;
    vm = vmStatus.getVm();
    id = vmStatus.id;

    totalResources = vmStatus.totalResources;
    resourceInUse = vmStatus.resourceInUse;
  }

  /**
   * Creates a "dummy" VM status for a placeholder
   */
  public VmStatus(int cores,
                  int coreCapacity,
                  Resource resource) {
    this.timeStamp = -1;
    this.vm = null;
    this.id = -1;
    this.totalResources = Resource.create(1,cores,
                                          coreCapacity, resource.memory(), resource.bandwidth(),
                                          resource.storage());
    this.resourceInUse = resource;
  }

  public Resource getResourceInUse() {
    return resourceInUse;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public int getId() {
    return id;
  }

  public int getCores() {
    return totalResources.numCores();
  }

  public int getCoreCapacity() {
    return totalResources.coreCapacity();
  }

  public VmStatus copy() {
    return new VmStatus(this);
  }

  public Vm getVm() {
    if (vm == null) {
    	return null; 
      //throw new IllegalStateException("Trying to access a VM from a placeholder VM. VmStatus id: "+ id);
    } else {
      return vm;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof VmStatus) {
      VmStatus vm = (VmStatus) o;
      if (vm.getId() == id) {
        return true;
      }
    }
    return false;
  }

}
