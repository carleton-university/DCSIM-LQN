package edu.uwo.csd.dcsim.management.capabilities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.management.AutonomicManager;
import edu.uwo.csd.dcsim.management.HostData;

public class HostPoolManager extends ManagerCapability {

  protected Map<Integer, HostData> hostMap = new HashMap<Integer, HostData>();

  public void addHost(Host host, AutonomicManager hostManager) {
    hostMap.put(host.id(), new HostData(host, hostManager));
  }

  public Collection<HostData> getHosts() {
    return hostMap.values();
  }

  public HostData getHost(int id) {
    return hostMap.get(id);
  }

}
