package edu.uwo.csd.dcsim.application;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import edu.uwo.csd.dcsim.core.Simulation;

/**
 * @author Michael Tighe
 */
public class InteractiveTaskInstance extends TaskInstance {

  private final InteractiveTask task;
  private double effectiveServiceTime;
  private double queueLength;
  private double responseTime;
  private double throughput;
  private double utilization;
  private DescriptiveStatistics utilizationDeltas = new DescriptiveStatistics(5);
  private double visitRatio;

  private final int hashCode;

  public InteractiveTaskInstance(InteractiveTask task) {
    super(task.simulation);
    this.task = task;

    //init hashCode
    hashCode = new HashCodeBuilder()
        .append(task.getId())
        .build();
  }

  @Override
  public void postScheduling() {
    //nothing to do
  }

  public double getServiceTime() {
    double
        serviceTime =
        task.getNormalServiceTime() * (task.getResourceSize().cpu() /
                                       (float) vm.getVMAllocation().host().coreCapacity());
    if (vm.isMigrating()) {
      serviceTime +=
          serviceTime * Float.parseFloat(Simulation.getProperty("vmMigrationServiceTimePenalty"));
    }

    return serviceTime;
  }

  public double getEffectiveServiceTime() {
    return effectiveServiceTime;
  }

  public void setEffectiveServiceTime(double effectiveServiceTime) {
    this.effectiveServiceTime = effectiveServiceTime;
  }

  public double getQueueLength() {
    return queueLength;
  }

  public void setQueueLength(double queueLength) {
    this.queueLength = queueLength;
  }

  public double getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(double responseTime) {
    this.responseTime = responseTime;
  }

  public double getThroughput() {
    return throughput;
  }

  public void setThroughput(double throughput) {
    this.throughput = throughput;
  }

  public double getUtilization() {
    return utilization;
  }

  public void setUtilization(double utilization) {
    this.utilization = utilization;
  }

  public DescriptiveStatistics getUtilizationDeltas() {
    return utilizationDeltas;
  }

  public void updateVisitRatio() {
    visitRatio = task.getVisitRatio() * task.getLoadBalancer().getInstanceShare(this);
  }

  public double getVisitRatio() {
    return visitRatio;
  }

  @Override
  public Task getTask() {
    return task;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}
