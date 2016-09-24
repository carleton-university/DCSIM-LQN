package edu.uwo.csd.dcsim.vm;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * VMAllocation is an resource allocation made by a Host for a VM. VMAllocation is instantiate on a
 * Host to hold a VM, and never leaves the Host even if the VM migrates. A migrating VM moves to a
 * new VMAllocation on the target Host.
 *
 * @author Michael Tighe
 */
public class VmAllocation {

  private Vm vm;
  private VmDescription vmDescription;
  private Host host;
  private Resource resources;

  public VmAllocation(VmDescription vmDescription, Host host) {
    this.vmDescription = vmDescription;
    this.host = host;
    vm = null;

    resources = Resource.empty();
  }

  public Resource getResourcesInUse() {
    return (vm != null) ?
           //TODO should there be a resourcesInUse? or is resourcesScheduled sufficient?
           vm.getResourceScheduled() :
           //if no VM, return new VirtualResources indicating 0 resources in use
           Resource.empty();
  }

  public void attachVm(Vm vm) {
    this.vm = vm;
    vm.setVMAllocation(this);
  }

  public void setVm(Vm vm) {
    this.vm = vm;
  }

  public Vm getVm() {
    return vm;
  }

  public Host host() {
    return host;
  }

  public VmDescription vmDescription() {
    return vmDescription;
  }

  public int getCpu() {
    return resources.cpu();
  }

  public int memory() {
    return resources.memory();
  }

  public int bandwidth() {
    return resources.bandwidth();
  }

  public int storage() {
    return resources.storage();
  }

  public void setResources(Resource resources) {
    this.resources = resources;
  }

  public void setLumped(int cpu,
                        int memory,
                        int bandwidth,
                        int storage) {
    setResources(resources.withLumped(cpu, memory, bandwidth, storage));
  }
}
