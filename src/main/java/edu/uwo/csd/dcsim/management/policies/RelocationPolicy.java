package edu.uwo.csd.dcsim.management.policies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.management.HostData;
import edu.uwo.csd.dcsim.management.HostDataComparator;
import edu.uwo.csd.dcsim.management.HostStatus;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.VmStatus;
import edu.uwo.csd.dcsim.management.VmStatusComparator;
import edu.uwo.csd.dcsim.management.action.MigrationAction;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;

/**
 * Implementation of IM2013 Balanced Relocation Policy
 *
 * @author Michael Tighe
 */
public class RelocationPolicy extends Policy {

  double lowerThreshold;
  double upperThreshold;
  double targetUtilization;

  public RelocationPolicy(double lowerThreshold,
                          double upperThreshold,
                          double targetUtilization) {
    addRequiredCapability(HostPoolManager.class);

    this.lowerThreshold = lowerThreshold;
    this.upperThreshold = upperThreshold;
    this.targetUtilization = targetUtilization;
  }

  public void execute() {

	  simulation.getLogger().info("RelocationPolicy.execute. get simulation time "+ simulation.getSimulationTime() + " Thread ID: " + Thread.currentThread().getId());
	  
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

    List<HostData> sources = orderSourceHosts(stressed);
    List<HostData> targets = orderTargetHosts(partiallyUtilized, underUtilized, empty);
    ArrayList<MigrationAction> migrations = new ArrayList<MigrationAction>();

    boolean found;

    // for each source host
    for (HostData source : sources) {

      found = false;
      List<VmStatus> vmList = orderSourceVms(source.getCurrentStatus().getVms(), source);

      // consider each VM within the source host
      for (VmStatus vm : vmList) {

        // look for a target host to receive this VM
        for (HostData target : targets) {
          if (target.getSandboxStatus().getIncomingMigrationCount() < 2 &&
              //restrict target incoming migrations to 2 for some reason
              HostData.canHost(vm, target.getSandboxStatus(), target.hostDecription()) &&
              //target has capability and capacity to host VM
              (target.getSandboxStatus().resourcesInUse().cpu() + vm.getResourceInUse()
                  .cpu()) /
              target.hostDecription().cpu()
              <= targetUtilization) {                                //target will not exceed target utilization

            //modify host and vm states to indicate the future migration. Note we can do this because
            //we are using the designated 'sandbox' host status
            source.getSandboxStatus().migrate(vm, target.getSandboxStatus());

            //invalidate source and target status, as we know them to be incorrect until the next status update arrives
            source.invalidateStatus(simulation.getSimulationTime());
            target.invalidateStatus(simulation.getSimulationTime());

            migrations.add(new MigrationAction(source.getHostManager(),
                                               source.getHost(),
                                               target.getHost(),
                                               vm.getId()));

            found = true;
            break;

          }
        }

        if (found) {
          break;
        }
      }

    }

    // Trigger migrations.
    for (MigrationAction migration : migrations) {
      migration.execute(simulation, this);
    }

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
        double avgCpuInUse = 0;
        int count = 0;
        for (HostStatus status : host.getHistory()) {
          //only consider times when the host is powered on TODO should there be events from hosts that are off?
          if (status.getState() == Host.HostState.ON) {
            avgCpuInUse += status.resourcesInUse().cpu();
            ++count;
          }
        }
        if (count != 0) {
          avgCpuInUse = avgCpuInUse / count;
        }

        double
            avgCpuUtilization =
            avgCpuInUse / host.hostDecription().cpu();

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

  public List<HostData> orderSourceHosts(List<HostData> stressed) {
    ArrayList<HostData> sorted = new ArrayList<HostData>(stressed);

    // Sort Stressed hosts in decreasing order by CPU utilization.
    Collections.sort(sorted, HostDataComparator.getComparator(HostDataComparator.CPU_UTIL));
    Collections.reverse(sorted);

    return sorted;
  }

  public List<VmStatus> orderSourceVms(List<VmStatus> sourceVms, HostData source) {
    ArrayList<VmStatus> sorted = new ArrayList<VmStatus>();

    // Remove VMs with less CPU load than the CPU load by which the source
    // host is stressed.
    double cpuExcess =
        source.getCurrentStatus().resourcesInUse().cpu()
        - source.hostDecription().cpu() * this.upperThreshold;
    for (VmStatus vm : sourceVms) {
      if (vm.getResourceInUse().cpu() >= cpuExcess) {
        sorted.add(vm);
      }
    }

    // Sort VMs in increasing order by CPU load.
    if (!sorted.isEmpty())    {
      Collections.sort(sorted, VmStatusComparator.getComparator(VmStatusComparator.CPU_IN_USE));
    } else {
      // Add original list of VMs and sort them in decreasing order by
      // CPU load, so as to avoid trying to migrate the smallest VMs
      // first (which would not help resolve the stress situation).
      sorted.addAll(sourceVms);
      Collections.sort(sorted, VmStatusComparator.getComparator(VmStatusComparator.CPU_IN_USE));
      Collections.reverse(sorted);
    }

    return sorted;
  }

  public List<HostData> orderTargetHosts(List<HostData> partiallyUtilized,
                                         List<HostData> underUtilized,
                                         List<HostData> empty) {
    ArrayList<HostData> targets = new ArrayList<HostData>();

    // Sort Partially-utilized hosts in increasing order by
    // <CPU utilization, power efficiency>.
    Collections.sort(partiallyUtilized, HostDataComparator
        .getComparator(HostDataComparator.CPU_UTIL, HostDataComparator.EFFICIENCY));

    // Sort Underutilized hosts in decreasing order by <CPU utilization,
    // power efficiency>.
    Collections.sort(underUtilized, HostDataComparator
        .getComparator(HostDataComparator.CPU_UTIL, HostDataComparator.EFFICIENCY));
    Collections.reverse(underUtilized);

    // Sort Empty hosts in decreasing order by <power efficiency,
    // power state>.
    Collections.sort(empty, HostDataComparator
        .getComparator(HostDataComparator.EFFICIENCY, HostDataComparator.PWR_STATE));
    Collections.reverse(empty);

    targets.addAll(partiallyUtilized);
    targets.addAll(underUtilized);
    targets.addAll(empty);

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
