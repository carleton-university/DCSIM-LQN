package edu.uwo.csd.dcsim.application.workload;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uwo.csd.dcsim.core.Simulation;

/**
 * TraceWorkload sets the incoming work level based on a trace file. Trace files list workload
 * values in the range [0, 1]
 *
 * @author Michael Tighe
 */
public class TraceWorkload extends Workload {

  private static Map<String, WorkloadTrace> workloadTraces = new HashMap<String, WorkloadTrace>();

  int scaleFactor = 1; //the factor by which to scale work values
  WorkloadTrace workloadTrace; //the workload trace

  int currentPosition; //the current position in the trace

  int rampUpPosition;
  int rampUpSteps = 0;

  /**
   * Create a new TraceWorkload.
   *
   * @param fileName    The file name of the trace to use.
   * @param scaleFactor The factor by which to scale trace workload values. Traces values are in the
   *                    range [0, 1], so workload values are in the range [0, scaleFactor]
   * @param offset      The offset in simulation time to start the trace at.
   */
  public TraceWorkload(Simulation simulation, String fileName, int scaleFactor, long offset) {
    super(simulation,
          false); //start trace workload NOT enabled -  application should enable the workload

    this.scaleFactor = scaleFactor;
    if (workloadTraces.containsKey(fileName)) {
      workloadTrace = workloadTraces.get(fileName);
    } else {
      workloadTrace = new WorkloadTrace(fileName);
      workloadTraces.put(fileName, workloadTrace);
    }

    currentPosition =
        (int) Math.floor((offset % (workloadTrace.getLastTime() + workloadTrace.stepSize))
                         / workloadTrace.stepSize);
  }

  public TraceWorkload(Simulation simulation, String fileName, long offset) {
    super(simulation,
          false); //start trace workload NOT enabled -  application should enable the workload

    if (workloadTraces.containsKey(fileName)) {
      workloadTrace = workloadTraces.get(fileName);
    } else {
      workloadTrace = new WorkloadTrace(fileName);
      workloadTraces.put(fileName, workloadTrace);
    }

    currentPosition =
        (int) Math.floor((offset % (workloadTrace.getLastTime() + workloadTrace.stepSize))
                         / workloadTrace.stepSize);
  }

  public void setRampUp(long time) {
    rampUpPosition = 0;
    rampUpSteps = (int) Math.ceil(time / (double) workloadTrace.stepSize);
  }

  @Override
  protected int getCurrentWorkLevel() {

    int level = (int) (workloadTrace.getValues().get(currentPosition) * scaleFactor);

    if (rampUpPosition < rampUpSteps) {
      level = (int) Math.floor(level * (rampUpPosition / (double) rampUpSteps));
    }

    return level;
  }

  @Override
  protected long updateWorkLevel() {

    if (enabled) {
      if (rampUpPosition < rampUpSteps) {
        ++rampUpPosition;
      } else {
        ++currentPosition;
        if (currentPosition >= workloadTrace.getTimes().size()) {
          currentPosition = 0;
        }
      }
    }

		/*
                 * Calculate the update time so that the event times are always divisible by the step size. This ensures that regardless
		 * of when the workload is created and started, all workloads with the same step size will update at the same time. This 
		 * is a performance optimization to reduce the number of time jumps in the simulation.
		 */
    return (simulation.getSimulationTime() - (simulation.getSimulationTime() % workloadTrace
        .getStepSize())) + workloadTrace.getStepSize();
  }

  public void setScaleFactor(int scaleFactor) {
    this.scaleFactor = scaleFactor;
  }

  public int getScaleFactor() {
    return scaleFactor;
  }

  private class WorkloadTrace {

    private ArrayList<Long> times;
    private ArrayList<Double> values;
    private Long stepSize;

    public WorkloadTrace(String fileName) {
      times = new ArrayList<Long>();
      values = new ArrayList<Double>();

      try {
        BufferedReader input = new BufferedReader(new FileReader(fileName));

        String line;

        //read first line, which should contain the step size
        line = input.readLine();
        stepSize = Long.parseLong(line) * 1000; //file is in seconds, simulation runs in ms

        int seperator;
        while ((line = input.readLine()) != null) {
          seperator = line.indexOf(',');
          times.add(Long.parseLong(line.substring(0, seperator).trim())
                    * 1000); //file is in seconds, simulation runs in ms
          values.add(Double.parseDouble(line.substring(seperator + 1).trim()));
        }

        input.close();

        //if 0 not first, assume (0, 0) as initial time/workload pair
        if (times.get(0) != 0) {
          times.add(0, 0l);
          values.add(0, 0.0);
        }

      } catch (FileNotFoundException e) {
        throw new RuntimeException("Could not find trace file '" + fileName + "'", e);
      } catch (IOException e) {
        throw new RuntimeException("Could not load trace file '" + fileName + "'", e);
      }

    }

    public List<Long> getTimes() {
      return times;
    }

    public long getLastTime() {
      return times.get(times.size() - 1);
    }

    public List<Double> getValues() {
      return values;
    }

    public long getStepSize() {
      return stepSize;
    }
  }

}
