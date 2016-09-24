package ca.carleton.dcsim.application;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Table;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ca.carleton.dcsim.SaviCloudNetwork;
import ca.carleton.lqn.LinearizedTask;
import ca.carleton.lqn.LqnGraph;
import ca.carleton.lqn.LqnsResults;
import ca.carleton.lqn.core.entries.EntryDescription;
import ca.carleton.lqn.core.tasks.TaskDescription;
import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.application.Task;
import edu.uwo.csd.dcsim.application.TaskInstance;
import edu.uwo.csd.dcsim.application.workload.Workload;
import edu.uwo.csd.dcsim.common.ObjectBuilder;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Resource;


/**
 * An application with queueing delays determined by Layered Queueing Network solver
 *
 * @author derekhawker
 */
public class LqnApplication extends Application {

  /**
   * Contains LQN model information
   */
  private final LqnGraph lqnGraph;
  private final List<AppTask> tasks;

  private final SaviCloudNetwork savi;

  private final Map<TaskDescription, Task> lqnTasks2DcsimTask;

  private Workload workload;
  double responseTime = 0;
  double throughput = 0;

  int totalCpuDemand;
  int totalCpuScheduled;

  int schedulingRounds;
  private Logger simLogger;

  /**
   * Simulation time when the LQN solver was last used to solve for response time.
   */
  private long lastTimeLqnSolved = 0;

  /**
   * True if LQNS has already solved for response time. Reset when simulator time moves forward.
   */
  private boolean isLqnSolved;


  public LqnApplication(Builder builder) {
    super(builder.simulation);
    simLogger = simulation.getLogger();

    this.savi = builder.dataCentreManager;
    this.workload = builder.workload;
    this.lqnGraph = builder.lqnGraph;
    this.lqnTasks2DcsimTask = new HashMap<>();
    this.tasks = new ArrayList<>();

    // Get All LQN model tasks and remove the reference.
    List<LinearizedTask> linearizedTasks = lqnGraph.toLinearizedList().tasks()
        .stream()
        .filter(td -> !td.equals(lqnGraph.referenceTask()))
        .collect(Collectors.toList());

    for (LinearizedTask linTask : linearizedTasks) {
      String name = linTask.task().name();
      AppTask task = createNewAppTask(name, linTask.task());

      lqnTasks2DcsimTask.put(linTask.task(), task);
    }
  }

  public AppTask createNewAppTask(String name, TaskDescription td) {
    AppTask.Builder tb = new AppTask.Builder(1, 2,
                                             Resource.createMicroInstance(), simulation);
    tb.application(LqnApplication.this);
    tb.name(name);
    tb.taskDescription(td);

    AppTask task = tb.build();
    lqnTasks2DcsimTask.put(td, task);
    tasks.add(task);
    return task;
  }

  @Override
  public void initializeScheduling() {
    schedulingRounds = 0;
    isLqnSolved = false;
    //reset scheduled resources and demand
    for (AppTask task : tasks) {
      for (TaskInstance instance : task.getInstances()) {
        //use the VMs max CPU capacity, as it may be on a different speed core than the Task size specifies
        instance.setResourceScheduled(task.getResourceSize()
                                          .withCpu(instance.getVM().getMaxCpu()));
        instance.setResourceDemand(instance.getResourceScheduled());
        instance.setFullDemand(null); //will be calculated on first 'updateDemand' call
      }
    }
  }

  @Override
  public void postScheduling() {
  }

