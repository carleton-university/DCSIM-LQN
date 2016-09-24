package edu.uwo.csd.dcsim.management.events;

import java.util.ArrayList;
import java.util.List;

import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.management.AutonomicManager;
import edu.uwo.csd.dcsim.vm.VmAllocationRequest;

public class VmPlacementEvent extends Event {

  private List<VmAllocationRequest> vmAllocationRequests;
  private List<VmAllocationRequest> failedRequests = new ArrayList<>();

  public VmPlacementEvent(AutonomicManager target, List<VmAllocationRequest> vmAllocationRequests) {
    super(target);
    this.vmAllocationRequests = vmAllocationRequests;
  }

  public VmPlacementEvent(AutonomicManager target, VmAllocationRequest vmAllocationRequest) {
    super(target);
    this.vmAllocationRequests = new ArrayList<>();
    vmAllocationRequests.add(vmAllocationRequest);
  }

  public List<VmAllocationRequest> getVMAllocationRequests() {
    return vmAllocationRequests;
  }

  public List<VmAllocationRequest> getFailedRequests() {
    return failedRequests;
  }

  public void addFailedRequest(VmAllocationRequest failedRequest) {
    failedRequests.add(failedRequest);
  }
}
