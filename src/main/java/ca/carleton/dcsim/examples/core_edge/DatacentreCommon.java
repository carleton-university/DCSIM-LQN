package ca.carleton.dcsim.examples.core_edge;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ca.carleton.dcsim.SaviCloudNetwork;
import ca.carleton.dcsim.managment.PolicyOption;
import ca.carleton.dcsim.managment.capabilities.ApplicationManager;
import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.core.EventCallbackListener;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Rack;
import edu.uwo.csd.dcsim.host.SwitchFactory;
import edu.uwo.csd.dcsim.host.power.HostPowerModel;
import edu.uwo.csd.dcsim.host.power.SPECHostPowerModel;
import edu.uwo.csd.dcsim.host.resourcemanager.DefaultResourceManagerFactory;
import edu.uwo.csd.dcsim.host.scheduler.DefaultResourceSchedulerFactory;
import edu.uwo.csd.dcsim.management.AutonomicManager;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;
import edu.uwo.csd.dcsim.management.events.VmPlacementEvent;
import edu.uwo.csd.dcsim.vm.VmAllocationRequest;
import edu.uwo.csd.dcsim.vm.VmDescription;

/**
 * @author Derek Hawker
 */
public class DatacentreCommon {

  private static Host.Builder createHost(Simulation simulation) {
    //Power Efficiency: 85.84 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 148, 159, 167, 175, 184, 194, 204, 213, 220, 227, 233);

    return new Host.Builder(simulation)
        .specification(1, 4, 2000, 163840, 1310720, 36864) //Jeevithan changed from 1, 1, 2500, 16384, 1310720, 36864
        .powerModel(powerModel)
        .privCpu(500)
        .privBandwidth(131072)
        .resourceManagerFactory(new DefaultResourceManagerFactory())
        .resourceSchedulerFactory(new DefaultResourceSchedulerFactory());
  }

  public static SaviCloudNetwork coreEdgeDataCentre(Simulation simulation,
                                                    List<Policy> oneShotPolicies,
                                                    List<PolicyOption> repeatingPolicies,
                                                    int totalHosts,
                                                    double core2edgeRatio) {
    int numCoreHosts = (int) Math.round(Math.ceil(totalHosts * core2edgeRatio));
    int[] dcNumHosts = {numCoreHosts, totalHosts - numCoreHosts};

    ArrayList<DataCentre> dcs = new ArrayList<>();
    HashMap<DataCentre, Integer> dc2index = Maps.newHashMap();
    for (int dcId = 0; dcId < dcNumHosts.length; dcId++) {

      ArrayList<Cluster> clusters = new ArrayList<>();
      DataCentre.Builder dcBuilder = new DataCentre.Builder(simulation)
          .switchFactory(SwitchFactory.switch10g48p());

      ArrayList<Rack> racks = new ArrayList<>();
      // Define Cluster types.
      Cluster.Builder clusterBuilder = new Cluster.Builder(simulation)
          .nSwitches(1).switchFactory(SwitchFactory.switch10g48p());

      // Define Rack types.
      int numHosts = dcNumHosts[dcId];
      Rack.Builder rackBuilder = new Rack.Builder(simulation)
          .nSlots(numHosts)
          .switchFactory(SwitchFactory.switch10g48p());

      ArrayList<Host> hosts = new ArrayList<>();
      Host.Builder hostBuilder = createHost(simulation);

      for (int h = 0; h < numHosts; h++) {
        Host host = hostBuilder.build();
        hosts.add(host);
      }
      rackBuilder.hosts(hosts);
      racks.add(rackBuilder.build());

      clusterBuilder.racks(racks);
      clusters.add(clusterBuilder.build());

      DataCentre dataCentre = dcBuilder.clusters(clusters).build();
      dcs.add(dataCentre);
      dc2index.put(dataCentre, dcId);
    }

    SaviCloudNetwork.Builder builder = new SaviCloudNetwork.Builder(simulation);

    HostPoolManager hostPool = new HostPoolManager();
    builder.hostPool(hostPool);

    ApplicationManager appPool = new ApplicationManager();
    builder.applicationPool(appPool);

    AutonomicManager autonomicMgr = new AutonomicManager(simulation, hostPool, appPool);
    for (Policy policy : oneShotPolicies) {
      autonomicMgr.installPolicy(policy);
    }
    for (PolicyOption po : repeatingPolicies) {
      autonomicMgr.installPolicy(po.policy(), po.executionInterval(), po.startTime());
    }
    builder.autonomicManager(autonomicMgr);

    builder.dc2Index(dc2index);

    builder.datacentres(dcs);

    builder.latencies(new double[][]{new double[]{0, 200}, new double[]{200, 0}});

    return builder.build();
  }

  public static List<Path> testCases() {
    try {
      return Files.list(Paths.get("testcases"))
          .filter(path -> path.getFileName().toString().endsWith(".lqn"))
          .sorted((o1, o2) -> {
            Integer i1 = Integer.valueOf(o1.getFileName().toString().replace(".lqn", ""));
            Integer i2 = Integer.valueOf(o2.getFileName().toString().replace(".lqn", ""));
            return i1.compareTo(i2);
          })
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void replicateTask(Simulation simulation, Task task) {
    ArrayList<VmAllocationRequest> vmList = new ArrayList<>();
    vmList.add(new VmAllocationRequest(new VmDescription(task)));
    VmPlacementEvent vmPlacementEvent =
        new VmPlacementEvent(simulation.datacenterManager().getAutonomicManger(), vmList);

    // Ensure that all VMs are placed, or kill the simulation
    EventCallbackListener ensureVmsPlaced = e -> {
      VmPlacementEvent pe = (VmPlacementEvent) e;
      if (!pe.getFailedRequests().isEmpty()) {
        throw new RuntimeException("Could not place all VMs " + pe.getFailedRequests().size());
      }
    };
    vmPlacementEvent.addCallbackListener(ensureVmsPlaced);

    simulation.sendEvent(vmPlacementEvent, simulation.getSimulationTime());
  }
}
