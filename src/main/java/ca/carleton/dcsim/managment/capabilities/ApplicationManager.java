package ca.carleton.dcsim.managment.capabilities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ca.carleton.lqn.LqnGraph;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.management.capabilities.ManagerCapability;

/**
 * Provides access to applications running in the cloud
 *
 * @author Derek Hawker
 */
public class ApplicationManager extends ManagerCapability {

  private LqnGraph lqnModel;
  protected Map<Integer, Application> applicationMap = new HashMap<>();

  public void addApplication(Application application) {
    applicationMap.put(application.id(), application);
  }

  public void setLqnModel(LqnGraph lqnModel) {
    this.lqnModel = lqnModel;
  }

  public Collection<Application> getApplications() {
    return applicationMap.values();
  }

  public Application getHost(int id) {
    return applicationMap.get(id);
  }

  public LqnGraph getLqnModel() { return lqnModel;}
}
