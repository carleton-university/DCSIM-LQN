package edu.uwo.csd.dcsim.management.events;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.management.AutonomicManager;

public class ApplicationPlacementEvent extends Event {

  private List<Application> applications;
  private boolean failed = false;

  public ApplicationPlacementEvent(AutonomicManager target, Application application) {
    super(target);

    applications = new ArrayList<Application>();
    applications.add(application);
  }

  public ApplicationPlacementEvent(AutonomicManager target, List<Application> applications) {
    super(target);

    this.applications = applications;
  }

  public List<Application> getApplications() {
    return applications;
  }

  public void setFailed(boolean failed) {
    this.failed = failed;
  }

  public boolean isFailed() {
    return failed;
  }

}
