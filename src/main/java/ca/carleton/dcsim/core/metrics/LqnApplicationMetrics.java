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
import edu.uwo.csd.dcsim.application.VmmApplication;
import edu.uwo.csd.dcsim.common.Utility;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.metrics.MetricCollection;
import edu.uwo.csd.dcsim.core.metrics.WeightedMetric;

/**
 * @author Derek Hawker
 */
public class LqnApplicationMetrics extends MetricCollection {

  Map<Application, WeightedMetric> cpuUnderProvision = new HashMap<Application, WeightedMetric>();
  Map<Application, WeightedMetric> cpuDemand = new HashMap<Application, WeightedMetric>();

  Map<Application, WeightedMetric> slaPenalty = new HashMap<Application, WeightedMetric>();
  Map<Application, Long> slaAchieved = new HashMap<Application, Long>();
  Map<Application, Long> totalTime = new HashMap<Application, Long>();
  Map<Application, WeightedMetric> responseTime = new HashMap<Application, WeightedMetric>();
  Map<Application, WeightedMetric> throughput = new HashMap<Application, WeightedMetric>();
  Map<Application, WeightedMetric> size = new HashMap<Application, WeightedMetric>();

  WeightedMetric aggregateCpuUnderProvision = new WeightedMetric();
  WeightedMetric aggregateCpuDemand = new WeightedMetric();
  WeightedMetric aggregateSlaPenalty = new WeightedMetric();
  WeightedMetric aggregateResponseTime = new WeightedMetric();
  WeightedMetric aggregateThroughput = new WeightedMetric();

  DescriptiveStatistics slaPenaltyStats;
  DescriptiveStatistics slaAchievementStats;
  DescriptiveStatistics responseTimeStats;
  DescriptiveStatistics throughputStats;
  DescriptiveStatistics sizeStats;

  long applicationsSpawned = 0;
  long applicationsShutdown = 0;
  long applicationPlacementsFailed = 0;

  long appSlaGraceTime = 0;

  public LqnApplicationMetrics(Simulation simulation) {
    super(simulation);

    if (Simulation.hasProperty("appSlaGraceTime")) {
      appSlaGraceTime = Long.parseLong(Simulation.getProperty("appSlaGraceTime"));
    }
  }

  @Override
  public void recordApplicationMetrics(Collection<Application> applications) {

    double currentCpuUnderProvision = 0;
    double currentCpuDemand = 0;
    double currentSlaPenalty = 0;
    double currentResponseTime = 0;
    double currentThroughput = 0;
    double interactiveApplications = 0;

    double val;
    for (Application application : applications) {

      //we don't want to record stats for Vmm applications
      if (application instanceof VmmApplication) {
        continue;
      }

      //we don't want to record stats for inactive or complete applications
      if (!application.isActive() || application.isComplete()) {
        continue;
      }

      //don't record metrics for an application before the 'SLA grace time' is up (allows application scaling to adjust to initial load)
      if (simulation.getSimulationTime() - application.getActivateTimeStamp() < appSlaGraceTime) {
        continue;
      }

      if (!cpuUnderProvision.containsKey(application)) {
        cpuUnderProvision.put(application, new WeightedMetric());
        cpuDemand.put(application, new WeightedMetric());
        slaPenalty.put(application, new WeightedMetric());
        slaAchieved.put(application, 0l);
        totalTime.put(application, 0l);
        size.put(application, new WeightedMetric());
      }

      //record the size of the application as VMs/Max VMs
      size.get(application).add(application.getSize() / (double) application.getMaxSize(),
                                simulation.getElapsedTime());

      if (application.getTotalCpuDemand() > application.getTotalCpuScheduled()) {
        val = (double) application.getTotalCpuDemand() - application.getTotalCpuScheduled();
        cpuUnderProvision.get(application).add(val, simulation.getElapsedTime());
        currentCpuUnderProvision += val;
      }
      val = (double) application.getTotalCpuDemand();
      cpuDemand.get(application).add(val, simulation.getElapsedTime());
      currentCpuDemand += val;

      if (application.getSla() != null) {
        val = application.getSla().calculatePenalty();

        slaPenalty.get(application).add(val, simulation.getElapsedSeconds());
        currentSlaPenalty += val;

        if (application.getSla().evaluate()) {
          slaAchieved.put(application, slaAchieved.get(application) + simulation.getElapsedTime());
        }

      }
      totalTime.put(application, totalTime.get(application) + simulation.getElapsedTime());

      if (!responseTime.containsKey(application)) {
        responseTime.put(application, new WeightedMetric());
        throughput.put(application, new WeightedMetric());
      }

      if (application instanceof LqnApplication) {
        LqnApplication lqnApplication = (LqnApplication) application;

        val = lqnApplication.getResponseTime();
        responseTime.get(lqnApplication).add(val, simulation.getElapsedTime());
        currentResponseTime += val;

        val = lqnApplication.getThroughput();
        throughput.get(lqnApplication).add(val, simulation.getElapsedTime());
        currentThroughput += val;

        ++interactiveApplications;
      }
    }

    aggregateCpuUnderProvision.add(currentCpuUnderProvision, simulation.getElapsedTime());
    aggregateCpuDemand.add(currentCpuDemand, simulation.getElapsedTime());
    aggregateSlaPenalty.add(currentSlaPenalty, simulation.getElapsedSeconds());
    if (interactiveApplications != 0) {
      aggregateResponseTime
          .add(currentResponseTime / interactiveApplications, simulation.getElapsedTime());
      aggregateThroughput
          .add(currentThroughput / interactiveApplications, simulation.getElapsedTime());
    }
  }

