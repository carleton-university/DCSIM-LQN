package edu.uwo.csd.dcsim.core.metrics;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.uwo.csd.dcsim.common.Utility;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Rack;

/**
 * This class handles the collection of metrics for Racks and Clusters in a Data Centre.
 * <p/>
 * Perhaps in the future, this class should be merged with HostMetrics.
 *
 * @author Gaston Keller
 */
public class ClusterMetrics extends MetricCollection {

  WeightedMetric powerConsumption = new WeightedMetric();
  WeightedMetric powerEfficiency = new WeightedMetric();

  WeightedMetric activeRacks = new WeightedMetric();
  //WeightedMetric rackUtilization = new WeightedMetric();
  WeightedMetric activeHostsPerRack = new WeightedMetric();

  WeightedMetric activeClusters = new WeightedMetric();
  //WeightedMetric clusterUtilization = new WeightedMetric();
  WeightedMetric activeRacksPerCluster = new WeightedMetric();

  long nRacks;
  long nClusters;

  public ClusterMetrics(Simulation simulation) {
    super(simulation);
  }

  @Override
  public void recordClusterMetrics(Collection<Cluster> clusters) {
    double currentPowerConsumption = 0;
    double currentTotalCpuInUse = 0;
    double currentActiveRacks = 0;
    double currentActiveClusters = 0;

    nRacks = 0;
    nClusters = clusters.size();

    for (Cluster cluster : clusters) {
      nRacks += cluster.getRackCount();
      currentPowerConsumption += cluster.getCurrentPowerConsumption();

      if (cluster.getState() == Cluster.ClusterState.ON) {
        currentActiveClusters++;

        int activeRacksInCluster = 0;
        for (Rack rack : cluster.racks()) {
          if (rack.getState() == Rack.RackState.ON) {
            activeRacksInCluster++;

            // Calculate number of active Hosts.
            int activeHosts = 0;
            for (Host host : rack.hosts()) {
              if (host.getState() == Host.HostState.ON) {
                activeHosts++;
              }

              currentTotalCpuInUse += host.getResourceManager().getCpuInUse();
            }

            activeHostsPerRack.add(activeHosts, simulation.getElapsedTime());
          }
        }

        activeRacksPerCluster.add(activeRacksInCluster, simulation.getElapsedTime());
        currentActiveRacks += activeRacksInCluster;
      }
    }

    powerConsumption.add(currentPowerConsumption, simulation.getElapsedSeconds());
    powerEfficiency
        .add(currentTotalCpuInUse / currentPowerConsumption, simulation.getElapsedSeconds());
    activeRacks.add(currentActiveRacks, simulation.getElapsedTime());
    activeClusters.add(currentActiveClusters, simulation.getElapsedTime());
  }

  public WeightedMetric getPowerConsumption() {
    return powerConsumption;
  }

  public WeightedMetric getPowerEfficiency() {
    return powerEfficiency;
  }

  public WeightedMetric getActiveRacks() {
    return activeRacks;
  }

//	public WeightedMetric getRackUtilization() {
//		return rackUtilization;
//	}

  public WeightedMetric getActiveHostsPerRack() {
    return activeHostsPerRack;
  }

  public WeightedMetric getActiveClusters() {
    return activeClusters;
  }

//	public WeightedMetric getClusterUtilization() {
//		return clusterUtilization;
//	}

  public WeightedMetric getActiveRacksPerCluster() {
    return activeRacksPerCluster;
  }

  @Override
  public void completeSimulation() {
    // TODO Auto-generated method stub
  }

