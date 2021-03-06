package edu.uwo.csd.dcsim.core;

import java.util.ArrayList;
import java.util.List;

public class SimulationEventBroadcastGroup implements SimulationEventListener {

  private List<SimulationEventListener> members = new ArrayList<SimulationEventListener>();

  public void addMember(SimulationEventListener member) {
    members.add(member);
  }

  public void removeMember(SimulationEventListener member) {
    members.remove(member);
  }

  public long size() {
    return members.size();
  }

  @Override
  public void handleEvent(Event e) {
    for (SimulationEventListener member : members) {
      member.handleEvent(e);
    }
  }

}
