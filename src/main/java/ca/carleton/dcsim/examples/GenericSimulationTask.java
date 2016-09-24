package ca.carleton.dcsim.examples;

import org.apache.log4j.Logger;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.carleton.dcsim.SaviCloudNetwork;
import ca.carleton.dcsim.application.InteractiveLatencyApplication;
import ca.carleton.dcsim.application.LqnApplication;
import ca.carleton.dcsim.examples.core_edge.DatacentreCommon;
import ca.carleton.lqn.LqnGraph;
import ca.carleton.lqn.core.tasks.TaskDescription;
import edu.uwo.csd.dcsim.SimulationExecutor;
import edu.uwo.csd.dcsim.SimulationTask;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.application.InteractiveApplication;
import edu.uwo.csd.dcsim.application.workload.TraceWorkload;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.core.Event;
import edu.uwo.csd.dcsim.core.EventCallbackListener;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.SimulationEventListener;
import edu.uwo.csd.dcsim.host.Resource;
import edu.uwo.csd.dcsim.management.events.VmPlacementEvent;

public abstract class GenericSimulationTask extends SimulationTask
    implements SimulationEventListener {

  private final LqnGraph lqnModel;

  public GenericSimulationTask(String name,
                               long seed,
                               LqnGraph lqnModel,
                               long simTime) {
    super(name, simTime);
    this.setRandomSeed(seed);
    this.setMetricRecordStart(SimTime.seconds(10));//Jeevithan changed from .hours(1)
    this.lqnModel = lqnModel;
  }
  public static void executeTestcases(Class clazz) {
    executeTestCases(clazz, true); //Jeevithan. changed from false. 
  }
  private static void executeTestCases(Class clazz, boolean force) {
    try {
      Constructor ctors = clazz.getDeclaredConstructor(String.class, Long.TYPE,
                                                       LqnGraph.class, Long.TYPE);

      Simulation.initializeLogging();

      SimulationExecutor executor = new SimulationExecutor();
      for (Path testcaseFile : DatacentreCommon.testCases().subList(0, 1)) {  //Jeevithan. changed from subList(0, 10)

        int lastSeparator = clazz.getPackage().getName().lastIndexOf('.') + 1;
        String loggerName = clazz.getPackage().getName().substring(lastSeparator) +
                            "-" + clazz.getSimpleName() + "-" +
                            testcaseFile.getFileName().toString().replace(".lqn", "");
        if (!force) {
          String fullLoggerName = Simulation.getLogDirectory() + "/" + loggerName + ".log";
          if (Files.exists(Paths.get(fullLoggerName))) {
            System.out.println(fullLoggerName + " already exists. Skipping testcase: " +
                               testcaseFile.getFileName());
            continue;
          }
        }

        Map<TaskDescription, Integer>
            replications =
            LQNSolverMaxUsers.testcaseReplications(testcaseFile);
        LqnGraph lqnModel = LqnGraph.readLqnModel(testcaseFile)
            .withReplicatedTasks(replications);

        SimulationTask simTask =
            (SimulationTask) ctors.newInstance(loggerName, 1088501048448116498l,
                                               lqnModel, SimTime.seconds(20000)); //Jeevithan changed from .hours(24)
        executor.addTask(simTask);
        System.out.println("replicated tasks: " + lqnModel.taskDescriptions().size());
      }
      Collection<SimulationTask> completedTasks = executor.execute(3);

      for (SimulationTask simulationTask : completedTasks) {
        Logger logger = simulationTask.getLogger();
        if (logger != null) {
          logger.info(simulationTask.getName());
          simulationTask.getMetrics().printDefault(logger);
        }
      }

    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the data centres to be used
   */
  protected abstract SaviCloudNetwork createSaviNetwork();

  /**
   * Return the application to run in the simulation.
   */
  protected abstract List<Application> createApplications(SaviCloudNetwork dataCentreManager,
                                                          LqnGraph lqnModel);

  /**
   * Add any extra simulation metrics. May include side effects
   */
  protected abstract void createSimulationMetrics();

  /**
   * Create a SimDataLogger
   */

  @Override
  public void setup(Simulation simulation) {
    createSimulationMetrics();

    simulation.addSaviNetwork(createSaviNetwork());

    SaviCloudNetwork saviNetwork = simulation.datacenterManager();
    List<Application> applications = createApplications(simulation.datacenterManager(),
                                                        lqnModel);
    for (Application app : applications) {
      saviNetwork.applicationPool().addApplication(app);

      VmPlacementEvent vmPlacementEvent = new VmPlacementEvent(saviNetwork.getAutonomicManger(),
                                                               app.createInitialVmRequests());
      // Ensure that all VMs are placed, or kill the simulation
      EventCallbackListener ensureVmsPlaced = e -> {
        VmPlacementEvent pe = (VmPlacementEvent) e;
        if (!pe.getFailedRequests().isEmpty()) {
          throw new RuntimeException("Could not place all VMs " + pe.getFailedRequests().size());
        }
      };
      vmPlacementEvent.addCallbackListener(ensureVmsPlaced);
      simulation.sendEvent(vmPlacementEvent, 0);
    }

    logger = simulation.getLogger();
  }

  @Override
  public void handleEvent(Event e) {
  }

  public static List<Application> createInteractiveApplication(Simulation simulation,
                                                               String traceName,
                                                               LqnGraph lqnModel) {
    String trace = new File("traces", traceName).toString();

    TraceWorkload workload = new TraceWorkload(simulation, trace, 150, 0);

    InteractiveApplication.Builder appBuilder = new InteractiveApplication.Builder(simulation);
    lqnModel.toLinearizedList().tasks().stream()
        .forEach(t -> appBuilder.task(1, 100, Resource.createMicroInstance(), t.demandTime(), 1));
    appBuilder.workload(workload)
        .thinkTime((float) lqnModel.referenceEntry().thinkTime());

    InteractiveApplication app = appBuilder.build();

    return Arrays.asList(app);
  }


  public static List<Application> createInteractiveLatencyApplication(Simulation simulation,
                                                                      String traceName,
                                                                      LqnGraph lqnModel) {
    String trace = new File("traces", traceName).toString();
    TraceWorkload workload = new TraceWorkload(simulation, trace, 150, 0);

    InteractiveApplication.Builder appBuilder =
        new InteractiveLatencyApplication.Builder(simulation);
    lqnModel.toLinearizedList().tasks()
        .stream()
        .forEach(t -> appBuilder.task(1, 100, Resource.createMicroInstance(), t.demandTime(), 1));
    appBuilder.workload(workload)
        .thinkTime((float) lqnModel.referenceEntry().thinkTime());

    return Arrays.asList(new InteractiveLatencyApplication(appBuilder));
  }

  public static List<Application> createLqnApplication(Simulation simulation,
                                                       SaviCloudNetwork dataCentreManager,
                                                       String traceName,
                                                       LqnGraph lqnModel) {
    String trace = new File("traces", traceName).toString();
    TraceWorkload workload = new TraceWorkload(simulation, trace, 150, 0);

    HashMap<TaskDescription, Integer> map = new HashMap<>();
    lqnModel.taskDescriptions().keySet()
        .stream()
        .filter(ed -> !ed.equals(lqnModel.referenceTask().name()))
        .forEach(c -> map.put(lqnModel.taskDescriptions().get(c), 3));

    LqnGraph lqnGraph = lqnModel;
    LqnApplication.Builder appBuilder = new LqnApplication.Builder(simulation)
        .datacentreManager(dataCentreManager)
        .workload(workload)
        .lqnGraph(lqnGraph);

    LqnApplication app = appBuilder.build();

    return Arrays.asList(app);
  }
}
