package ca.carleton.dcsim.application;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Map;

import ca.carleton.dcsim.SaviCloudNetwork;
import ca.carleton.lqn.Latency;
import ca.carleton.lqn.LqnGraph;
import ca.carleton.lqn.core.calls.EntryCall;
import ca.carleton.lqn.core.entries.EntryDescription;
import ca.carleton.lqn.core.tasks.TaskDescription;
import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Rack;

/**
 * @author Derek Hawker
 */
public class DcsimLqnUtilities {

  public static Table<EntryDescription, EntryDescription, Double>
  buildEntryLatencies(Simulation simulation,
                      LqnGraph lqnGraph,
                      Map<TaskDescription, Task> lqn2DcSimTaskMap,
                      Map<TaskInstance, TaskDescription> instance2replication) {
    Table<EntryDescription, EntryDescription, Double> latencies = HashBasedTable.create();

    lqnGraph.entryCalls().entries().stream()
        .filter(ed -> !ed.getKey().equals(lqnGraph.referenceEntry()))
        .forEach(ecs -> {
          EntryDescription entryFrom = ecs.getKey();
          EntryCall call = ecs.getValue();
          EntryDescription entryTo = call.callTo();

          Task entryFromTask = lqn2DcSimTaskMap.get(
              lqnGraph.owningTaskOf(entryFrom));
          Task entryToTask = lqn2DcSimTaskMap.get(
              lqnGraph.owningTaskOf(entryTo));

          Host entryFromHost = entryFromTask.getInstances().get(0)
              .getVM().getVMAllocation().host();
          Host entryToHost = entryToTask.getInstances().get(0)
              .getVM().getVMAllocation().host();

          if (entryFromHost != entryToHost) {
            double taskLatency = calculateTaskLatency(simulation, entryFromHost, entryToHost);
            latencies.put(entryFrom, entryTo, taskLatency);
          }
        });

    return latencies;
  }

  public static Table<EntryDescription, EntryDescription, Double>
  buildLatencies(Simulation simulation,
                 LqnGraph lqnGraph,
                 BiMap<TaskInstance, TaskDescription> instance2replication) {
    Table<EntryDescription, EntryDescription, Double> latencies = HashBasedTable.create();

    lqnGraph.entryCalls().entries().stream()
        .filter(ed -> !ed.getKey().equals(lqnGraph.referenceEntry()))
        .forEach(ecs -> {
          EntryDescription entryFrom = ecs.getKey();
          EntryCall call = ecs.getValue();
          EntryDescription entryTo = call.callTo();

          TaskDescription taskFrom = lqnGraph.owningTaskOf(entryFrom);
          TaskDescription taskTo = lqnGraph.owningTaskOf(entryTo);

          BiMap<TaskDescription, TaskInstance> inverse = instance2replication.inverse();
          Host hostFrom = inverse.get(taskFrom).getVM().getVMAllocation().host();
          Host hostTo = inverse.get(taskTo).getVM().getVMAllocation().host();

          if (hostFrom != hostTo) {
            double taskLatency = calculateTaskLatency(simulation, hostFrom,hostTo);
            latencies.put(entryFrom, entryTo, taskLatency);
          }
        });

    return latencies;
  }

  public static double taskLatency(Simulation simulation,
                                   TaskInstance from,
                                   TaskInstance to) {
    Host entryFromHost = from.getVM().getVMAllocation().host();
    Host entryToHost = to.getVM().getVMAllocation().host();

    if (entryFromHost != entryToHost) {
      return calculateTaskLatency(simulation, entryFromHost, entryToHost);
    } else {
      return 0.0;
    }
  }

  private static double calculateTaskLatency(Simulation simulation,
                                             Host entryFromHost,
                                             Host entryToHost) {
    if (entryFromHost.equals(entryToHost)) {
      // don't add a thinktime/latency entry because they are both on the same host.
      return 0;
    }

    Rack entryFromRack = simulation.host2Rack(entryFromHost);
    Rack entryToRack = simulation.host2Rack(entryToHost);
    if (entryFromRack.equals(entryToRack)) {
      // not on the same host, but in the same rack.
      return Latency.INTRA_RACK_DELAY;
    }

    Cluster entryFromCluster = simulation.rack2Cluster(entryFromRack);
    Cluster entryToCluster = simulation.rack2Cluster(entryToRack);
    if (entryFromCluster.equals(entryToCluster)) {
      // In the same cluster, but not the same rack or host.
      return Latency.INTER_RACK_DELAY;
    }

    DataCentre entryFromDataCentre = simulation.cluster2DataCentre(entryFromCluster);
    DataCentre entryToDataCentre = simulation.cluster2DataCentre(entryToCluster);
    if (entryFromDataCentre.equals(entryToDataCentre)) {
      // In the same data centre, but not the same cluster or rack.
      return Latency.INTER_CLUSTER_DELAY;
    } else {
      // Not in the same datacenter. compute the latencies between data centres.
      SaviCloudNetwork savi = simulation.datacenterManager();
      return savi.getLatency(entryFromDataCentre, entryToDataCentre);
    }
  }
}