package ca.carleton.dcsim.core.metrics;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.carleton.dcsim.application.LqnApplication;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.application.InteractiveApplication;
import edu.uwo.csd.dcsim.application.workload.Workload;
import edu.uwo.csd.dcsim.common.Utility;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.metrics.MetricCollection;
import edu.uwo.csd.dcsim.core.metrics.WeightedMetric;


/**
 * @author Derek Hawker
 */
public class WorkloadMetrics extends MetricCollection {

  Map<Workload, WeightedMetric> aggregateWorkloads = new HashMap<>();
  DescriptiveStatistics workloadStats;

  public WorkloadMetrics(Simulation simulation) {
    super(simulation);
  }

  @Override
  public void recordApplicationMetrics(Collection<Application> applications) {
    for (Application app : applications) {

      Workload workload = null;
      if (app instanceof InteractiveApplication) {

        InteractiveApplication iapp = (InteractiveApplication) app;
        workload = iapp.getWorkload();
      } else if (app instanceof LqnApplication) {

        LqnApplication lqnApp = (LqnApplication) app;
        workload = lqnApp.getWorkload();
      } else {
        continue;
      }
      WeightedMetric workloadMetric = aggregateWorkloads.get(workload);
      if (workloadMetric == null) {
        workloadMetric = new WeightedMetric();
        aggregateWorkloads.put(workload, workloadMetric);
      }

      int val = workload.getWorkOutputLevel();
      workloadMetric.add(val, simulation.getElapsedTime());
      break; // hack to only get a single app's workload.
    }
  }


  @Override
  public void completeSimulation() {
    workloadStats = new DescriptiveStatistics();

    for (WeightedMetric wm : aggregateWorkloads.values()) {
      workloadStats.addValue(wm.getMean());
    }
  }

  @Override
  public void printDefault(Logger out) {
    out.info("-- Workload--");
    out.info("    total: " + workloadStats.getSum());
    out.info(
        "    max: " + Utility.roundDouble(workloadStats.getMax(), Simulation.getMetricPrecision()));
    out.info("    mean: " + Utility
        .roundDouble(workloadStats.getMean(), Simulation.getMetricPrecision()));
    out.info(
        "    min: " + Utility.roundDouble(workloadStats.getMin(), Simulation.getMetricPrecision()));
  }

  @Override
  public List<Pair<String, Object>> getMetricValues() {

    ArrayList<Pair<String, Object>> metrics = new ArrayList<>();

    metrics.add(new ImmutablePair<String, Object>("workloadTotal", workloadStats.getSum()));
    metrics.add(new ImmutablePair<String, Object>("workloadMax", Utility
        .roundDouble(workloadStats.getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("workloadMean", Utility
        .roundDouble(workloadStats.getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("workloadMin", Utility
        .roundDouble(workloadStats.getMin(), Simulation.getMetricPrecision())));

    return metrics;
  }

}

