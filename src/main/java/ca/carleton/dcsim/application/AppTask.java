package ca.carleton.dcsim.application;

import java.util.ArrayList;
import java.util.List;

import ca.carleton.lqn.core.tasks.TaskDescription;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.application.ApplicationListener;
import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.application.loadbalancer.EqualShareLoadBalancer;
import edu.uwo.csd.dcsim.application.loadbalancer.LoadBalancer;
import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * @author Derek Hawker
 */
public class AppTask extends Task {

  private LoadBalancer loadBalancer;
  private LqnApplication application;
  private List<AppInstance> instances = new ArrayList<>();
  private final String name;
  private final TaskDescription taskDescription;

  public AppTask(Builder builder) {
    super(builder.defaultInstances, builder.maxInstances,
          builder.resourceSize, builder.simulation);

    this.application = builder.application;

    if (builder.loadBalancer == null) {
      //set default load balancer
      setLoadBalancer(new EqualShareLoadBalancer());
    } else {
      setLoadBalancer(builder.loadBalancer.build());
    }

    name = builder.name;
    taskDescription = builder.taskDescription;
  }

  /**
   * Get the load balancer for this tier
   */
  public LoadBalancer getLoadBalancer() {
    return loadBalancer;
  }

  /**
   * Set the load balancer for this tier
   */
  public void setLoadBalancer(LoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
    loadBalancer.setTask(this);
  }

  @Override
  public TaskInstance createInstance() {
    AppInstance instance = new AppInstance(this, simulation);
    instances.add(instance);
    startInstance(instance);

    for (ApplicationListener listener : application.getApplicationListeners()) {
      listener.onCreateTaskInstance(instance);
    }

    return instance;
  }

  public void removeInstance(TaskInstance instance) {
    if (instances.contains(instance)) {
      instances.remove(instance);
      stopInstance(instance);

      for (ApplicationListener listener : application.getApplicationListeners()) {
        listener.onRemoveTaskInstance(instance);
      }
    } else {
      throw new RuntimeException("Attempted to remove instance from task that does not contain it");
    }
  }

  @Override
  public void doStartInstance(TaskInstance instance) {
    //if the application is active, ensure that the workload is enabled
    if (application.isActive()) {
      application.getWorkload().setEnabled(true);
    }
  }

  @Override
  public void doStopInstance(TaskInstance instance) {
    if (instances.contains(instance)) {
      instances.remove(instance);

      for (ApplicationListener listener : application.getApplicationListeners()) {
        listener.onRemoveTaskInstance(instance);
      }
    } else {
      throw new RuntimeException("Attempted to remove instance from task that does not contain it");
    }
  }

  @Override
  public Application getApplication() {
    return application;
  }

  public void setApplication(LqnApplication application) {
    this.application = application;
  }

  @Override
  public List<TaskInstance> getInstances() {
    return new ArrayList<TaskInstance>(instances);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(": [");

    for (AppInstance inst : instances) {
      sb.append(inst.getVM().getVMAllocation().host())
          .append(", ");
    }

    return sb.append("]").toString();
  }

  public String getName() {
    return name;
  }

  public TaskDescription taskDescription() {
    return taskDescription;
  }

  public static class Builder implements ObjectBuilder<AppTask> {

    int defaultInstances;
    int maxInstances;
    Resource resourceSize;
    ObjectBuilder<LoadBalancer> loadBalancer = null;
    LqnApplication application = null;
    String name = null;
    TaskDescription taskDescription;
    Simulation simulation;

    public Builder(int defaultInstances,
                   int maxInstances,
                   Resource resourceSize,
                   Simulation simulation) {

      this.defaultInstances = defaultInstances;
      this.maxInstances = maxInstances;
      this.resourceSize = resourceSize;
      this.simulation = simulation;
    }

    public Builder loadBalancer(ObjectBuilder<LoadBalancer> loadBalancer) {
      this.loadBalancer = loadBalancer;
      return this;
    }

    public Builder application(LqnApplication application) {
      this.application = application;
      return this;
    }

    public Builder taskDescription(TaskDescription taskDescription) {
      this.taskDescription = taskDescription;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public AppTask build() {
      if (taskDescription == null) {throw new IllegalStateException("No TaskDescription");}
      if (simulation == null) {throw new IllegalStateException("No Simulation");}
      if (name == null) {throw new IllegalStateException("No name");}

      return new AppTask(this);
    }
  }
}
