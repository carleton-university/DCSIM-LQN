package ca.carleton.dcsim.examples.core_edge.static_deployment;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ca.carleton.dcsim.SaviCloudNetwork;
import ca.carleton.dcsim.core.metrics.LqnApplicationMetrics;
import ca.carleton.dcsim.core.metrics.WorkloadMetrics;
import ca.carleton.dcsim.examples.GenericSimulationTask;
import ca.carleton.dcsim.examples.core_edge.DatacentreCommon;
import ca.carleton.dcsim.managment.PolicyOption;
import ca.carleton.lqn.LqnGraph;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.policies.DefaultVmPlacementPolicy;
import edu.uwo.csd.dcsim.management.policies.HostStatusPolicy;

public final class LqnStatic extends GenericSimulationTask {

//  private static final String epa = "epa"; //Jeevithan
	private static final String epa = "epa (const 0.6)";

  public static void main(String args[]) throws IOException {
    GenericSimulationTask.executeTestcases(LqnStatic.class);
  }

  public LqnStatic(String name,
                   long seed,
                   LqnGraph lqnModel,
                   long simTime) {
    super(name, seed, lqnModel, simTime);
    this.setRandomSeed(seed);
    this.setMetricRecordStart(SimTime.hours(1));
  }

  @Override
  protected SaviCloudNetwork createSaviNetwork() {
    List<Policy> oneShotPolicies = Arrays.asList(
        new DefaultVmPlacementPolicy(),
        new HostStatusPolicy(1));

    List<PolicyOption> repeatingPolicies = Arrays.asList();

    return DatacentreCommon.coreEdgeDataCentre(simulation, oneShotPolicies,
                                               repeatingPolicies, 50,
                                               0.1);
  }

  @Override
  protected List<Application> createApplications(SaviCloudNetwork dataCentreManager,
                                                 LqnGraph lqnModel) {
    return createLqnApplication(simulation, dataCentreManager, epa, lqnModel);
  }

  @Override
  protected void createSimulationMetrics() {
    simulation.getSimulationMetrics()
        .addCustomMetricCollection(new WorkloadMetrics(simulation));
    simulation.getSimulationMetrics()
        .addCustomMetricCollection(new LqnApplicationMetrics(simulation));
  }
}
