package edu.uwo.csd.dcsim.application;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.application.loadbalancer.EqualShareLoadBalancer;
import edu.uwo.csd.dcsim.application.loadbalancer.LoadBalancer;
import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * A tier of InteractiveApplications
 *
 * @author Michael Tighe
 */
public class InteractiveTask extends Task {

  private final LoadBalancer loadBalancer;
  private final double normalServiceTime;
  private final double visitRatio;
  private InteractiveApplication application;
  private List<InteractiveTaskInstance> instances = new ArrayList<>();

  public InteractiveTask(InteractiveApplication application,
                         int defaultInstances,
                         int maxInstances,
                         Resource resourceSize,
                         double normalServiceTime,
                         double visitRatio,
                         LoadBalancer loadBalancer,
                         Simulation simulation) {
    super(defaultInstances, maxInstances, resourceSize, simulation);

    this.application = application;
    this.normalServiceTime = normalServiceTime;
    this.visitRatio = visitRatio;

    this.loadBalancer = loadBalancer;
    loadBalancer.setTask(this);
  }

  public InteractiveTask(InteractiveApplication application,
                         int defaultInstances,
                         int maxInstances,
                         Resource resourceSize,
                         double normalServiceTime,
                         double visitRatio,
                         Simulation simulation) {
    this(application, defaultInstances,
         maxInstances, resourceSize,
         normalServiceTime, visitRatio,
         new EqualShareLoadBalancer(), simulation);
  }

  public InteractiveTask(Builder builder) {
    this(builder.application, builder.defaultInstances,
         builder.maxInstances, builder.resourceSize,
         builder.serviceTime, builder.visitRatio,
         (builder.loadBalancer == null) ? new EqualShareLoadBalancer()
                                        : builder.loadBalancer.build(),
         builder.simulation);
  }

  public InteractiveTask(InteractiveTask interactiveTask) {
    this(interactiveTask.application, interactiveTask.defaultInstances,
         interactiveTask.maxInstances, interactiveTask.resourceSize,
         interactiveTask.normalServiceTime, interactiveTask.visitRatio,
         (interactiveTask.loadBalancer != null) ?
         interactiveTask.loadBalancer :
         new EqualShareLoadBalancer(),
         interactiveTask.simulation);
  }

  /**
   * Get the load balancer for this tier
   */
  public LoadBalancer getLoadBalancer() {
    return loadBalancer;
  }

  @Override
  public TaskInstance createInstance() {
    InteractiveTaskInstance instance = new InteractiveTaskInstance(this);
    instances.add(instance);
    startInstance(instance);

    for (ApplicationListener listener : application.getApplicationListeners()) {
      listener.onCreateTaskInstance(instance);
    }

    return instance;
  }

  public void removeInstance(InteractiveTaskInstance instance) {
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
    // TODO ...remove from Task/Load Balancer? When is this even used?

  }

  public double getNormalServiceTime() {
    return normalServiceTime;
  }

  public double getVisitRatio() {
    return visitRatio;
  }

  @Override
  public Application getApplication() {
    return application;
  }

  public void setApplication(InteractiveApplication application) {
    this.application = application;
  }

//  public List<InteractiveTaskInstance> getInteractiveTaskInstances() {
//    return instances;
//  }

  @Override
  public List<TaskInstance> getInstances() {
    return new ArrayList<TaskInstance>(instances);
  }

  public List<InteractiveTaskInstance> getInteractiveTaskInstances() {
    return new ArrayList<>(instances);
  }

  public static class Builder implements ObjectBuilder<InteractiveTask> {

    int defaultInstances;
    int maxInstances;
    Resource resourceSize;
    double serviceTime;
    double visitRatio;
    ObjectBuilder<LoadBalancer> loadBalancer = null;
    InteractiveApplication application = null;
    Simulation simulation;

    public Builder(int defaultInstances,
                   int maxInstances,
                   Resource resourceSize,
                   double serviceTime,
                   double visitRatio,
                   Simulation simulation) {

      this.defaultInstances = defaultInstances;
      this.maxInstances = maxInstances;
      this.resourceSize = resourceSize;
      this.serviceTime = serviceTime;
      this.visitRatio = visitRatio;
      this.simulation = simulation;
    }

    public Builder loadBalancer(ObjectBuilder<LoadBalancer> loadBalancer) {
      this.loadBalancer = loadBalancer;
      return this;
    }

    public Builder application(InteractiveApplication application) {
      this.application = application;
      return this;
    }

    @Override
    public InteractiveTask build() {
      if (simulation == null) { throw new IllegalStateException("No simulation."); }
      return new InteractiveTask(this);
    }

  }

}
