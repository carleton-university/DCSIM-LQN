package edu.uwo.csd.dcsim.vm;

import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * Describes the general characteristics of a VM, and can instantiate new instances of VMs based on
 * the description
 *
 * @author Michael Tighe
 */
public class VmDescription {

  private Resource resources;
  private Task task;

  public VmDescription(Task task) {
    this.resources = task.getResourceSize();
    this.task = task;
  }

//  private VmDescription(int cores,
//                        int coreCapacity,
//                        int memory,
//                        int bandwidth,
//                        int storage,
//                        Task task) {
//    this.resources = Resource.create(1, cores, coreCapacity, memory, bandwidth, storage);
//    this.task = task;
//  }

  public Vm createVM(Simulation simulation, VmAllocation vmAllocation) {
    return new Vm(simulation, this, task.createInstance(), vmAllocation);
  }

  public int grossCpu() {
    return resources.grossCpu();
  }

  public int numCores() {
    return resources.numCores();
  }

  public int numCpus() {
    return resources.numCpus();
  }

  public int coreCapacity() {
    return resources.coreCapacity();
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

  public Task getTask() {
    return task;
  }

  public Resource resources() {
    return resources;
  }
}