  @Override
  public void completeSimulation() {
    slaPenaltyStats = new DescriptiveStatistics();
    slaAchievementStats = new DescriptiveStatistics();
    responseTimeStats = new DescriptiveStatistics();
    throughputStats = new DescriptiveStatistics();
    sizeStats = new DescriptiveStatistics();

    for (Application application : slaPenalty.keySet()) {
      slaPenaltyStats.addValue(slaPenalty.get(application).getSum());
    }

    for (Application application : slaAchieved.keySet()) {
      slaAchievementStats
          .addValue(slaAchieved.get(application) / (double) totalTime.get(application));
    }

    for (Application application : responseTime.keySet()) {
      responseTimeStats.addValue(responseTime.get(application).getMean());
    }

    for (Application application : throughput.keySet()) {
      throughputStats.addValue(throughput.get(application).getMean());
    }

    for (Application application : size.keySet()) {
      sizeStats.addValue(size.get(application).getMean());
    }
  }

  public Map<Application, WeightedMetric> getCpuUnderProvision() {
    return cpuUnderProvision;
  }

  public Map<Application, WeightedMetric> getCpuDemand() {
    return cpuDemand;
  }

  public Map<Application, WeightedMetric> getSlaPenalty() {
    return slaPenalty;
  }

  public Map<Application, Long> getSlaAchieved() {
    return slaAchieved;
  }

  public Map<Application, Long> getAppTotalTime() {
    return totalTime;
  }

  public Map<Application, WeightedMetric> getResponseTime() {
    return responseTime;
  }

  public Map<Application, WeightedMetric> getThroughput() {
    return throughput;
  }

  public WeightedMetric getAggregateCpuUnderProvision() {
    return aggregateCpuUnderProvision;
  }

  public WeightedMetric getAggregateCpuDemand() {
    return aggregateCpuDemand;
  }

  public WeightedMetric getAggregateSlaPenalty() {
    return aggregateSlaPenalty;
  }

  public WeightedMetric getAggregateResponseTime() {
    return aggregateResponseTime;
  }

  public WeightedMetric getAggregateThroughput() {
    return aggregateThroughput;
  }

  public DescriptiveStatistics getSlaPenaltyStats() {
    return slaPenaltyStats;
  }

  public DescriptiveStatistics getSlaAchievementStats() {
    return slaAchievementStats;
  }

  public long getSlaAchievementCountGTEValue(double slaValue) {
    long count = 0;

    for (double v : slaAchievementStats.getValues()) {
      if (v >= slaValue) {
        ++count;
      }
    }

    return count;
  }

  public long getSlaAchievementCountLTValue(double slaValue) {
    long count = 0;

    for (double v : slaAchievementStats.getValues()) {
      if (v < slaValue) {
        ++count;
      }
    }

    return count;
  }

  public DescriptiveStatistics getResponseTimeStats() {
    return responseTimeStats;
  }

  public DescriptiveStatistics getThroughputStats() {
    return throughputStats;
  }

  public DescriptiveStatistics getSizeStats() {
    return sizeStats;
  }

  public long getApplicationsSpawned() {
    return applicationsSpawned;
  }

  public long getTotalApplicationCount() {
    return cpuUnderProvision.keySet().size();
  }

  public void setApplicationsSpawned(long applicationsSpawned) {
    this.applicationsSpawned = applicationsSpawned;
  }

