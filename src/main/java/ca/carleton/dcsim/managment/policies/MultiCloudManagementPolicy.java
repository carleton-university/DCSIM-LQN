//package ca.carleton.dcsim.managment.policies;
//
//import java.util.ArrayList;
//
//import ca.carleton.dcsim.application.AppTask;
//import edu.uwo.csd.dcsim.host.Resource;
//import edu.uwo.csd.dcsim.management.AutonomicManager;
//import edu.uwo.csd.dcsim.management.HostData;
//import edu.uwo.csd.dcsim.management.Policy;
//import edu.uwo.csd.dcsim.management.VmStatus;
//import edu.uwo.csd.dcsim.management.action.InstantiateVmAction;
//import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;
//import edu.uwo.csd.dcsim.management.events.ShutdownVmEvent;
//import edu.uwo.csd.dcsim.management.events.VmPlacementEvent;
//import edu.uwo.csd.dcsim.vm.VmAllocationRequest;
//
///**
//* DefaultVmPlacementPolicy takes a very basic approach to placement. It simply iterates through the
//* set of hosts, in no particular order (in the order they were added to the host manager), and
//* places the VM on the first Host it encounters that has enough capacity.
//*
//* @author Michael Tighe
//*/
//public class MultiCloudManagementPolicy extends Policy {
//
//  public MultiCloudManagementPolicy() {
//    addRequiredCapability(HostPoolManager.class);
//  }
//
//  public void execute(VmPlacementEvent event) {
//    HostPoolManager hostPool = manager.getCapability(HostPoolManager.class);
//
//    //filter out invalid host status
//    ArrayList<HostData> hosts = new ArrayList<HostData>();
//    for (HostData host : hostPool.getHosts()) {
//      if (host.isStatusValid()) {
//        hosts.add(host);
//      }
//    }
//
//    //reset the sandbox host status to the current host status
//    for (HostData host : hosts) {
//      host.resetSandboxStatusToCurrent();
//    }
//
//    //iterate though each VM to place
//    for (VmAllocationRequest vmAllocationRequest : event.getVMAllocationRequests()) {
//      HostData target = null;
//
//      final String taskName =
//          ((AppTask) vmAllocationRequest.getVMDescription().getTask()).getName();
//      switch (taskName) {
//        case "DataStorage":
//          target = hosts.get(0);
//          break;
//        case "DataEdge":
//          target = hosts.get(2);
//          break;
//        case "DataAnalysisC":
//          target = hosts.get(1);
//          break;
//        case "HandleXact":
//          target = hosts.get(3);
//          break;
//        case "DataAnalysisE":
//          target = hosts.get(4);
//          break;
//        default:
//          //add a failed request to the event for any event callback listeners to check
//          event.addFailedRequest(vmAllocationRequest);
//          return;
//      }
//
//      Resource reqResource = Resource.createLumped(
//          vmAllocationRequest.coreCapacity(),
//          vmAllocationRequest.memory(),
//          vmAllocationRequest.bandwidth(),
//          vmAllocationRequest.storage());
//
//      //add a dummy placeholder VM to keep track of placed VM resource requirements
//      target.getSandboxStatus().instantiateVm(
//          new VmStatus(vmAllocationRequest.getVMDescription().numCores(),
//                       vmAllocationRequest.getVMDescription().coreCapacity(),
//                       reqResource)
//      );
//
//      //invalidate this host status, as we know it to be incorrect until the next status update arrives
//      target.invalidateStatus(simulation.getSimulationTime());
//
//      InstantiateVmAction instantiateVmAction = new InstantiateVmAction(
//          target, vmAllocationRequest, event);
//      instantiateVmAction.execute(simulation, this);
//
//    }
//  }
//
//  public void execute(ShutdownVmEvent event) {
//    HostPoolManager hostPool = manager.getCapability(HostPoolManager.class);
//    AutonomicManager hostManager = hostPool.getHost(event.getHostId()).getHostManager();
//
//    //mark host status as invalid
//    hostPool.getHost(event.getHostId()).invalidateStatus(simulation.getSimulationTime());
//
//    //prevent the original event from logging, since we are creating a new event to forward to the host
//    event.setLog(false);
//
//    ShutdownVmEvent
//        shutdownEvent =
//        new ShutdownVmEvent(hostManager, event.getHostId(), event.getVmId());
//    event.addEventInSequence(shutdownEvent);
//    simulation.sendEvent(shutdownEvent);
//  }
//
//  @Override
//  public void onInstall() {
//    // TODO Auto-generated method stub
//
//  }
//
//  @Override
//  public void onManagerStart() {
//    // TODO Auto-generated method stub
//
//  }
//
//  @Override
//  public void onManagerStop() {
//    // TODO Auto-generated method stub
//
//  }
//
//}
