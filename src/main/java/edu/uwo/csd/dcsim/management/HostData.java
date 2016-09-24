package edu.uwo.csd.dcsim.management;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.vm.VmDescription;

public class HostData {

  private double powerEfficiency;
  private final Resource resources;
  private final AutonomicManager hostManager;
  private final Host host;

  private HostStatus currentStatus = null;
  private HostStatus sandboxStatus = null;
  //this is a HostStatus variable that can be freely modified for use in policies
  private boolean statusValid = true;
  private long invalidationTime = -1;

  private List<HostStatus> history = new ArrayList<>();

  private final int hashCode;

  public HostData(Host host, AutonomicManager hostManager) {
    this.host = host;
    this.hostManager = hostManager;

    this.powerEfficiency = host.powerEfficiency(1);
    resources = Resource.create(host.numCpu(), host.numCores(),
                                host.coreCapacity(), host.memory(),
                                host.bandwidth(), host.storage());

    //initialize currentStatus in order to maintain a status of powered off Hosts
    currentStatus = new HostStatus(host, 0);

    //init hashCode
    hashCode = new HashCodeBuilder()
        .append(host.id())
        .build();
  }

  public void addHostStatus(HostStatus hostStatus, int historyWindowSize) {
    currentStatus = hostStatus;
    if (sandboxStatus == null) {
      resetSandboxStatusToCurrent();
    }
    //only return the status to 'valid' if we know it was sent at at time after it was invalidated
    //TODO this might cause problems if, instead of waiting for the next status, we request an immediate update
    //with the message arriving at the same sim time.
    if (hostStatus.getTimeStamp() > invalidationTime) {
      statusValid = true; //if status was invalidated, we now know it is correct
    }

    history.add(0, hostStatus);
    if (history.size() > historyWindowSize) {
      history.remove(history.size() - 1);
    }
  }

  public HostStatus getCurrentStatus() {
    //return a copy of the status to ensure that it is read-only
    return currentStatus.copy();
  }

  public void setSandboxStatus(HostStatus status) {
    sandboxStatus = status;
  }

  public HostStatus getSandboxStatus() {
    return sandboxStatus;
  }

  public void resetSandboxStatusToCurrent() {
    if (currentStatus != null) {
      this.sandboxStatus = currentStatus.copy();
    }
  }

  public List<HostStatus> getHistory() {
    //return a copy of the history to ensure that it is read-only
    ArrayList<HostStatus> historyCopy = new ArrayList<HostStatus>();
    for (HostStatus status : history) {
      historyCopy.add(status.copy());
    }
    return historyCopy;
  }

  public int getId() {
    return host.id();
  }

  public Resource hostDecription() {
    return resources;
  }

  public AutonomicManager getHostManager() {
    return hostManager;
  }

  public Host getHost() {
    return host;
  }

  public boolean isStatusValid() {
    return statusValid;
  }

  public void invalidateStatus(long time) {
    this.statusValid = false;
    invalidationTime = time;
  }

  public static boolean canHost(VmDescription vmDescription,
                                HostStatus currentStatus,
                                HostData target) {
    Resource targetRez = target.hostDecription();
    Resource reqdRez = vmDescription.resources();

    if (!reqdRez.fitsIn(targetRez)) {
      return false;
    }
    //check available resource
    Resource resourceInUse = currentStatus.resourcesInUse();
    if (targetRez.grossCpu() - resourceInUse.cpu() < reqdRez.cpu()) {
      return false;
    }
    if (targetRez.memory() - resourceInUse.memory() < reqdRez.memory()) {
      return false;
    }
    if (targetRez.bandwidth() - resourceInUse.bandwidth() < reqdRez.bandwidth()) {
      return false;
    }

    return targetRez.storage() - resourceInUse.storage() >= reqdRez.storage();
  }

  public static boolean canHost(VmStatus vm,
                                HostStatus currentStatus,
                                Resource hostDescription) {
    return canHost(vm.getCores(), vm.getCoreCapacity(),
                   vm.getResourceInUse(), currentStatus,
                   hostDescription);
  }

  public static boolean canHost(int reqCores,
                                int reqCoreCapacity,
                                Resource reqResource,
                                HostStatus currentStatus,
                                Resource hostDescription) {
    //verify that this host can host the given vm

    //check capabilities (e.g. core count, core capacity)
    if (hostDescription.numCpus() * hostDescription.numCores() < reqCores) {
      return false;
    }
    if (hostDescription.coreCapacity() < reqCoreCapacity) {
      return false;
    }

    //check available resource
    Resource resourceInUse = currentStatus.resourcesInUse();
    if (hostDescription.coreCapacity() - resourceInUse.cpu() < reqResource.cpu()) {
      return false;
    }
    if (hostDescription.memory() - resourceInUse.memory() < reqResource.memory()) {
      return false;
    }
    if (hostDescription.bandwidth() - resourceInUse.bandwidth() < reqResource.bandwidth()) {
      return false;
    }

    return hostDescription.storage() - resourceInUse.storage() >= reqResource.storage();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public double powerEfficiency() {
    return powerEfficiency;
  }
}
