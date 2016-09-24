package edu.uwo.csd.dcsim.application;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.application.sla.ServiceLevelAgreement;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.management.AutonomicManager;
import edu.uwo.csd.dcsim.management.events.ShutdownVmEvent;
import edu.uwo.csd.dcsim.vm.Vm;
import edu.uwo.csd.dcsim.vm.VmAllocation;
import edu.uwo.csd.dcsim.vm.VmAllocationRequest;

/**
 * Represents an Application running in the DataCentre, consisting of an incoming Workload source
 * and a set of application tiers. Each tier completes work and passes it on to the next tier, until
 * the final tier returns it to the Workload. A tier consists of one or more running Applications.
 *
 * @author Michael Tighe
 */
public abstract class Application {

  protected final Simulation simulation;

  private final long applicationConstructionTime;
  private final int id;
  private ServiceLevelAgreement sla = null;
  private List<ApplicationListener> applicationListeners = new ArrayList<ApplicationListener>();

  private final int hashCode;
  private boolean complete = false;
  private long activateTimeStamp = Long.MIN_VALUE;
  private long completeTimeStamp = Long.MIN_VALUE;

  public Application(Simulation simulation) {
    this.simulation = simulation;
    simulation.addApplication(this);
    this.id = simulation.nextId(Application.class.toString());

    applicationConstructionTime = simulation.getSimulationTime();
    //init hashCode
    hashCode = new HashCodeBuilder()
        .append(id())
        .append(simulation.getSimulationTime())
        .build();
  }

  /**
   * Indicates whether or not this Application is Active. Active Applications have had at least one
   * instance of each Task created at some point in the simulation. Metrics are only recorded for
   * active Applications.
   */
  public boolean isActive() {
    for (Task t : getTasks()) {
      if (!t.isActive()) {
        return false;
      }
    }

    if (activateTimeStamp == Long.MIN_VALUE) {
      activateTimeStamp = simulation.getSimulationTime();
    }

    return true;
  }

  /**
   * Force all tasks (and therefore, the application) to be active. Irreversible.
   */
  public void activate() {
    for (Task t : getTasks()) {
      t.activate();
    }
  }

  public long getActivateTimeStamp() {
    return activateTimeStamp;
  }

  public long getCompleteTimeStamp() {
    return completeTimeStamp;
  }

  public abstract void initializeScheduling();

  public abstract boolean updateDemand();

  public abstract void postScheduling();

  public abstract void advanceSimulation();

  /**
   * Generate a set of VMAllocationRequests for the initial set of VMs run the Application
   */
  public List<VmAllocationRequest> createInitialVmRequests() {
    ArrayList<VmAllocationRequest> vmList = new ArrayList<VmAllocationRequest>();

    //create a VMAllocationRequest for the minimum number of instances in each task
    for (Task task : getTasks()) {
      vmList.addAll(task.createInitialVmRequests());
    }
    return vmList;
  }

  public boolean canShutdown() {
    //verify that none of the VMs in the service are currently migrating
    for (Task task : getTasks()) {
      for (TaskInstance instance : task.getInstances()) {
        if (instance.getVM().isMigrating() || instance.getVM().isPendingMigration()) {
          return false;
        }
      }
    }

    return true;
  }

  public void shutdownApplication(AutonomicManager target, Simulation simulation) {

    complete = true;
    completeTimeStamp = simulation.getSimulationTime();

    for (Task task : getTasks()) {
      List<TaskInstance> instances = task.getInstances();
      for (TaskInstance instance : instances) {
        Vm vm = instance.getVM();
        VmAllocation vmAlloc = vm.getVMAllocation();
        Host host = vmAlloc.host();

        if (vm.isMigrating() || instance.getVM().isPendingMigration()) {
          throw new RuntimeException("Tried to shutdown migrating VM #" + vm.getId()
                                     + ". Operation not allowed in simulation.");
        }

        simulation.sendEvent(new ShutdownVmEvent(target, host.id(), vm.getId()));
      }
    }

    simulation.removeApplication(this);

    for (ApplicationListener listener : applicationListeners) {
      listener.onShutdownApplication(this);
    }
  }

  public abstract int getTotalCpuDemand();

  public abstract int getTotalCpuScheduled();

  /**
   * Get the tasks that this Application consists of
   */
  public abstract List<Task> getTasks();

  public List<ApplicationListener> getApplicationListeners() {
    return applicationListeners;
  }

  public void addApplicationListener(ApplicationListener listener) {
    applicationListeners.add(listener);
  }

  public void removeApplicationListener(ApplicationListener listener) {
    applicationListeners.remove(listener);
  }

  public Simulation simulation() {
    return simulation;
  }

  public int id() {
    return id;
  }

  public ServiceLevelAgreement getSla() {
    return sla;
  }

  public void setSla(ServiceLevelAgreement sla) {
    this.sla = sla;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    return this == o ||
           o instanceof Application &&
           isEqual((Application) o);
  }

  private boolean isEqual(Application o) {
    return id() == o.id() &&
           applicationConstructionTime == o.applicationConstructionTime;
  }

  public int getMaxSize() {
    int size = 0;
    for (Task task : getTasks()) {
      size += task.maxInstances;
    }
    return size;
  }

  public int getSize() {
    int size = 0;
    for (Task task : getTasks()) {
      size += task.getInstances().size();
    }
    return size;
  }

  public boolean isComplete() {
    return complete;
  }
}