  @Override
  public boolean updateDemand() {
    // Exit if LQN has already been solved.
    if (isLqnSolved || lastTimeLqnSolved == simulation.getSimulationTime()) {
      return false;
    }

    //If no clients arrived, default to 1)
    int numClients = Math.max(1, workload.getWorkOutputLevel());

    simLogger.debug(
        "numClients = " + numClients +
        ", time = " + simulation.getSimulationTime() +
        ", elapsed = " + simulation.getElapsedTime());

    //check for "dead" application (any Task has no TaskInstance)
    boolean dead = false;
    for (AppTask task : tasks) {
      if (task.getInstances().size() == 0) {
        dead = true;
      }
    }

    if (dead || !this.isActive()) {
      for (AppTask task : tasks) {
        for (TaskInstance instance : task.getInstances()) {
          instance.setResourceDemand(
              instance.getResourceDemand().withCpu(0));
          instance.setFullDemand(instance.getResourceDemand());
        }
      }
      throughput = 0;
      responseTime = Double.MAX_VALUE;
      return false;
    }

    Map<TaskInstance, String> task2ProcessorNameMap =
        reverseMap(
            findUniqueHosts(tasks, savi.hostLocationMap()));

    LqnGraph latencyLqnGraph = currentDeploymentLqnModel(numClients, task2ProcessorNameMap);

    LqnsResults lqnStats = latencyLqnGraph.solveLqnModel(simulation.getTempLqnModelFile(),
                                                         simulation.getTempLqnsOutputFile());

    for (AppTask task : tasks) {
      for (TaskInstance instance : task.getInstances()) {
        //TODO
//            instance.setThroughput(lqnStats.taskThroughputs.get(dcSimTask2EntryMap.get(task)));
//            instance.setUtilization(lqnStats.taskUtilizations.get(dcSimTask2EntryMap.get(task)));
        Double procUtil = lqnStats.processorUtilizations()
            .get(task2ProcessorNameMap.get(instance));
        int cpuUtil = (int) (instance.getVM().getMaxCpu() * procUtil);
        Resource demanded = instance.getResourceDemand().add(task.getResourceSize())
            .withCpu(cpuUtil);
        instance.setResourceDemand(demanded);

        if (instance.getFullDemand() == null) {
          //the first time demand is calculated, we get the full resource demand assuming full resource availability (no contention)
          instance.setFullDemand(instance.getResourceDemand());
        }
      }
    }

    responseTime = lqnStats.responseTime(lqnGraph.referenceEntry());
    isLqnSolved = true;
    lastTimeLqnSolved = simulation.getSimulationTime();
    return false;
  }

  private LqnGraph currentDeploymentLqnModel(int numClients,
                                             Map<TaskInstance, String> task2ProcessorNameMap) {// Create new task descriptions for each replicated tasks.
    // Create a translation map. Foreach replicated task, map the name of the original entry to
    // its new unique entry.
    HashMultimap<TaskDescription, TaskDescription> replicatedTasks = HashMultimap.create();
    HashMultiset<TaskDescription> counter = HashMultiset.create();
    HashMap<TaskDescription, TaskDescription> realHosts = new HashMap<>();
    HashBiMap<TaskInstance, TaskDescription> instance2replication = HashBiMap.create();
    lqnTasks2DcsimTask.entrySet().stream()
        .filter(e -> !e.getKey().equals(lqnGraph.referenceEntry()))
        .forEach(e -> {
          TaskDescription td = lqnGraph.taskDescriptions().get(e.getKey().name());
          Task appTask = e.getValue();
          List<TaskInstance> appInsts = appTask.getInstances();

          for (TaskInstance appInst : appInsts) {
            if (appInsts.size() == 1) {
              TaskDescription realProcessorTask = td.copyWithName(td.name())
                  .copyWithProcessor(task2ProcessorNameMap.get(appInst));
              realHosts.put(td, realProcessorTask);
              instance2replication.put(appInst, realProcessorTask);
            } else {
              counter.add(td);
              TaskDescription replicatedTask = td.copyWithName(td.name() + counter.count(td))
                  .copyWithProcessor(task2ProcessorNameMap.get(appInst));
              replicatedTasks.put(td, replicatedTask);
              instance2replication.put(appInst, replicatedTask);
            }
          }
        });

    LqnGraph lqnProgram = lqnGraph.withNumUsers(numClients)
        .withTasks(realHosts)
        .withReplications(replicatedTasks);

    Table<EntryDescription, EntryDescription, Double>
        latencies = DcsimLqnUtilities.buildLatencies(simulation, lqnProgram, instance2replication);

    return lqnProgram.withLatencyTasks(latencies);
  }

