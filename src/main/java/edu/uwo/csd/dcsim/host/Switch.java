package edu.uwo.csd.dcsim.host;

import java.util.ArrayList;
import java.util.List;

/**
 * A network switch within a data centre.
 *
 * @author Gaston Keller
 */
public class Switch implements NetworkingElement {

  private int bandwidth = 0;                        // in KB
  private int nPorts = 0;
  private int power = 0;

  private Link upLink = null;
  private List<Link> ports = null;

  /**
   * Creates an instance of Switch.
   */
  public Switch(int bandwidth, int ports, int power) {
    this.bandwidth = bandwidth;
    this.nPorts = ports;
    this.power = power;
    this.ports = new ArrayList<>(ports);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    Switch aSwitch = (Switch) o;

    if (bandwidth != aSwitch.bandwidth) { return false; }
    if (nPorts != aSwitch.nPorts) { return false; }
    if (power != aSwitch.power) { return false; }

    return true;
  }

  @Override
  public int hashCode() {
    int result = bandwidth;
    result = 31 * result + nPorts;
    result = 31 * result + power;
    return result;
  }

  // Accessor and mutator methods.
  public int getBandwidth() {
    return bandwidth;
  }

  public int getPortCount() {
    return nPorts;
  }

  public int getPowerConsumption() {
    return power;
  }

  public Link getUpLink() {
    return upLink;
  }

  public void setUpLink(Link upLink) {
    this.upLink = upLink;
  }

  public List<Link> getPorts() {
    return ports;
  }

  public void setPorts(List<Link> ports) throws Exception {
    if (ports.size() < nPorts) {
      this.ports = ports;
    } else {
      throw new RuntimeException("Port capacity exceeded in Switch.");
    }
  }

  public void addPort(Link port) {
    if (ports.size() < nPorts) {
      ports.add(port);
    } else {
      throw new RuntimeException("Port capacity exceeded in Switch.");
    }
  }

}
