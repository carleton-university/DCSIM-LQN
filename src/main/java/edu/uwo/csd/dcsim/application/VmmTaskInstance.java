package edu.uwo.csd.dcsim.application;

import org.apache.commons.lang3.builder.HashCodeBuilder;


/**
 * @author Michael Tighe
 */
public class VmmTaskInstance extends TaskInstance {

  private VmmTask task;
  private final int hashCode;

  public VmmTaskInstance(VmmTask task) {
    super(task.simulation);
    this.task = task;

    //init hashCode
    hashCode = new HashCodeBuilder()
        .append(task.getId())
        .append(task.getInstances().size())
        .build();
  }

  @Override
  public void postScheduling() {

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
