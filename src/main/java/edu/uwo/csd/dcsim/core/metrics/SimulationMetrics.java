package edu.uwo.csd.dcsim.core.metrics;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;

//import org.jopendocument.dom.OOUtils;
//import org.jopendocument.dom.spreadsheet.Sheet;

public class SimulationMetrics {

  Simulation simulation;
  HostMetrics hostMetrics;
  ClusterMetrics clusterMetrics;
  ApplicationMetrics applicationMetrics;
  ManagementMetrics managementMetrics;
  Map<Class<? extends MetricCollection>, MetricCollection>
      customMetrics =
      new HashMap<Class<? extends MetricCollection>, MetricCollection>();

  long executionTime;
  int applicationSchedulingTimedOut = 0;
  long nSteps = 0;

  public SimulationMetrics(Simulation simulation) {
    this.simulation = simulation;

    hostMetrics = new HostMetrics(simulation);
    clusterMetrics = new ClusterMetrics(simulation);
    applicationMetrics = new ApplicationMetrics(simulation);
    managementMetrics = new ManagementMetrics(simulation);
  }

  public HostMetrics getHostMetrics() {
    return hostMetrics;
  }

  public ClusterMetrics getClusterMetrics() {
    return clusterMetrics;
  }

  public ApplicationMetrics getApplicationMetrics() {
    return applicationMetrics;
  }

  public ManagementMetrics getManagementMetrics() {
    return managementMetrics;
  }

  public void recordMetrics() {
    for (MetricCollection custom : customMetrics.values()) {
      custom.recordMetrics();
    }
  }

  public void recordApplicationMetrics(Collection<Application> applications) {
    applicationMetrics.recordApplicationMetrics(applications);
    for (MetricCollection custom : customMetrics.values()) {
      custom.recordApplicationMetrics(applications);
    }
  }

  public void recordHostMetrics(Collection<Host> hosts) {
    hostMetrics.recordHostMetrics(hosts);
    for (MetricCollection custom : customMetrics.values()) {
      custom.recordHostMetrics(hosts);
    }
  }

  public void recordClusterMetrics(Collection<Cluster> clusters) {
    clusterMetrics.recordClusterMetrics(clusters);
    for (MetricCollection custom : customMetrics.values()) {
      custom.recordClusterMetrics(clusters);
    }
  }

  public void completeSimulation() {
    hostMetrics.completeSimulation();
    clusterMetrics.completeSimulation();
    applicationMetrics.completeSimulation();
    managementMetrics.completeSimulation();

    for (MetricCollection custom : customMetrics.values()) {
      custom.completeSimulation();
    }
  }

  public void setExecutionTime(long time) {
    executionTime = time;
  }

  public long getExecutionTime() {
    return executionTime;
  }

  public long getNSteps() {
    return nSteps;
  }

  public void incrementNSteps() {
    ++nSteps;
  }

  public int getApplicationSchedulingTimedOut() {
    return applicationSchedulingTimedOut;
  }

  public void setApplicationSchedulingTimedOut(int applicationSchedulingTimedOut) {
    this.applicationSchedulingTimedOut = applicationSchedulingTimedOut;
  }

  public void incrementApplicationSchedulingTimedOut() {
    ++applicationSchedulingTimedOut;
  }

  @SuppressWarnings("unchecked")
  public <T extends MetricCollection> T getCustomMetricCollection(Class<T> type) {
    return (T) customMetrics.get(type);
  }

  public void addCustomMetricCollection(MetricCollection metricCollection) {
    customMetrics.put(metricCollection.getClass(), metricCollection);
  }

  public void printDefault(Logger out) {

    hostMetrics.printDefault(out);
    out.info("");
    clusterMetrics.printDefault(out);
    out.info("");
    applicationMetrics.printDefault(out);
    out.info("");
    managementMetrics.printDefault(out);
    out.info("");

    for (MetricCollection metrics : customMetrics.values()) {
      metrics.printDefault(out);
      out.info("");
    }

    out.info("-- SIMULATION --");
    out.info("   execution time: " + SimTime.toHumanReadable(getExecutionTime()));
    out.info("   simulated time: " + SimTime.toHumanReadable(simulation.getDuration()));
    out.info(
        "   metric recording start: " + SimTime.toHumanReadable(simulation.getMetricRecordStart()));
    out.info("   metric recording duration: " + SimTime
        .toHumanReadable(simulation.getDuration() - simulation.getMetricRecordStart()));
    out.info("   application scheduling timed out: " + applicationSchedulingTimedOut);
    out.info("   simulation time steps: " + nSteps);

  }

  public List<Pair<String, Object>> getMetricValues() {
    List<Pair<String, Object>> metrics = new ArrayList<Pair<String, Object>>();

    metrics.add(new ImmutablePair<String, Object>("executionTime", getExecutionTime()));
    metrics.add(new ImmutablePair<String, Object>("simulatedTime", simulation.getDuration()));
    metrics.add(
        new ImmutablePair<String, Object>("metricRecordStart", simulation.getMetricRecordStart()));
    metrics.add(new ImmutablePair<String, Object>("metricRecordDuration",
                                                  simulation.getDuration() - simulation
                                                      .getMetricRecordStart()));
    metrics.add(
        new ImmutablePair<String, Object>("appSchedulingTimeout", applicationSchedulingTimedOut));
    metrics.add(new ImmutablePair<String, Object>("nSteps", nSteps));

    metrics.addAll(hostMetrics.getMetricValues());
    metrics.addAll(clusterMetrics.getMetricValues());
    metrics.addAll(applicationMetrics.getMetricValues());
    metrics.addAll(managementMetrics.getMetricValues());

    for (MetricCollection custom : customMetrics.values()) {
      metrics.addAll(custom.getMetricValues());
    }

    return metrics;
  }

  public void printCSV(PrintStream out) {
    printCSV(out, true);
  }

  public void printCSV(PrintStream out, boolean headings) {
    List<Pair<String, Object>> metrics = getMetricValues();

    if (headings) {
      out.print("name");
      for (Pair<String, Object> metric : metrics) {
        out.print("," + metric.getLeft());
      }
      out.println("");
    }

    out.print(simulation.getName());
    for (Pair<String, Object> metric : metrics) {
      out.print("," + metric.getRight());
    }
    out.println("");
  }

  public static void writeToODS(String fileName, SimulationMetrics simMetrics) {
    ArrayList<SimulationMetrics> list = new ArrayList<SimulationMetrics>();
    list.add(simMetrics);

    writeToODS(fileName, list);
  }

  public static void writeToODS(String fileName, List<SimulationMetrics> simMetrics) {

    Vector<String> columnNames = new Vector<String>();
    Vector<Vector<Object>> data = new Vector<Vector<Object>>();

    //TODO verify that all simulation results return the same list of metrics

    //Build column names
    columnNames.add("Name");
    for (Pair<String, Object> metric : simMetrics.get(0).getMetricValues()) {
      columnNames.add(metric.getLeft());
    }

    //Build data
    for (SimulationMetrics metrics : simMetrics) {
      Vector<Object> simData = new Vector<Object>();
      simData.add(metrics.simulation.getName());
      for (Pair<String, Object> metric : metrics.getMetricValues()) {
        simData.add(metric.getRight());
      }
      data.add(simData);
    }

    TableModel model = new DefaultTableModel(data, columnNames);

    SpreadSheet spreadsheet = SpreadSheet.createEmpty(model);

    // Save the data to an ODS file and open it.
    final File file = new File(fileName + ".ods");
    try {
      spreadsheet.saveAs(file);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
