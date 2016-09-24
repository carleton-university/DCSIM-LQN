package edu.uwo.csd.dcsim;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;

import ca.carleton.dcsim.application.AppTask;
import ca.carleton.dcsim.application.LqnApplication;
import edu.uwo.csd.dcsim.application.InteractiveApplication;
import edu.uwo.csd.dcsim.application.InteractiveTask;
import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.SimulationEventListener;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Link;
import edu.uwo.csd.dcsim.host.Rack;
import edu.uwo.csd.dcsim.host.Switch;
import edu.uwo.csd.dcsim.host.SwitchFactory;
import edu.uwo.csd.dcsim.vm.Vm;
import edu.uwo.csd.dcsim.vm.VmAllocation;

/**
 * Represents a single simulated DataCentre, which consists of a set of Host machines.
 * <p/>
 * Represents a single simulated data centre, which consists of a set of Clusters, connected through
 * two central switches (data and management networks, respectively).
 *
 * @author Michael Tighe
 */
public class DataCentre implements SimulationEventListener {

  private final int id;

  private final List<Cluster> clusters;
  private final Switch dataNetworkSwitch;
  private final Switch mgmtNetworkSwitch;
  private final int hashCode;

  private final Simulation simulation;

  public DataCentre(Builder builder) {
    this(builder.dataNetworkSwitch,
         builder.mgmtNetworkSwitch,
         builder.clusters,
         builder.simulation);
  }

  public DataCentre(Switch dataNetworkSwitch,
                    Switch mgmtNetworkSwitch,
                    List<Cluster> clusters,
                    Simulation simulation) {

    this.simulation = simulation;

    this.id = simulation.nextId(DataCentre.class.toString());

    this.dataNetworkSwitch = dataNetworkSwitch;
    this.mgmtNetworkSwitch = mgmtNetworkSwitch;
    this.clusters = Collections.unmodifiableList(clusters);

    this.hashCode = new HashCodeBuilder()
        .append(id)
        .append(dataNetworkSwitch.hashCode())
        .append(mgmtNetworkSwitch.hashCode())
        .append(clusters.hashCode())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    DataCentre that = (DataCentre) o;

    if (id != that.id) { return false; }
    if (!clusters.equals(that.clusters)) { return false; }
    if (!dataNetworkSwitch.equals(that.dataNetworkSwitch)) { return false; }
    if (!mgmtNetworkSwitch.equals(that.mgmtNetworkSwitch)) { return false; }

    return true;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public void handleEvent(Event e) {
    throw new UnsupportedOperationException("Auto-generated method.");
  }

  /**
   * Log state of the DataCentre
   */
  public void logState() {
    for (Cluster cluster : clusters) {
      for (Rack rack : cluster.racks()) {
        for (Host host : rack.hosts()) {
          host.logState();
        }
      }
    }
  }


  public double getCurrentPowerConsumption() {
    double power = 0;
    for (Cluster cluster : clusters) {
      for (Rack rack : cluster.racks()) {
        for (Host host : rack.hosts()) {
          power += host.getCurrentPowerConsumption();
        }
      }
    }
    return power;
  }

  public int numHosts() {
    int numHosts = 0;
    for (Cluster cluster : clusters) {
      for (Rack rack : cluster.racks()) {
        numHosts += rack.getHostCount();
      }
    }

    return numHosts;
  }
  public int getWorkload() {
    for (Cluster cluster : clusters) {
      for (Rack rack : cluster.racks()) {
        for (Host host : rack.hosts()) {

          for (VmAllocation vmAlloc : host.getVMAllocations()) {
            Vm vm = vmAlloc.getVm();
            if (vm == null) { continue; }

            TaskInstance ti = vm.getTaskInstance();
            if (ti == null) { continue; }
            Task task = ti.getTask();
            if (task == null) { continue; }

            if (task instanceof AppTask) {
              return ((LqnApplication) task.getApplication())
                  .getWorkload().getWorkOutputLevel();
            } else if (task instanceof InteractiveTask) {
              return ((InteractiveApplication) task.getApplication())
                  .getWorkload().getWorkOutputLevel();
            }
          }
        }
      }
    }

    return -1;
  }

  public double getResponseTime() {
    for (Cluster cluster : clusters) {
      for (Rack rack : cluster.racks()) {
        for (Host host : rack.hosts()) {

          for (VmAllocation vmAlloc : host.getVMAllocations()) {
            Vm vm = vmAlloc.getVm();
            if (vm == null) { continue; }

            TaskInstance ti = vm.getTaskInstance();
            if (ti == null) { continue; }
            Task task = ti.getTask();
            if (task == null) { continue; }

            if (task instanceof AppTask) {
              return ((LqnApplication) task.getApplication())
                  .getResponseTime();
            } else if (task instanceof InteractiveTask) {
              return ((InteractiveApplication) task.getApplication())
                  .getResponseTime();
            }
          }
        }
      }
    }

    return -1;
  }

  /**
   * Build a Datacener
   */
  public static class Builder implements ObjectBuilder<DataCentre> {

    final Simulation simulation;
    private List<Cluster> clusters;
    private Switch dataNetworkSwitch;
    private Switch mgmtNetworkSwitch;

    public Builder(Simulation simulation) {
      this.simulation = simulation;
    }


    @Override
    public DataCentre build() {
      if (dataNetworkSwitch == null || mgmtNetworkSwitch == null) {
        throw new IllegalStateException("must provide a switchfactory.");
      }

      if (clusters == null) {
        throw new IllegalStateException("must provide clusters.");
      }

      return new DataCentre(this);
    }

    public Builder switchFactory(SwitchFactory switchFactory) {
      this.dataNetworkSwitch = switchFactory.newInstance();
      this.mgmtNetworkSwitch = switchFactory.newInstance();

      return this;
    }

    public Builder clusters(List<Cluster> clusters) {
      if (dataNetworkSwitch == null || mgmtNetworkSwitch == null) {
        throw new IllegalStateException("Must provide switchFactory first.");
      }

      this.clusters = clusters;
      for (Cluster cluster : clusters) {
        addCluster(cluster);
      }
      return this;
    }

    /**
     * Adds a cluster to the data centre, connecting it to both data and management networks.
     * <p/>
     * It also adds the Hosts in the Cluster to the DataCentre (required for the Simulation class to
     * work properly).
     */
    private void addCluster(Cluster cluster) {
      // Set Data Network.
      Switch clusterSwitch = cluster.getMainDataSwitch();
      Link link = new Link(clusterSwitch, dataNetworkSwitch);
      clusterSwitch.setUpLink(link);
      dataNetworkSwitch.addPort(link);

      // Set Management Network.
      clusterSwitch = cluster.getMainMgmtSwitch();
      link = new Link(clusterSwitch, mgmtNetworkSwitch);
      clusterSwitch.setUpLink(link);
      mgmtNetworkSwitch.addPort(link);
    }
  }

  public List<Cluster> clusters() {
    return clusters;
  }

  public Switch getDataNetworkSwitch() {
    return dataNetworkSwitch;
  }

  public Switch getMgmtNetworkSwitch() {
    return mgmtNetworkSwitch;
  }

  public int id() {
    return this.id;
  }
}
