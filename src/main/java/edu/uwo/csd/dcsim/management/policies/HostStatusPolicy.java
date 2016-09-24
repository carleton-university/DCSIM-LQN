package edu.uwo.csd.dcsim.management.policies;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.management.HostData;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;
import edu.uwo.csd.dcsim.management.events.HostStatusEvent;

public class HostStatusPolicy extends Policy {

  List<Class<? extends Event>> triggerEvents = new ArrayList<Class<? extends Event>>();

  private int windowSize;

  /**
   * @param windowSize number of previous datapoints to keep in history.
   */
  public HostStatusPolicy(int windowSize) {
    addRequiredCapability(HostPoolManager.class);

    this.windowSize = windowSize;
  }

  public void execute(HostStatusEvent event) {
    HostPoolManager hostPool = manager.getCapability(HostPoolManager.class);

    HostData hostData = hostPool.getHost(event.getHostStatus().getId());
    hostData.addHostStatus(event.getHostStatus(), windowSize);
  }

  @Override
  public void onInstall() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onManagerStart() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onManagerStop() {
    // TODO Auto-generated method stub

  }

}
