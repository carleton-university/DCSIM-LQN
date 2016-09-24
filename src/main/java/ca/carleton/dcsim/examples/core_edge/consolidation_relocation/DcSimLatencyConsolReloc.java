package ca.carleton.dcsim.examples.core_edge.consolidation_relocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ca.carleton.dcsim.SaviCloudNetwork;
import ca.carleton.dcsim.core.metrics.WorkloadMetrics;
import ca.carleton.dcsim.examples.GenericSimulationTask;
import ca.carleton.dcsim.examples.core_edge.DatacentreCommon;
import ca.carleton.dcsim.managment.PolicyOption;
import ca.carleton.lqn.LqnGraph;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.management.Policy;
import edu.uwo.csd.dcsim.management.policies.ConsolidationPolicy;
import edu.uwo.csd.dcsim.management.policies.DefaultVmPlacementPolicy;
import edu.uwo.csd.dcsim.management.policies.HostStatusPolicy;
import edu.uwo.csd.dcsim.management.policies.RelocationPolicy;

public final class DcSimLatencyConsolReloc extends GenericSimulationTask {

  private static final String epa = "epa";

  public static void main(String args[]) throws IOException {
    GenericSimulationTask.executeTestcases(DcSimLatencyConsolReloc.class);
  }


  public DcSimLatencyConsolReloc(String name,
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

    List<PolicyOption> repeatingPolicies = Arrays.asList(
        PolicyOption.create(new RelocationPolicy(0.5, 0.9, 0.85),
                            SimTime.hours(1),
                            SimTime.hours(1) + 1),
        PolicyOption.create(new ConsolidationPolicy(0.5, 0.9, 0.85),
                            SimTime.hours(2),
                            SimTime.hours(2) + 2));

    return DatacentreCommon.coreEdgeDataCentre(simulation, oneShotPolicies,
                                               repeatingPolicies, 25,
                                               0.1);
  }

  @Override
  protected List<Application> createApplications(SaviCloudNetwork dataCentreManager,
                                                 LqnGraph lqnModel) {
    return GenericSimulationTask.createInteractiveLatencyApplication(simulation,
                                                                     epa,
                                                                     lqnModel);
  }


  @Override
  protected void createSimulationMetrics() {
    simulation.getSimulationMetrics()
        .addCustomMetricCollection(new WorkloadMetrics(simulation));
  }
}