  public void incrementApplicationsSpawned() {
    ++applicationsSpawned;
  }

  public long getApplicationsShutdown() {
    return applicationsShutdown;
  }

  public void setApplicationShutdown(long applicationShutdown) {
    this.applicationsShutdown = applicationShutdown;
  }

  public void incrementApplicationShutdown() {
    ++applicationsShutdown;
  }

  public long getApplicationPlacementsFailed() {
    return applicationPlacementsFailed;
  }

  public void setApplicationPlacementFailed(long applicationPlacementFailed) {
    this.applicationPlacementsFailed = applicationPlacementFailed;
  }

  public void incrementApplicationPlacementsFailed() {
    ++applicationPlacementsFailed;
  }

  public boolean isMVAApproximate() {
    return InteractiveApplication.approximateMVA;
  }

  @Override
  public void printDefault(Logger out) {
    out.info("-- LQN APPLICATIONS --");
    out.info("   total: " + getTotalApplicationCount());
    out.info("   spawned: " + getApplicationsSpawned());
    out.info("   shutdown: " + getApplicationsShutdown());
    out.info("   failed placement: " + getApplicationPlacementsFailed());
    out.info(
        "   average size: " + Utility.roundDouble(Utility.toPercentage(getSizeStats().getMean()))
        + "%");
    out.info("CPU Underprovision");
    out.info("   percentage: " + Utility.roundDouble(Utility.toPercentage(
                                                         getAggregateCpuUnderProvision().getSum()
                                                         / getAggregateCpuDemand().getSum()),
                                                     Simulation.getMetricPrecision()) + "%");
    out.info("SLA");
    out.info("  achievement");
    out.info("    >= 99%: " + Utility
        .roundDouble(getSlaAchievementCountGTEValue(0.99), Simulation.getMetricPrecision()));
    out.info("    >= 95%: " + Utility
        .roundDouble(getSlaAchievementCountGTEValue(0.95), Simulation.getMetricPrecision()));
    out.info("    >= 90%: " + Utility
        .roundDouble(getSlaAchievementCountGTEValue(0.9), Simulation.getMetricPrecision()));
    out.info("    < 90%: " + Utility
        .roundDouble(getSlaAchievementCountLTValue(0.9), Simulation.getMetricPrecision()));
    out.info("    mean: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getMean()),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    stdev: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getStandardDeviation()),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    max: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getMax()),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    95th: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(95)),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    75th: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(75)),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    50th: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(50)),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    25th: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(25)),
                     Simulation.getMetricPrecision()) + "%");
    out.info("    min: " + Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getMin()),
                     Simulation.getMetricPrecision()) + "%");
    out.info("  aggregate penalty");
    out.info("    total: " + (long) getAggregateSlaPenalty().getSum());
    out.info("    max: " + Utility
        .roundDouble(getAggregateSlaPenalty().getMax(), Simulation.getMetricPrecision()));
    out.info("    mean: " + Utility
        .roundDouble(getAggregateSlaPenalty().getMean(), Simulation.getMetricPrecision()));
    out.info("    min: " + Utility
        .roundDouble(getAggregateSlaPenalty().getMin(), Simulation.getMetricPrecision()));
    out.info("  per application penalty");
    out.info("    mean: " + Utility
        .roundDouble(getSlaPenaltyStats().getMean(), Simulation.getMetricPrecision()));
    out.info("    stdev: " + Utility
        .roundDouble(getSlaPenaltyStats().getStandardDeviation(), Simulation.getMetricPrecision()));
    out.info("    max: " + Utility
        .roundDouble(getSlaPenaltyStats().getMax(), Simulation.getMetricPrecision()));
    out.info("    95th: " + Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(95), Simulation.getMetricPrecision()));
    out.info("    75th: " + Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(75), Simulation.getMetricPrecision()));
    out.info("    50th: " + Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(50), Simulation.getMetricPrecision()));
    out.info("    25th: " + Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(25), Simulation.getMetricPrecision()));
    out.info("    min: " + Utility
        .roundDouble(getSlaPenaltyStats().getMin(), Simulation.getMetricPrecision()));
    out.info("Response Time");
    out.info("    max: " + Utility
        .roundDouble(getAggregateResponseTime().getMax(), Simulation.getMetricPrecision()));
    out.info("    mean: " + Utility
        .roundDouble(getAggregateResponseTime().getMean(), Simulation.getMetricPrecision()));
    out.info("    min: " + Utility
        .roundDouble(getAggregateResponseTime().getMin(), Simulation.getMetricPrecision()));
    out.info("Throughput");
    out.info("    max: " + Utility
        .roundDouble(getAggregateThroughput().getMax(), Simulation.getMetricPrecision()));
    out.info("    mean: " + Utility
        .roundDouble(getAggregateThroughput().getMean(), Simulation.getMetricPrecision()));
    out.info("    min: " + Utility
        .roundDouble(getAggregateThroughput().getMin(), Simulation.getMetricPrecision()));
    out.info("Interactive Application Model Algorithm: ");
    if (!isMVAApproximate()) {
      out.info("MVA");
    } else {
      out.info("Schweitzer's MVA Approximation");
    }
  }

  @Override
  public List<Pair<String, Object>> getMetricValues() {

    List<Pair<String, Object>> metrics = new ArrayList<Pair<String, Object>>();

    metrics
        .add(new ImmutablePair<String, Object>("lqn_cpuUnderprovision", Utility.roundDouble(Utility
                                                                                                .toPercentage(
                                                                                                    getAggregateCpuUnderProvision()
                                                                                                        .getSum()
                                                                                                    / getAggregateCpuDemand()
                                                                                                        .getSum()),
                                                                                            Simulation
                                                                                                .getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveGTE99", Utility
        .roundDouble(getSlaAchievementCountGTEValue(0.99), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveGTE95", Utility
        .roundDouble(getSlaAchievementCountGTEValue(0.95), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveGTE90", Utility
        .roundDouble(getSlaAchievementCountGTEValue(0.9), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveLT90", Utility
        .roundDouble(getSlaAchievementCountLTValue(0.9), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveMean", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getMean()),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveStdev", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getStandardDeviation()),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveMax", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getMax()),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieve95", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(95)),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieve75", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(75)),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieve50", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(50)),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieve25", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getPercentile(25)),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAchieveMin", Utility
        .roundDouble(Utility.toPercentage(getSlaAchievementStats().getMin()),
                     Simulation.getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("lqn_slaAggregateTotal",
                                                  (long) getAggregateSlaPenalty().getSum()));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAggregateMax", Utility
        .roundDouble(getAggregateSlaPenalty().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAggregateMean", Utility
        .roundDouble(getAggregateSlaPenalty().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaAggregateMin", Utility
        .roundDouble(getAggregateSlaPenalty().getMin(), Simulation.getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplicationMean", Utility
        .roundDouble(getSlaPenaltyStats().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplicationStdev", Utility
        .roundDouble(getSlaPenaltyStats().getStandardDeviation(),
                     Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplicationMax", Utility
        .roundDouble(getSlaPenaltyStats().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplication95", Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(95), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplication75", Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(75), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplication50", Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(50), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplication25", Utility
        .roundDouble(getSlaPenaltyStats().getPercentile(25), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_slaApplicationMin", Utility
        .roundDouble(getSlaPenaltyStats().getMin(), Simulation.getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("lqn_responseTimeMax", Utility
        .roundDouble(getAggregateResponseTime().getMax(), Simulation.getMetricPrecision())));
    metrics.add(
        new ImmutablePair<String, Object>("lqn_responseTimeMean",
                                          getAggregateResponseTime().getMean()));
    metrics.add(new ImmutablePair<String, Object>("lqn_responseTimeMin", Utility
        .roundDouble(getAggregateResponseTime().getMin(), Simulation.getMetricPrecision())));

    metrics.add(new ImmutablePair<String, Object>("lqn_throughputMax", Utility
        .roundDouble(getAggregateThroughput().getMax(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_throughputMean", Utility
        .roundDouble(getAggregateThroughput().getMean(), Simulation.getMetricPrecision())));
    metrics.add(new ImmutablePair<String, Object>("lqn_throughputMin", Utility
        .roundDouble(getAggregateThroughput().getMin(), Simulation.getMetricPrecision())));

    metrics.add(
        new ImmutablePair<String, Object>("lqn_applicationsSpawned", getApplicationsSpawned()));
    metrics.add(
        new ImmutablePair<String, Object>("lqn_applicationsShutdown", getApplicationsShutdown()));
    metrics.add(new ImmutablePair<String, Object>("lqn_applicationPlacementsFailed",
                                                  getApplicationPlacementsFailed()));
    metrics.add(new ImmutablePair<String, Object>("lqn_averageSize", Utility
        .roundDouble(getSizeStats().getMean(), Simulation.getMetricPrecision())));

    return metrics;
  }

}