  @Override
  public void printDefault(Logger out) {
    out.info("-- CLUSTERS --");
    out.info("   nRacks: " + nRacks);
    out.info("   nClusters: " + nClusters);

    out.info("Active Racks");
    out.info("   max: " + Utility
        .roundDouble(getActiveRacks().getMax(), Simulation.getMetricPrecision()));
    out.info("   mean: " + Utility
        .roundDouble(getActiveRacks().getMean(), Simulation.getMetricPrecision()));
    out.info("   min: " + Utility
        .roundDouble(getActiveRacks().getMin(), Simulation.getMetricPrecision()));
    out.info("   Active Hosts Per Rack");
    out.info("      max: " + Utility
        .roundDouble(getActiveHostsPerRack().getMax(), Simulation.getMetricPrecision()));
    out.info("      mean: " + Utility
        .roundDouble(getActiveHostsPerRack().getMean(), Simulation.getMetricPrecision()));
    out.info("      min: " + Utility
        .roundDouble(getActiveHostsPerRack().getMin(), Simulation.getMetricPrecision()));

    out.info("Active Clusters");
    out.info("   max: " + Utility
        .roundDouble(getActiveClusters().getMax(), Simulation.getMetricPrecision()));
    out.info("   mean: " + Utility
        .roundDouble(getActiveClusters().getMean(), Simulation.getMetricPrecision()));
    out.info("   min: " + Utility
        .roundDouble(getActiveClusters().getMin(), Simulation.getMetricPrecision()));
    out.info("   Active Racks Per Cluster");
    out.info("      max: " + Utility
        .roundDouble(getActiveRacksPerCluster().getMax(), Simulation.getMetricPrecision()));
    out.info("      mean: " + Utility
        .roundDouble(getActiveRacksPerCluster().getMean(), Simulation.getMetricPrecision()));
    out.info("      min: " + Utility
        .roundDouble(getActiveRacksPerCluster().getMin(), Simulation.getMetricPrecision()));

    out.info("Power");
    out.info("   consumed: " + Utility
        .roundDouble(Utility.toKWH(getPowerConsumption().getSum()), Simulation.getMetricPrecision())
             + "kWh");
    out.info("   max: " + Utility
        .roundDouble(getPowerConsumption().getMax(), Simulation.getMetricPrecision()) + "Ws");
    out.info("   mean: " + Utility
        .roundDouble(getPowerConsumption().getMean(), Simulation.getMetricPrecision()) + "Ws");
    out.info("   min: " + Utility
        .roundDouble(getPowerConsumption().getMin(), Simulation.getMetricPrecision()) + "Ws");
    out.info("   efficiency: " + Utility
        .roundDouble(getPowerEfficiency().getMean(), Simulation.getMetricPrecision()) + "cpu/watt");
  }

  @Override
  public List<Pair<String, Object>> getMetricValues() {
    ArrayList<Pair<String, Object>> metrics = new ArrayList<>();

    metrics.add(new ImmutablePair<String, Object>("nRacks", nRacks));
    metrics.add(new ImmutablePair<String, Object>("nClusters", nClusters));

    metrics.add(new ImmutablePair<String, Object>("activeRacksMax", Utility
        .roundDouble(getActiveRacks().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeRacksMean", Utility
        .roundDouble(getActiveRacks().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeRacksMin", Utility
        .roundDouble(getActiveRacks().getMin(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeHostsPerRackMax", Utility
        .roundDouble(getActiveHostsPerRack().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeHostsPerRackMean", Utility
        .roundDouble(getActiveHostsPerRack().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeHostsPerRackMin", Utility
        .roundDouble(getActiveHostsPerRack().getMin(), Simulation.getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("activeClustersMax", Utility
        .roundDouble(getActiveClusters().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeClustersMean", Utility
        .roundDouble(getActiveClusters().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeClustersMin", Utility
        .roundDouble(getActiveClusters().getMin(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeRacksPerClusterMax", Utility
        .roundDouble(getActiveRacksPerCluster().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeRacksPerClusterMean", Utility
        .roundDouble(getActiveRacksPerCluster().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("activeRacksPerClusterMin", Utility
        .roundDouble(getActiveRacksPerCluster().getMin(), Simulation.getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("powerConsumed", Utility
        .roundDouble(Utility.toKWH(getPowerConsumption().getSum()),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("powerMax", Utility
        .roundDouble(getPowerConsumption().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("powerMean", Utility
        .roundDouble(getPowerConsumption().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("powerMin", Utility
        .roundDouble(getPowerConsumption().getMin(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("powerEfficiency", Utility
        .roundDouble(getPowerEfficiency().getMean(), Simulation.getMetricPrecision())));

    return metrics;
  }
}
