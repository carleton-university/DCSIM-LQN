package ca.carleton.dcsim.application;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.core.Simulation;

/**
 * @author Michael Tighe
 */
public class AppInstance extends TaskInstance {
  private final AppTask task;

  private final int hashCode;

  public AppInstance(AppTask task, Simulation simulation) {
    super(simulation);
    if (task == null) {
      throw new IllegalArgumentException("task is null");
    }

    this.task = task;
    //init hashCode
    hashCode = new HashCodeBuilder()
        .append(id)
        .append(task.getId())
        .build();
  }

  @Override
  public void postScheduling() {
    //nothing to do
  }

  @Override
  public Task getTask() {
    return task;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AppInstance)) {
      return false;
    }
    AppInstance that = (AppInstance) o;
    return isEqual(that);
  }

  private boolean isEqual(AppInstance that) {
    return task.getId() == that.task.getId() &&
           task.getInstances().size() == that.task.getInstances().size();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}
