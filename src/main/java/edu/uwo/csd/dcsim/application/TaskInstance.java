package edu.uwo.csd.dcsim.application;

import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.vm.Vm;

/**
 * @author Michael Tighe
 */
public abstract class TaskInstance {

  protected final long id;
  private final Simulation simulation;
  protected Vm vm; //the VM on which this task is running
  protected Resource fullDemand;
  protected Resource resourceDemand;
  protected Resource resourceScheduled;

  /**
   * Create a new Application, attached to the specified Simulation
   */
  public TaskInstance(Simulation simulation) {
    this.simulation = simulation;
    this.fullDemand = Resource.empty();
    this.resourceDemand = Resource.empty();
    this.resourceScheduled = Resource.empty();
    id = simulation.nextId(this.getClass().toString());
  }

  public long getId() {
    return id;
  }

  /**
   * Get the VM that this Application is running in
   */
  public Vm getVM() {
    return vm;
  }

  /**
   * Set the VM that this Application is running in
   */
  public void setVM(Vm vm) {
    this.vm = vm;
  }

  public final Resource getFullDemand() {
    return fullDemand;
  }

  public final Resource getResourceDemand() {
    return resourceDemand;
  }

  public final Resource getResourceScheduled() {
    return resourceScheduled;
  }

  public final void setFullDemand(Resource fullDemand) {
    this.fullDemand = fullDemand;
  }

  public final void setResourceDemand(Resource resourceDemand) {
    this.resourceDemand = resourceDemand;
  }

  public final void setResourceScheduled(Resource resourceScheduled) {
    this.resourceScheduled = resourceScheduled;
  }

  /**
   * Called after scheduling but before advancing to the next simulation time (executing), offering
   * an opportunity to trigger future events
   */
  public abstract void postScheduling();

  public abstract Task getTask();

  public int nextId(String name) {
      return simulation.nextId(name);
  }
}
