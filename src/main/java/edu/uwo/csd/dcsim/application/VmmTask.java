package edu.uwo.csd.dcsim.application;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * @author Michael Tighe
 */
public class VmmTask extends Task {

  private VmmApplication application;
  private VmmTaskInstance instance;

  public VmmTask(VmmApplication application,
                 int defaultInstances,
                 int minInstances,
                 int maxInstances,
                 Resource resourceSize,
                 Simulation simulation) {
    super(defaultInstances, resourceSize, simulation);

    this.application = application;
    instance = new VmmTaskInstance(this);
  }

  @Override
  public TaskInstance createInstance() {
    return instance;
  }

  public void removeInstance(TaskInstance instance) {
    throw new IllegalStateException("Not Implemented.");

  }

  @Override
  public void doStartInstance(TaskInstance instance) {
    throw new IllegalStateException("Not Implemented.");
  }

  @Override
  public void doStopInstance(TaskInstance instance) {
    throw new IllegalStateException("Not Implemented.");
  }

  @Override
  public List<TaskInstance> getInstances() {
    ArrayList<TaskInstance> instances = new ArrayList<TaskInstance>();
    instances.add(instance);
    return instances;
  }

  @Override
  public Application getApplication() {
    return application;
  }

  public VmmTaskInstance getInstance() {
    return instance;
  }


}
