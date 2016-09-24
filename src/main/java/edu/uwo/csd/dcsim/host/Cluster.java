package edu.uwo.csd.dcsim.host;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.SimulationEventListener;

/**
 * A cluster within a data centre. Encompasses a set of racks and two sets of switches (data and
 * management networks, respectively).
 *
 * @author Gaston Keller
 */
public final class Cluster implements SimulationEventListener {
  private int id = 0;

  private final List<Rack> racks;          // List of racks.

  private final List<Switch> mgmtSwitches;      // Management network switches.
  private final List<Switch> dataSwitches;      // Data network switches.
  private final Switch mainDataSwitch;          // Data network main (top-level) switch.
  private final Switch mainMgmtSwitch;          // Management network main (top-level) switch.

  public enum ClusterState {
    ON, SUSPENDED, OFF;

  }

  private ClusterState state;

  private final int hashCode;

  private Cluster(Builder builder) {

    this.id = builder.simulation.nextId(Cluster.class.toString());

    int nSwitches = builder.nSwitches;

    this.dataSwitches = new ArrayList<>(nSwitches);
    this.mgmtSwitches = new ArrayList<>(nSwitches);
    for (int i = 0; i < nSwitches; i++) {
      this.dataSwitches.add(builder.switchFactory.newInstance());
      this.mgmtSwitches.add(builder.switchFactory.newInstance());
    }

    this.racks = Collections.unmodifiableList(builder.racks);
    for (int i = 0; i < racks.size(); i++) {
      Rack rack = racks.get(i);

      // Set Data Network.
      Switch rackSwitch = rack.dataNetworkSwitch();
      Link link = new Link(rackSwitch, dataSwitches.get(i % nSwitches));
      rackSwitch.setUpLink(link);
      dataSwitches.get(i % nSwitches).addPort(link);

      // Set Management Network.
      rackSwitch = rack.mgmtNetworkSwitch();
      link = new Link(rackSwitch, mgmtSwitches.get(i % nSwitches));
      rackSwitch.setUpLink(link);
      mgmtSwitches.get(i % nSwitches).addPort(link);

    }

    // Complete network(s) layout.
    // If there is only one switch, identify it as main (top-level) for
    // the network.
    // Otherwise, add a central, higher-level switch and connect all other
    // switches to it -- star topology.

    if (nSwitches == 1) {
      mainDataSwitch = dataSwitches.get(0);
      mainMgmtSwitch = mgmtSwitches.get(0);
    } else {
      mainDataSwitch = builder.switchFactory.newInstance();
      mainMgmtSwitch = builder.switchFactory.newInstance();

      for (int i = 0; i < nSwitches; i++) {
        // Set Data Network.
        Switch aSwitch = dataSwitches.get(i);
        Link link = new Link(aSwitch, mainDataSwitch);
        aSwitch.setUpLink(link);
        mainDataSwitch.addPort(link);

        // Set Management Network.
        aSwitch = mgmtSwitches.get(i);
        link = new Link(aSwitch, mainMgmtSwitch);
        aSwitch.setUpLink(link);
        mainMgmtSwitch.addPort(link);
      }
    }

    // Set default state.
    state = ClusterState.OFF;

    //init hashCode
    hashCode = new HashCodeBuilder()
        .append(id)
        .append(racks.hashCode())
        .append(mgmtSwitches.hashCode())
        .append(dataSwitches.hashCode())
        .build();
  }

  /**
   * Builds a new Cluster object. This is the only way to instantiate Cluster.
   *
   * @author Gaston Keller
   */
  public static class Builder implements ObjectBuilder<Cluster> {

    final Simulation simulation;

    int nSwitches = 0;

    SwitchFactory switchFactory = null;
    List<Rack> racks;

    public Builder(Simulation simulation) {
      if (simulation == null) {
        throw new NullPointerException();
      }
      this.simulation = simulation;
    }

    public Builder nSwitches(int val) {
      this.nSwitches = val;
      return this;
    }

    public Builder racks(List<Rack> racks) {
      this.racks = racks;
      return this;
    }

    public Builder switchFactory(SwitchFactory switchFactory) {
      this.switchFactory = switchFactory;
      return this;
    }


    @Override
    public Cluster build() {

      if (racks == null) {
        throw new IllegalStateException("racks must be set");
      }
      if (racks.size() == 0 || nSwitches == 0) {
        throw new IllegalStateException(
            "Must specify number of racks and switches before building Cluster.");
      }
      if (switchFactory == null) {
        throw new IllegalStateException("Must specify Switch factory before building Cluster.");
      }

      return new Cluster(this);
    }

  }

  public double getCurrentPowerConsumption() {
    double power = 0;

    for (Rack rack : racks) {
      power += rack.getCurrentPowerConsumption();
    }

    // Add power consumption of the Cluster's Switches.
    power += mainDataSwitch.getPowerConsumption();
    power += mainMgmtSwitch.getPowerConsumption();
    if (getSwitchCount() > 1) {        // Star topology.
      for (Switch s : dataSwitches) {
        power += s.getPowerConsumption();
      }
      for (Switch s : mgmtSwitches) {
        power += s.getPowerConsumption();
      }
    }

    return power;
  }

  public void updateState() {
    // Calculate number of active and suspended Racks -- the rest are powered-off.
    int activeRacks = 0;
    int suspendedRacks = 0;
    for (Rack rack : racks) {
      if (rack.getState() == Rack.RackState.ON) {
        activeRacks++;
      } else if (rack.getState() == Rack.RackState.SUSPENDED) {
        suspendedRacks++;
      }
    }

    // Determine Rack's current state.
    if (activeRacks > 0) {
      state = ClusterState.ON;
    } else if (suspendedRacks > 0) {
      state = ClusterState.SUSPENDED;
    } else {
      state = ClusterState.OFF;
    }
  }

  @Override
  public void handleEvent(Event e) {
    throw new IllegalStateException("Not implemented.");
  }

  // Accessor and mutator methods.

  public int id() {
    return id;
  }

  /** Number of racks in the cluster. */
  public int getRackCount() {
    return racks.size();
  }

  /** Number of switches in the cluster. */
  public int getSwitchCount() {
    return dataSwitches.size();
  }

  public List<Rack> racks() {
    return racks;
  }

  public List<Switch> getDataSwitches() {
    return new ArrayList<>(dataSwitches);
  }

  public List<Switch> getMgmtSwitches() {
    return new ArrayList<>(mgmtSwitches);
  }

  public Switch getMainDataSwitch() {
    return mainDataSwitch;
  }

  public Switch getMainMgmtSwitch() {
    return mainMgmtSwitch;
  }

  public ClusterState getState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    Cluster cluster = (Cluster) o;

    if (id != cluster.id) { return false; }
    if (!dataSwitches.equals(cluster.dataSwitches)) { return false; }
    if (!mgmtSwitches.equals(cluster.mgmtSwitches)) { return false; }
    if (!racks.equals(cluster.racks)) { return false; }

    return true;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}
