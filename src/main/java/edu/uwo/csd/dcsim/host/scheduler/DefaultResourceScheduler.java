package edu.uwo.csd.dcsim.host.scheduler;

import com.google.common.math.IntMath;

import java.math.RoundingMode;
import java.util.ArrayList;

import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.vm.Vm;
import edu.uwo.csd.dcsim.vm.VmAllocation;

import static com.google.common.base.Preconditions.checkState;

public class DefaultResourceScheduler extends ResourceScheduler {

  @Override
  public void scheduleResources() {

    Resource resourceRemaining = Resource.createLumped(
        host.getResourceManager().getTotalCpu(),
        host.getResourceManager().getTotalMemory(),
        host.getResourceManager().getTotalBandwidth(),
        host.getResourceManager().getTotalStorage());

    //first, schedule privileged domain (VMM) its full demand
    Vm privDomainVm = host.getPrivDomainAllocation().getVm();
    Resource privResourceDemand = privDomainVm.getResourceDemand();

    checkState(resourceRemaining.cpu() >= privResourceDemand.cpu(),
               "Host #" + host.id()
               + " does not have enough CPU to execute the VMM (privileged domain)");
    checkState(resourceRemaining.memory() >= privResourceDemand.memory(),
               "Host #" + host.id()
               + " does not have enough memory to execute the VMM (privileged domain)");
    checkState(resourceRemaining.bandwidth() >= privResourceDemand.bandwidth(),
               "Host #" + host.id()
               + " does not have enough bandwidth to execute the VMM (privileged domain)");
    checkState(resourceRemaining.storage() >= privResourceDemand.storage(),
               "Host #" + host.id()
               + " does not have enough storage to execute the VMM (privileged domain)");

    resourceRemaining = Resource.createLumped(
        resourceRemaining.cpu() - privResourceDemand.cpu(),
        resourceRemaining.memory() - privResourceDemand.memory(),
        resourceRemaining.bandwidth() - privResourceDemand.bandwidth(),
        resourceRemaining.storage() - privResourceDemand.storage());
    privDomainVm.scheduleResources(privResourceDemand);

    //build list of VM allocations that current contain a VM
    ArrayList<VmAllocation> vmAllocations = new ArrayList<>();
    for (VmAllocation vmAlloc : host.getVMAllocations()) {
      if (vmAlloc.getVm() != null) {
        vmAllocations.add(vmAlloc);
      }
    }

    //initialize resource scheduling
    for (VmAllocation vmAlloc : vmAllocations) {
      Vm vm = vmAlloc.getVm();

      //start with CPU at 0 and all other resources equal to demand
      Resource scheduled = vm.getResourceDemand();
      /* Verify that enough memory, bandwidth and storage are available. For now, we simply kill
      the simulation if this is not the case, the the behaviour is presently undefined */
      checkState(scheduled.memory() <= resourceRemaining.memory(),
                 "Host #" + host.id() +
                 " does not have enough memory to execute VM #" + vm.getId());
      checkState(scheduled.bandwidth() <= resourceRemaining.bandwidth(),
                 "Host #" + host.id() +
                 " does not have enough bandwidth to execute VM #" + vm.getId());
      checkState(scheduled.storage() <= resourceRemaining.storage(),
                 "Host #" + host.id() +
                 " does not have enough storage to execute VM #" + vm.getId());

      scheduled = scheduled.withCpu(0);
      resourceRemaining = resourceRemaining.withLumped(
          resourceRemaining.cpu(),
          resourceRemaining.memory() - scheduled.memory(),
          resourceRemaining.bandwidth() - scheduled.bandwidth(),
          resourceRemaining.storage() - scheduled.storage());
      vm.scheduleResources(scheduled);

    }

    //now, we schedule CPU fairly among all VMs
    int incompleteVms = vmAllocations.size();

    //adjust incompleteVm count to remove any VMs that have 0 CPU demand
    for (VmAllocation vmAlloc : vmAllocations) {
      if (vmAlloc.getVm().getResourceDemand().cpu() == 0) {
        --incompleteVms;
      }
    }

    int cpuShare;
    while (resourceRemaining.cpu() > 0 && incompleteVms > 0) {
      cpuShare = IntMath.divide(resourceRemaining.cpu(), incompleteVms, RoundingMode.FLOOR);

      //if resourcesRemaining is small enough, it could be rounded to 0. Set '1' as minimum share.
      cpuShare = Math.max(cpuShare, 1);

      for (VmAllocation vmAlloc : vmAllocations) {
        Vm vm = vmAlloc.getVm();
        Resource scheduled = vm.getResourceScheduled();

        int remainingCpuDemand = vm.getResourceDemand().cpu() - scheduled.cpu();

        if (remainingCpuDemand > 0) {
          if (remainingCpuDemand <= cpuShare) {
            scheduled = scheduled.withCpu(scheduled.cpu() + remainingCpuDemand);
            resourceRemaining = resourceRemaining
                .withCpu(resourceRemaining.cpu() - remainingCpuDemand);
            --incompleteVms;
          } else {
            scheduled = scheduled.withCpu(scheduled.cpu() + cpuShare);
            resourceRemaining = resourceRemaining.withCpu(resourceRemaining.cpu() - cpuShare);
          }

          vm.scheduleResources(scheduled);
        }

        //check if we are out of CPU. This can occur when share is defaulted to '1' as minimum
        if (resourceRemaining.cpu() == 0) {
          break;
        }

      }
    }
  }
}
