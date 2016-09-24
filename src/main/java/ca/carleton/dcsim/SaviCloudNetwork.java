package ca.carleton.dcsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.carleton.dcsim.managment.capabilities.ApplicationManager;
import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Rack;
import edu.uwo.csd.dcsim.management.AutonomicManager;
import edu.uwo.csd.dcsim.management.capabilities.HostManager;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;
import edu.uwo.csd.dcsim.management.policies.HostMonitoringPolicy;
import edu.uwo.csd.dcsim.management.policies.HostOperationsPolicy;

/**
 * @author Derek Hawker
 */
public class SaviCloudNetwork extends DataCentreManager {

  private final Simulation simulation;
  private final HostPoolManager hostPool;
  private final AutonomicManager autonomicMgr;
  private final List<DataCentre> dataCentres;

  private final Map<Host, DataCentre> hostLocationMap;

  private final ApplicationManager applicationPool;

  private final double[][] latencies;
  private final Map<DataCentre, Integer> dc2index;

  public SaviCloudNetwork(Builder builder) {
    simulation = builder.simulation;
    hostPool = builder.hostPool;
    applicationPool = builder.applicationPool;

    autonomicMgr = builder.autonomicManager;

    dataCentres = new ArrayList<>(builder.dataCentres);

    hostLocationMap = new HashMap<>();
    for (DataCentre dc : dataCentres()) {
      for (Cluster cluster : dc.clusters()) {
        for (Rack rack : cluster.racks()) {
          for (Host host : rack.hosts()) {
            hostLocationMap.put(host, dc);
          }
        }
      }
    }

    latencies = builder.latencies;
    dc2index = builder.dc2index;
  }

  public AutonomicManager getAutonomicManger() {
    return autonomicMgr;
  }

  @Override
  public Map<Host, DataCentre> hostLocationMap() {
    return hostLocationMap;
  }

  public double getLatency(DataCentre dcFrom, DataCentre dcTo) {
    return latencies[dc2index.get(dcFrom)][dc2index.get(dcTo)];
  }

  public List<DataCentre> dataCentres() {
    return dataCentres;
  }

  public ApplicationManager applicationPool() {
    return applicationPool;
  }

  public static class Builder implements ObjectBuilder<SaviCloudNetwork> {

    final Simulation simulation;
    List<DataCentre> dataCentres;
    HostPoolManager hostPool;
    AutonomicManager autonomicManager;
    ApplicationManager applicationPool;

    double[][] latencies;
    Map<DataCentre, Integer> dc2index;

    public Builder(Simulation simulation) {
      if (simulation == null) {
        throw new NullPointerException();
      }
      this.simulation = simulation;
    }

    public Builder datacentres(List<DataCentre> dataCentres) {
      this.dataCentres = dataCentres;
      return this;
    }

    public Builder dc2Index(Map<DataCentre, Integer> dc2index) {
      this.dc2index = dc2index;
      return this;
    }

    public Builder autonomicManager(AutonomicManager autonomicManager) {
      this.autonomicManager = autonomicManager;
      return this;
    }

    public Builder hostPool(HostPoolManager hostPool) {
      this.hostPool = hostPool;
      return this;
    }

    public Builder latencies(double[][] latencies) {
      this.latencies = latencies;
      return this;
    }

    @Override
    public SaviCloudNetwork build() {
      if (dataCentres == null) {
        throw new IllegalStateException("Need to give a list of datacentres.");
      }
      if (hostPool == null) {
        throw new IllegalStateException("Need to give a hostpool manager");
      }

      if (autonomicManager == null) {
        throw new IllegalStateException("Need to give an autonomic manager");
      }

      if (applicationPool == null) {
        throw new IllegalStateException("Need to give an applicationManager");
      }

      if (dc2index == null){
        throw new IllegalStateException("Need to assign Datacentres to index map.");
      }

      // Initialize all hosts. Add to HostPool
      //turn hosts off by default
      for (DataCentre dc : dataCentres) {
        for (Cluster cluster : dc.clusters()) {
          for (Rack rack : cluster.racks()) {
            for (Host host : rack.hosts()) {
              host.setState(Host.HostState.OFF);
              AutonomicManager hostAM = new AutonomicManager(simulation, new HostManager(host));
              hostAM.installPolicy(new HostMonitoringPolicy(autonomicManager), SimTime.seconds(1000), //Jeevithan minutes(5)
            		  SimTime.seconds(1000)+5);//Jeevithan SimTime.minutes(simulation.getRandom().nextInt(4)));
              hostAM.installPolicy(new HostOperationsPolicy());
              host.installAutonomicManager(hostAM);

              hostPool.addHost(host, hostAM);
            }
          }
        }
      }

      return new SaviCloudNetwork(this);
    }


    public Builder applicationPool(ApplicationManager appPool) {
      this.applicationPool = appPool;
      return this;
    }
  }
}
