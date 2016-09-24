package edu.uwo.csd.dcsim.application;

import java.util.ArrayList;
import java.util.List;

import ca.carleton.dcsim.application.AppTask;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.vm.VmAllocationRequest;
import edu.uwo.csd.dcsim.vm.VmDescription;

/**
 * @author Michael Tighe
 */
public abstract class Task {

  private final int id;

  protected final Resource resourceSize;
  protected final int defaultInstances;
  protected final int maxInstances;

  private boolean active = false;

  protected final Simulation simulation;

  public Task(int defaultInstances,
              Resource resourceSize,
              Simulation simulation) {
    this(defaultInstances, defaultInstances,
         resourceSize, simulation);
  }

  public Task(int defaultInstances,
              int maxInstances,
              Resource resourceSize,
              Simulation simulation) {
    this.defaultInstances = defaultInstances;
    this.maxInstances = maxInstances;
    this.resourceSize = resourceSize;
    this.simulation = simulation;
    id = simulation.nextId(AppTask.class.toString());
  }

  /**
   * Active indicates whether or not an instance of this Task has ever been created. Controls
   * whether or not the Application metrics are recorded for this application (metrics are not
   * recorded for inactive Applications)
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Force task to be active. Irreversible.
   */
  public void activate() {
    this.active = true;
  }

  /**
   * Create an Task instance for this task
   */
  public abstract TaskInstance createInstance();

//  public abstract void removeInstance(TaskInstance instance);

  public void startInstance(TaskInstance instance) {
    activate();
    doStartInstance(instance);
  }

  public abstract void doStartInstance(TaskInstance instance);

  public void stopInstance(TaskInstance instance) {
    doStopInstance(instance);
  }

  public abstract void doStopInstance(TaskInstance instance);

  /**
   * Get the collection of Task Instances in this Task
   */
  public abstract List<TaskInstance> getInstances();

  public abstract Application getApplication();

  public List<VmAllocationRequest> createInitialVmRequests() {
    ArrayList<VmAllocationRequest> vmList = new ArrayList<VmAllocationRequest>();

    //create a VMAllocationRequest for the minimum number of instances
    for (int i = 0; i < getDefaultInstances(); ++i) {
      vmList.add(new VmAllocationRequest(new VmDescription(this)));
    }
    return vmList;
  }

  public int getDefaultInstances() {
    return defaultInstances;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public Resource getResourceSize() {
    return resourceSize;
  }

  public int getId() {
    return id;
  }
}