  Map<String, List<TaskInstance>> findUniqueHosts(List<AppTask> tasks,
                                                  Map<Host, DataCentre> hostDataCentreMap) {
    HashSet<String> hostNames = new HashSet<>();
    HashMap<String, List<TaskInstance>> hostTaskMap = new HashMap<>();

    for (AppTask appTask : tasks) {
      for (TaskInstance inst : appTask.getInstances()) {

        AppInstance lqnInst = (AppInstance) inst;

        Host host = lqnInst.getVM().getVMAllocation().host();
        String hostname = "d" + hostDataCentreMap.get(host).id() +
                          "c" + simulation.host2Cluster(host).id() +
                          "r" + simulation.host2Rack(host).id() +
                          "h" + host.id();
        hostNames.add(hostname);

        if (!hostTaskMap.containsKey(hostname)) {
          List<TaskInstance> processTasks = new ArrayList<>();
          hostTaskMap.put(hostname, processTasks);
        }
        List<TaskInstance> processTasks = hostTaskMap.get(hostname);
        processTasks.add(lqnInst);
        hostTaskMap.put(hostname, processTasks);
      }
    }

    return hostTaskMap;
  }

  Map<TaskInstance, String> reverseMap(Map<String, List<TaskInstance>> processorsTasksMap) {
    // Create a reversed look-up map of processorsTasksMap
    Map<TaskInstance, String> tasksProcessorsMap = new HashMap<>();
    for (Map.Entry<String, List<TaskInstance>> entry : processorsTasksMap.entrySet()) {
      for (TaskInstance inst : entry.getValue()) {
        tasksProcessorsMap.put(inst, entry.getKey());
      }
    }

    return tasksProcessorsMap;
  }

  @Override
  public void advanceSimulation() {
    for (AppTask task : tasks) {
      for (TaskInstance instance : task.getInstances()) {
        totalCpuDemand += instance.getFullDemand().cpu();
        totalCpuScheduled += instance.getResourceScheduled().cpu();
      }
    }
  }

  @Override
  public int getTotalCpuDemand() {
    return totalCpuDemand;
  }

  @Override
  public int getTotalCpuScheduled() {
    return totalCpuScheduled;
  }

  public double getResponseTime() {
    return responseTime;
  }

  public double getThroughput() {
    return throughput;
  }

  /**
   * Get the Workload for this Service
   */
  public Workload getWorkload() {
    return workload;
  }

  /**
   * Set the Workload for this Service
   */
  public void setWorkload(Workload workload) {
    this.workload = workload;
  }

  @Override
  public List<Task> getTasks() {
    return new ArrayList<>(tasks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (AppTask task : tasks) {
      sb.append(task).append("\n");
    }

    return sb.toString();
  }

  /**
   * Maps LQN tasks to DCSim tasks. Needed when creating LQN model for solver
   */
  public Map<TaskDescription, Task> lqnTasks2DcsimTask() {
    return lqnTasks2DcsimTask;
  }


  public static class Builder implements ObjectBuilder<Application> {

    private Simulation simulation;
    private Workload workload;
    private LqnGraph lqnGraph;
    private SaviCloudNetwork dataCentreManager;

    public Builder(Simulation simulation) {
      this.simulation = simulation;
    }


    public Builder datacentreManager(SaviCloudNetwork dataCentreManager) {
      this.dataCentreManager = dataCentreManager;
      return this;
    }

    public Builder workload(Workload workload) {
      this.workload = workload;
      return this;
    }

    public Builder lqnGraph(LqnGraph lqnGraph) {
      this.lqnGraph = lqnGraph;

      return this;
    }

    @Override
    public LqnApplication build() {
      return new LqnApplication(this);
    }
  }
}

