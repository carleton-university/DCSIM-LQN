package edu.uwo.csd.dcsim.host;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;

import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.SimulationEventListener;

/**
 * A rack within a data centre. Hosts a collection of blades or servers and two switches (data and
 * management networks, respectively).
 *
 * @author Gaston Keller
 */
public final class Rack implements SimulationEventListener, Comparable<Rack> {

  private final Simulation simulation;

  private int id = 0;
  private int nSlots = 0;                // Number of hosts that can be hosted in the rack.

  private final List<Host> hosts;          // List of hosts.

  private final Switch dataNetworkSwitch;        // Data network switch.
  private final Switch mgmtNetworkSwitch;        // Management network switch.

  private final int hashCode;

  public enum RackState {
    ON, SUSPENDED, OFF
  }

  private RackState state;

  private Rack(Builder builder) {

    this.simulation = builder.simulation;

    this.id = simulation.nextId(Rack.class.toString());

    this.nSlots = builder.nSlots;

    this.dataNetworkSwitch = builder.switchFactory.newInstance();
    this.mgmtNetworkSwitch = builder.switchFactory.newInstance();

    this.hosts = Collections.unmodifiableList(builder.hosts);
    for (Host host : hosts) {

      // Set Data Network.
      NetworkCard networkCard = host.getDataNetworkCard();
      Link link = new Link(networkCard, dataNetworkSwitch);
      networkCard.setLink(link);
      dataNetworkSwitch.addPort(link);

      // Set Management Network.
      networkCard = host.getMgmtNetworkCard();
      link = new Link(networkCard, mgmtNetworkSwitch);
      networkCard.setLink(link);
      mgmtNetworkSwitch.addPort(link);
    }

    // Set default state.
    state = RackState.OFF;

    /* ************************************************************
    Object must be in final state before running the below code.

    Init hashCode */
    hashCode = new HashCodeBuilder()
        .append(id)
        .append(nSlots)
        .append(dataNetworkSwitch.hashCode())
        .append(mgmtNetworkSwitch.hashCode())
        .build();
  }

  /**
   * Builds a new Rack object. This is the only way to instantiate Rack.
   *
   * @author Gaston Keller
   */
  public static class Builder implements ObjectBuilder<Rack> {

    private final Simulation simulation;

    private int nSlots = 0;

    private SwitchFactory switchFactory = null;
    private List<Host> hosts;

    public Builder(Simulation simulation) {
      if (simulation == null) {
        throw new NullPointerException();
      }
      this.simulation = simulation;
    }

    public Builder nSlots(int val) {
      this.nSlots = val;
      return this;
    }

    public Builder switchFactory(SwitchFactory switchFactory) {
      this.switchFactory = switchFactory;
      return this;
    }

    @Override
    public Rack build() {

      if (hosts == null) {
        throw new IllegalStateException("Must give a list of hosts.");
      }
      if (nSlots == 0 || hosts.size() == 0) {
        throw new IllegalStateException("Must specify Rack's capacity before building Rack.");
      }
      if (hosts.size() > nSlots) {
        throw new IllegalStateException("Number of hosts exceeds Rack's slot capacity.");
      }
      if (switchFactory == null) {
        throw new IllegalStateException("Must specify Switch factory before building Rack.");
      }

      return new Rack(this);
    }

    public Builder hosts(List<Host> hosts) {
      this.hosts = hosts;
      return this;
    }
  }

  public double getCurrentPowerConsumption() {
    double power = 0;

    for (Host host : hosts) {
      power += host.getCurrentPowerConsumption();
    }

    power += dataNetworkSwitch.getPowerConsumption();
    power += mgmtNetworkSwitch.getPowerConsumption();

    return power;
  }

  public void updateState() {
    RackState oldState = state;

    // Calculate number of active and suspended Hosts -- the rest are powered-off.
    int activeHosts = 0;
    int suspendedHosts = 0;
    for (Host host : hosts) {
      Host.HostState state = host.getState();
      if (state == Host.HostState.ON ||
          state == Host.HostState.POWERING_ON ||
          state == Host.HostState.SUSPENDING ||
          state == Host.HostState.POWERING_OFF)

      {
        activeHosts++;
      } else if (state == Host.HostState.SUSPENDED) {
        suspendedHosts++;
      }
      // ELSE Host is OFF or FAILED
    }

    // Determine Rack's current state.
    if (activeHosts > 0) {
      state = RackState.ON;
    } else if (suspendedHosts > 0) {
      state = RackState.SUSPENDED;
    } else {
      state = RackState.OFF;
    }

    // If there was a change in state, update parent Cluster's state.
    if (state != oldState) {
      simulation.rack2Cluster(this).updateState();
    }
  }

  @Override
  public void handleEvent(Event e) {
    throw new UnsupportedOperationException("Auto-generated Method");
  }

  // Accessor and mutator methods.

  public int id() {
    return id;
  }

  public int getHostCount() {
    return hosts.size();
  }

  public int getSlotCount() {
    return nSlots;
  }

  public List<Host> hosts() {
    return hosts;
  }

  public Switch dataNetworkSwitch() {
    return dataNetworkSwitch;
  }

  public Switch mgmtNetworkSwitch() {
    return mgmtNetworkSwitch;
  }

  public RackState getState() {
    return state;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    Rack rack = (Rack) o;

    if (id != rack.id) { return false; }
    if (nSlots != rack.nSlots) { return false; }
    if (!dataNetworkSwitch.equals(rack.dataNetworkSwitch)) { return false; }
    if (!mgmtNetworkSwitch.equals(rack.mgmtNetworkSwitch)) { return false; }

    return true;
  }
  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public int compareTo(Rack arg0) {
    return arg0.id - id;
  }
}
