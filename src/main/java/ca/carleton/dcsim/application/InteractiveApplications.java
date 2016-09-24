package ca.carleton.dcsim.application;

import java.util.List;
import java.util.Map;

import ca.carleton.lqn.LinearizedEntry;
import ca.carleton.lqn.LinearizedPair;
import ca.carleton.lqn.LinearizedTask;
import ca.carleton.lqn.LqnGraph;
import ca.carleton.lqn.core.entries.EntryDescription;
import edu.uwo.csd.dcsim.application.InteractiveApplication;
import edu.uwo.csd.dcsim.application.workload.Workload;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Resource;

/**
 * @author Derek Hawker
 */
public class InteractiveApplications {

  public static InteractiveApplication createFromLqn(Simulation simulation,
                                                     LqnGraph lqnGraph,
                                                     Workload workload,
                                                     float thinkTime) {
    LinearizedPair entryDemands = lqnGraph.toLinearizedList();

    InteractiveApplication.Builder appBuilder = new InteractiveApplication.Builder(simulation)
        .workload(workload)
        .thinkTime(thinkTime);

    for (LinearizedTask entryName : entryDemands.tasks()) {
      appBuilder
          .task(1, 1, Resource.createLumped(2000, 1, 1, 1), entryName.demandTime(), 1);
    }

    return appBuilder.build();
  }
}
