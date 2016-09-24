package edu.uwo.csd.dcsim.management.policies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import ca.carleton.dcsim.application.AppTask;
import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.management.HostData;
import edu.uwo.csd.dcsim.management.HostDataComparator;
import edu.uwo.csd.dcsim.management.HostStatus;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.VmStatus;
import edu.uwo.csd.dcsim.management.VmStatusComparator;
import edu.uwo.csd.dcsim.management.action.MigrationAction;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;
import edu.uwo.csd.dcsim.vm.VmAllocation;

/**
 * Implementation of a manual intervention policy 
 *
 * @author 
 */
public class ManualPolicy extends Policy {

  private int callCount = 0; 
  public ManualPolicy() {
    addRequiredCapability(HostPoolManager.class);

  }

  public void execute() {

	simulation.getLogger().info("============= Manual policy called " + ++callCount + "====================");
	  
    HostPoolManager hostPool = manager.getCapability(HostPoolManager.class);

    Collection<HostData> hosts = hostPool.getHosts();

    //reset the sandbox host status to the current host status
    try{
    	
    	AppTask appTask = null; 
    	List<TaskInstance> appInsts = null; 
		for (HostData host : hosts) {
	//		simulation.getLogger().info("Host: "+ host.getId());
			List<VmAllocation> vmAllocations = host.getHost().getVMAllocations();
			for (VmAllocation vmAllocation : vmAllocations) {
				if (vmAllocation.getVm() != null){
					if (vmAllocation.getVm().getVMDescription().getTask() instanceof AppTask)
					{
						appTask = (AppTask) vmAllocation.getVm().getVMDescription().getTask();
						appInsts = appTask.getInstances();

				          
					}
					
					simulation.getLogger().info("VM: "+ vmAllocation.getVm().getId() + 
							". CPU Required: "+ vmAllocation.getVm().getTaskInstance().getResourceDemand().cpu() +
							". Task: "+ vmAllocation.getVm().getVMDescription().getTask().getId() +
							". AppTask: "+ ((appTask != null) ? appTask.getName(): " N/A") +
							". TaskInstance: " + vmAllocation.getVm().getTaskInstance().getId() + 
							". Host: "+ host.getId() + 
							". DataCentre: " + simulation.host2DataCentre(host.getHost()).id());
		//			simulation.getLogger().info("Task: "+ vmAllocation.getVm().getVMDescription().getTask().getId());
					
					if (appTask != null){
						for (TaskInstance appInst : appInsts) {
				            if (appInsts.size() == 1) {
				            	simulation.getLogger().info("App instance is one");
				            }
				           
			            	simulation.getLogger().info("App instance: "+ appInst.getId()); 
				            
				          }
					}
					appTask = null;
					appInsts = null; 
				}
			}
		}
    }
    catch (Exception e){
    	e.printStackTrace(); 
    }
    
    simulation.getLogger().info("=============       End of Manual policy called        ====================");
    
    simulation.getLogger().info("<<<<<<<<<<<<<<<<<<<  Manual policy called   Start            >>>>>>>>>>>>>>>>>>>>>");
    
    
    
    
    
    
    simulation.getLogger().info("<<<<<<<<<<<<<<<<<<<  Manual policy called    end             >>>>>>>>>>>>>>>>>>>>>");
//
//    ArrayList<HostData> stressed = new ArrayList<HostData>();
//    ArrayList<HostData> partiallyUtilized = new ArrayList<HostData>();
//    ArrayList<HostData> underUtilized = new ArrayList<HostData>();
//    ArrayList<HostData> empty = new ArrayList<HostData>();
//
//    classifyHosts(stressed, partiallyUtilized, underUtilized, empty, hosts);
//
//    List<HostData> sources = orderSourceHosts(stressed);
//    List<HostData> targets = orderTargetHosts(partiallyUtilized, underUtilized, empty);
//    ArrayList<MigrationAction> migrations = new ArrayList<MigrationAction>();
//
//    boolean found;
//
//    // for each source host
//    for (HostData source : sources) {
//
//      found = false;
//      List<VmStatus> vmList = orderSourceVms(source.getCurrentStatus().getVms(), source);
//
//      // consider each VM within the source host
//      for (VmStatus vm : vmList) {
//
//        // look for a target host to receive this VM
//        for (HostData target : targets) {
//          if (target.getSandboxStatus().getIncomingMigrationCount() < 2 &&
//              //restrict target incoming migrations to 2 for some reason
//              HostData.canHost(vm, target.getSandboxStatus(), target.hostDecription()) &&
//              //target has capability and capacity to host VM
//              (target.getSandboxStatus().resourcesInUse().cpu() + vm.getResourceInUse()
//                  .cpu()) /
//              target.hostDecription().cpu()
//              <= targetUtilization) {                                //target will not exceed target utilization
//
//            //modify host and vm states to indicate the future migration. Note we can do this because
//            //we are using the designated 'sandbox' host status
//            source.getSandboxStatus().migrate(vm, target.getSandboxStatus());
//
//            //invalidate source and target status, as we know them to be incorrect until the next status update arrives
//            source.invalidateStatus(simulation.getSimulationTime());
//            target.invalidateStatus(simulation.getSimulationTime());
//
//            migrations.add(new MigrationAction(source.getHostManager(),
//                                               source.getHost(),
//                                               target.getHost(),
//                                               vm.getId()));
//
//            found = true;
//            break;
//
//          }
//        }
//
//        if (found) {
//          break;
//        }
//      }
//
//    }
//
//    // Trigger migrations.
//    for (MigrationAction migration : migrations) {
//      migration.execute(simulation, this);
//    }

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
