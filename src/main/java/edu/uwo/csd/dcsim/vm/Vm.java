package edu.uwo.csd.dcsim.vm;

import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.common.Utility;
import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.SimulationEventListener;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * Represents a Virtual Machine, running a Task Instance. Must be contained within a VMAllocation on
 * a Host
 *
 * @author Michael Tighe
 */
public class Vm implements SimulationEventListener {

  private final int id;
  private final Simulation simulation;
  private final VmDescription vmDescription;
  private final TaskInstance taskInstance;

  /**
   * Real physical resourcse allocated as prescribed by the vmDescription
   */
  private VmAllocation vmAllocation;
  private Resource resourceScheduled = Resource.empty();

  public Vm(Simulation simulation,
            VmDescription vmDescription,
            TaskInstance taskInstance,
            VmAllocation vmAllocation) {
    this.simulation = simulation;
    this.id = simulation.nextId(Vm.class.toString());
    this.vmDescription = vmDescription;

    taskInstance.setVM(this);
    this.taskInstance = taskInstance;

    vmAllocation.setVm(this);
    this.vmAllocation = vmAllocation;
  }

  public Resource getResourceDemand() {
    //cap CPU request at max CPU
    return taskInstance.getResourceDemand()
        .withCpu(Math.min(taskInstance.getResourceDemand().cpu(), getMaxCpu()));
  }

  public Resource getResourceScheduled() {
    //prevent editing of this value outside of scheduleResources(), since the taskInstance value must be the same
    return resourceScheduled;
  }

  public void scheduleResources(Resource resource) {

    //double check that we are not trying to schedule more than maxCpu
    if (resource.cpu() > getMaxCpu()) {
      throw new InsufficientResourcesException(resource, vmDescription.resources());
    }

    this.resourceScheduled = resource;
    taskInstance.setResourceScheduled(resource);
  }

  public int getMaxCpu() {
    return vmDescription.grossCpu();
  }

  public void startTaskInstance() {
    vmDescription.getTask().startInstance(taskInstance);
  }

  public void stopTaskInstance() {
    vmDescription.getTask().stopInstance(taskInstance);
  }

  public void logState() {
    if (getVMAllocation().host().getState() == Host.HostState.ON) {
      simulation.getLogger()
          .info("VM #" + getId() + " CPU[" + Utility.roundDouble(resourceScheduled.cpu(), 2) +
                 "/" + vmAllocation.getCpu() +
                 "/" + Utility.roundDouble(getResourceDemand().cpu(), 2) + "] " +
                 "BW[" + Utility.roundDouble(resourceScheduled.bandwidth(), 2) +
                 "/" + vmAllocation.bandwidth() +
                 "/" + Utility.roundDouble(getResourceDemand().bandwidth(), 2) + "] " +
                 "MEM[" + resourceScheduled.memory() +
                 "/" + vmAllocation.memory() + "] " +
                 "STORAGE[" + resourceScheduled.storage() +
                 "/" + vmAllocation.storage() + "]" +
                 "Task #" + taskInstance.getTask().getApplication().id() + "-" + taskInstance
              .getTask().getId());
    }

    //trace output
    simulation.getTraceLogger().info(
        "#v," + getId() +
        "," + vmAllocation.host().id() +
        "," + Utility.roundDouble(resourceScheduled.cpu(), 2) +
        "," + Utility.roundDouble(getResourceDemand().cpu(), 2) +
        "," + Utility.roundDouble(resourceScheduled.bandwidth(), 2) +
        "," + Utility.roundDouble(getResourceDemand().bandwidth(), 2) +
        "," + resourceScheduled.memory() +
        "," + vmAllocation.memory()
        + "," + resourceScheduled.storage() +
        "," + vmAllocation.storage());

  }

  public boolean isMigrating() {
    return vmAllocation.host().isMigrating(this);
  }

  public boolean isPendingMigration() {
    return vmAllocation.host().isPendingMigration(this);
  }

  public int getId() {
    return id;
  }

  public TaskInstance getTaskInstance() {
    return taskInstance;
  }

  public VmDescription getVMDescription() {
    return vmDescription;
  }

  public VmAllocation getVMAllocation() {
    return vmAllocation;
  }

  public void setVMAllocation(VmAllocation vmAllocation) {
    this.vmAllocation = vmAllocation;
  }

  @Override
  public void handleEvent(Event e) {
    simulation.getLogger().warn("A handle event in Vm.java was called and is not implemented.");
  }

  public Simulation getSimulation() {
    return simulation;
  }
}
