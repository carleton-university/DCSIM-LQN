package ca.carleton.dcsim.managment.policies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.management.HostData;
import edu.uwo.csd.dcsim.management.HostDataComparator;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.VmStatus;
import edu.uwo.csd.dcsim.management.VmStatusComparator;
import edu.uwo.csd.dcsim.management.action.ConcurrentManagementActionExecutor;
import edu.uwo.csd.dcsim.management.action.MigrationAction;
import edu.uwo.csd.dcsim.management.action.SequentialManagementActionExecutor;
import edu.uwo.csd.dcsim.management.action.ShutdownHostAction;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;

/**
 * @author Derek Hawker
 */
public class LQNConsolidationPolicy extends Policy {

  final double lowerThreshold;
  final double upperThreshold;
  final double targetUtilization;

  public LQNConsolidationPolicy(double lowerThreshold,
                                double upperThreshold,
                                double targetUtilization) {
    addRequiredCapability(HostPoolManager.class);

    this.lowerThreshold = lowerThreshold;
    this.upperThreshold = upperThreshold;
    this.targetUtilization = targetUtilization;
  }

  public void execute() {

    HostPoolManager hostPool = manager.getCapability(HostPoolManager.class);

    Collection<HostData> hosts = hostPool.getHosts();

    //reset the sandbox host status to the current host status
    for (HostData host : hosts) {
      host.resetSandboxStatusToCurrent();
    }

    ArrayList<HostData> stressed = new ArrayList<HostData>();
    ArrayList<HostData> partiallyUtilized = new ArrayList<HostData>();
    ArrayList<HostData> underUtilized = new ArrayList<HostData>();
    ArrayList<HostData> empty = new ArrayList<HostData>();

    classifyHosts(stressed, partiallyUtilized, underUtilized, empty, hosts);

    //filter out potential source hosts that have incoming migrations
    List<HostData> unsortedSources = underUtilized
        .stream()
        .filter(host -> host.getCurrentStatus().getIncomingMigrationCount() == 0)
        .collect(Collectors.toList());

    List<HostData> sources = orderSourceHosts(unsortedSources);

    List<HostData> targets = orderTargetHosts(partiallyUtilized, underUtilized);

    ConcurrentManagementActionExecutor migrations = new ConcurrentManagementActionExecutor();
    ConcurrentManagementActionExecutor shutdownActions = new ConcurrentManagementActionExecutor();

    HashSet<HostData> usedSources = new HashSet<HostData>();
    HashSet<HostData> usedTargets = new HashSet<HostData>();

    for (HostData source : sources) {
      if (!usedTargets
          .contains(source)) {        // Check that the source host hasn't been used as a target.
        List<VmStatus> vmList = this.orderSourceVms(source.getCurrentStatus().getVms());

        for (VmStatus vm : vmList) {

          for (HostData target : targets) {
            if (source != target &&
                !usedSources.contains(target) &&
                //Check that the target host hasn't been used as a source.
                HostData.canHost(vm, target.getSandboxStatus(), target.hostDecription()) &&
                //target has capability and capacity to host VM
                (target.getSandboxStatus().resourcesInUse().cpu() + vm.getResourceInUse().cpu())
                / target.hostDecription().cpu()
                <= targetUtilization) {                                //target will not exceed target utilization

              //modify host and vm states to indicate the future migration. Note we can do this because
              //we are using the designated 'sandbox' host status
              source.getSandboxStatus().migrate(vm, target.getSandboxStatus());

              //invalidate source and target status, as we know them to be incorrect until the next status update arrives
              source.invalidateStatus(simulation.getSimulationTime());
              target.invalidateStatus(simulation.getSimulationTime());

              migrations.addAction(new MigrationAction(source.getHostManager(),
                                                       source.getHost(),
                                                       target.getHost(),
                                                       vm.getId()));

              //if the host will be empty after this migration, instruct it to shut down
              if (source.getSandboxStatus().getVms().size() == 0) {
                shutdownActions.addAction(new ShutdownHostAction(source.getHost()));
              }

              usedTargets.add(target);
              usedSources.add(source);

              break;
            }
          }
        }
      }
    }

    // Trigger migrations.
    SequentialManagementActionExecutor actionExecutor = new SequentialManagementActionExecutor();
    actionExecutor.addAction(migrations);
    actionExecutor.addAction(shutdownActions);
    actionExecutor.execute(simulation, this);

  }

  private void classifyHosts(List<HostData> stressed,
                             List<HostData> partiallyUtilized,
                             List<HostData> underUtilized,
                             List<HostData> empty,
                             Collection<HostData> hosts) {

    for (HostData host : hosts) {

      //filter out hosts with a currently invalid status
      if (host.isStatusValid()) {

        // Calculate host's avg CPU utilization in the last window of time
        double avgCpuInUse = averageCpu(host);
        double avgCpuUtilization = avgCpuInUse / host.hostDecription().coreCapacity();

        //classify hosts, add copies of the host so that modifications can be made
        if (host.getCurrentStatus().getVms().size() == 0) {
          empty.add(host);
        } else if (avgCpuUtilization < lowerThreshold) {
          underUtilized.add(host);
        } else if (avgCpuUtilization > upperThreshold) {
          stressed.add(host);
        } else {
          partiallyUtilized.add(host);
        }

      }
    }
  }

  private double averageCpu(HostData host) {
    OptionalDouble avg = host.getHistory()
        .stream()
        .filter(status -> status.getState() == Host.HostState.ON)
        .mapToInt(status -> status.resourcesInUse().cpu())
        .average();

    return avg.isPresent() ? avg.getAsDouble() : 0.0;
  }


  private List<VmStatus> orderSourceVms(List<VmStatus> sourceVms) {

    ArrayList<VmStatus> sources = new ArrayList<VmStatus>(sourceVms);

    // Sort VMs in decreasing order by <overall capacity, CPU load>.
    // (Note: since CPU can be oversubscribed, but memory can't, memory
    // takes priority over CPU when comparing VMs by _size_ (capacity).)
    Collections.sort(sources, VmStatusComparator.getComparator(VmStatusComparator.MEMORY,
                                                               VmStatusComparator.CPU_CORES,
                                                               VmStatusComparator.CORE_CAP,
                                                               VmStatusComparator.CPU_IN_USE));
    Collections.reverse(sources);

    return sources;
  }

  private List<HostData> orderSourceHosts(List<HostData> underUtilized) {

    ArrayList<HostData> sources = new ArrayList<HostData>(underUtilized);

    // Sort Underutilized hosts in increasing order by <power efficiency,
    // CPU utilization>.
    Collections.sort(sources,
                     HostDataComparator.getComparator(HostDataComparator.EFFICIENCY,
                                                      HostDataComparator.CPU_UTIL));

    return sources;
  }

  private List<HostData> orderTargetHosts(List<HostData> partiallyUtilized,
                                          List<HostData> underUtilized) {
    ArrayList<HostData> targets = new ArrayList<HostData>();

    // Sort Partially-utilized and Underutilized hosts in decreasing order
    // by <power efficiency, CPU utilization>.
    targets.addAll(partiallyUtilized);
    targets.addAll(underUtilized);
    Collections.sort(targets,
                     HostDataComparator.getComparator(HostDataComparator.EFFICIENCY,
                                                      HostDataComparator.CPU_UTIL));
    Collections.reverse(targets);

    return targets;
  }

  @Override
  public void onInstall() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onManagerStart() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onManagerStop() {
    // TODO Auto-generated method stub

  }
}
