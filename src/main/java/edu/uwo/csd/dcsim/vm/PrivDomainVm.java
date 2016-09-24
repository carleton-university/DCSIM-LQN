package edu.uwo.csd.dcsim.vm;

import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.common.Utility;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * A special VM that runs the VmmApplication for a Host
 *
 * @author Michael Tighe
 */
public class PrivDomainVm extends Vm {

  public PrivDomainVm(Simulation simulation,
                      VmDescription vmDescription,
                      TaskInstance application,
                      VmAllocation allocation) {
    super(simulation, vmDescription, application, allocation);
  }

  @Override
  public void logState() {
    TaskInstance taskInstance = getTaskInstance();
    Resource resourceScheduled = getResourceScheduled();
    VmAllocation vmAllocation = getVMAllocation();
    Simulation simulation = getSimulation();

    if (getVMAllocation().host().getState() == Host.HostState.ON) {
      simulation.getLogger()
          .debug("PRIV  CPU[" + Utility.roundDouble(resourceScheduled.cpu(), 2) +
                 "/" + vmAllocation.getCpu() +
                 "/" + Utility.roundDouble(taskInstance.getResourceDemand().cpu(), 2) + "] " +
                 "BW[" + Utility.roundDouble(resourceScheduled.bandwidth(), 2) +
                 "/" + vmAllocation.bandwidth() +
                 "/" + Utility.roundDouble(taskInstance.getResourceDemand().bandwidth(), 2)
                 + "] " +
                 "MEM[" + resourceScheduled.memory() +
                 "/" + vmAllocation.memory() + "] " +
                 "STORAGE[" + resourceScheduled.storage() +
                 "/" + vmAllocation.storage() + "]");
    }

    //trace output
    simulation.getTraceLogger().info("#vp," + getId() + "," + vmAllocation.host().id() + "," +
                                     Utility.roundDouble(resourceScheduled.cpu(), 2) + ","
                                     + Utility
        .roundDouble(taskInstance.getResourceDemand().cpu(), 2) + "," +
                                     Utility.roundDouble(resourceScheduled.bandwidth(), 2) + ","
                                     + Utility
        .roundDouble(taskInstance.getResourceDemand().bandwidth(), 2) + "," +
                                     resourceScheduled.memory() + "," + vmAllocation.memory()
                                     + "," +
                                     resourceScheduled.storage() + "," + vmAllocation.storage());
  }

}
