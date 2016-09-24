package edu.uwo.csd.dcsim.management;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.vm.VmAllocation;

public class HostStatus {

  private long timeStamp;
  private int id;
  private int incomingMigrations;
  private int outgoingMigrations;
  private List<VmStatus> migratingInVms = new ArrayList<VmStatus>();
  private Host.HostState state;
  private List<Resource> startingVmAllocations = new ArrayList<Resource>();

  private double powerConsumption;

  VmStatus privDomain;
  List<VmStatus> vms = new ArrayList<VmStatus>();

  public HostStatus(Host host, long timeStamp) {

    this.timeStamp = timeStamp;

    id = host.id();
    incomingMigrations = host.getMigratingIn().size();
    outgoingMigrations = host.getMigratingOut().size();
    state = host.getState();

    powerConsumption = host.getCurrentPowerConsumption();

    privDomain = new VmStatus(host.getPrivDomainAllocation().getVm(), timeStamp);

    for (VmAllocation vmAlloc : host.getVMAllocations()) {
      if (vmAlloc.getVm() != null) {
        vms.add(new VmStatus(vmAlloc.getVm(), timeStamp));
      }
    }

    //keep track of resources promised to starting VMs
    for (VmAllocation vmAlloc : host.startingVms()) {
      startingVmAllocations.add(
          Resource.createLumped(vmAlloc.getCpu(), vmAlloc.memory(),
                                vmAlloc.bandwidth(), vmAlloc.storage()));
    }

    //keep track of resources promised to incoming VMs
    for (VmAllocation vmAlloc : host.getMigratingIn()) {
      Resource resource = Resource.createLumped(
          vmAlloc.vmDescription().grossCpu(), vmAlloc.vmDescription().memory(),
          vmAlloc.vmDescription().bandwidth(), vmAlloc.vmDescription().storage());

      migratingInVms.add(
          new VmStatus(vmAlloc.vmDescription().numCores(),
                       vmAlloc.vmDescription().coreCapacity(),
                       resource));
    }
  }

  public HostStatus(HostStatus host) {
    timeStamp = host.timeStamp;

    id = host.id;
    incomingMigrations = host.incomingMigrations;
    outgoingMigrations = host.outgoingMigrations;
    state = host.state;

    startingVmAllocations = new ArrayList<>();
    for (Resource r : host.startingVmAllocations) {
      startingVmAllocations.add(r);
    }

    powerConsumption = host.powerConsumption;

    privDomain = host.privDomain.copy();

    for (VmStatus vm : host.vms) {
      vms.add(vm.copy());
    }

    for (VmStatus vm : host.migratingInVms) {
      migratingInVms.add(vm.copy());
    }
  }

  public void instantiateVm(VmStatus vm) {
    vms.add(vm);
  }

  public void migrate(VmStatus vm, HostStatus target) {
    ++outgoingMigrations;
    vms.remove(vm);

    target.vms.add(vm);
    ++target.incomingMigrations;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public int getId() {
    return id;
  }

  public Host.HostState getState() {
    return state;
  }

  public VmStatus getPrivDomainState() {
    return privDomain;
  }

  public int getIncomingMigrationCount() {
    return incomingMigrations;
  }

  public int getOutgoingMigrationCount() {
    return outgoingMigrations;
  }

  public List<VmStatus> getVms() {
    return vms;
  }

  public List<Resource> getStartingVmAllocations() {
    return startingVmAllocations;
  }

  public List<VmStatus> getMigratingInVms() {
    return migratingInVms;
  }

  public int getCpuAllocated() {
    int cpu = 0;

    for (VmStatus vmStatus : vms) {
      cpu += vmStatus.getCores() * vmStatus.getCoreCapacity();
    }

    //add resources promised to starting VMs
    for (Resource resource : startingVmAllocations) {
      cpu += resource.cpu();
    }

    //add resources promised to incoming VMs
    for (VmStatus vmStatus : migratingInVms) {
      cpu += vmStatus.getCores() * vmStatus.getCoreCapacity();
    }

    return cpu;
  }

  public Resource resourcesInUse() {
    Resource resourceInUse = privDomain.getResourceInUse();

    for (VmStatus vmStatus : vms) {
      resourceInUse = resourceInUse.add(vmStatus.getResourceInUse());
    }
    //add resources promised to starting VMs
    for (Resource resource : startingVmAllocations) {
      resourceInUse = resourceInUse.add(resource);
    }
    //add resources promised to incoming VMs
    for (VmStatus vmStatus : migratingInVms) {
      resourceInUse = resourceInUse.add(vmStatus.getResourceInUse());
    }

    return resourceInUse;
  }

  public double getPowerConsumption() {
    return powerConsumption;
  }

  public HostStatus copy() {
    return new HostStatus(this);
  }
}
