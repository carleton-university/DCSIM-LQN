package edu.uwo.csd.dcsim.core;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Random;

import ca.carleton.dcsim.SaviCloudNetwork;
import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.application.Application;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.common.Utility;
import edu.uwo.csd.dcsim.core.events.RecordMetricsEvent;
import edu.uwo.csd.dcsim.core.events.RunMonitorsEvent;
import edu.uwo.csd.dcsim.core.events.TerminateSimulationEvent;
import edu.uwo.csd.dcsim.core.metrics.SimulationMetrics;
import edu.uwo.csd.dcsim.host.Cluster;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.Rack;
import edu.uwo.csd.dcsim.logging.LogPerRunFileAppender;
import edu.uwo.csd.dcsim.logging.SimulationFileAppender;
import edu.uwo.csd.dcsim.logging.SimulationPatternLayout;

//import edu.uwo.csd.ca.carleton.edu.uwo.csd.dcsim.ca.carleton.edu.uwo.csd.dcsim.common.SimTime;

/**
 * Simulation is a simulation of a data centre, which consists of a collection of DataCentres
 * containing Hosts, which host VMs running Applications. Simulation is the main object that
 * controls the execution of the simulation. <p/> All DataCentres in the simulation must be added to
 * this object, as well as all Workload objects feeding applications within the simulation.
 *
 * @author Michael Tighe
 */
public class Simulation implements SimulationEventListener {

  //Logging defaults
  public static final String DEFAULT_CONSOLE_CONVERSION_PATTERN = "%-5p %-50c - %m%n";
  public static final String DEFAULT_MAINFILE_CONVERSION_PATTERN = "%-5p %-50c - %m%n";
  public static final String DEFAULT_MAINFILE_FILE_NAME = "main-%d.log";

  public static final String DEFAULT_LOGGER_CONVERSION_PATTERN = "[%t] [%s] [%p] - %m%n";
  public static final String DEFAULT_LOGGER_DATE_FORMAT = "yyyy_MM_dd'-'HH_mm_ss";
  public static final String DEFAULT_LOGGER_FILE_NAME = "simulation.log";

  //Trace logging defautls
  public static final String DEFAULT_TRACE_CONVERSION_PATTERN = "%s,%m%n";
  public static final String DEFAULT_TRACE_DATE_FORMAT = "yyyy_MM_dd'-'HH_mm_ss";
  public static final String DEFAULT_TRACE_FILE_NAME = "%n-%d.trace";

  //directory constants
  private static String homeDirectory = null;
  private static String LOG_DIRECTORY = "/log";
  private static String CONFIG_DIRECTORY = "/config";

  //the name of property in the simulation properties file that defines the precision with which to report metrics
  private static String METRIC_PRECISION_PROP = "metricPrecision";

  private static ConsoleAppender consoleAppender;
  private static LogPerRunFileAppender mainFileAppender;
  private static Properties properties; //simulation properties

  private static Random random;
  private static Logger simLogger = Logger.getLogger(Simulation.class);

  protected final Logger logger; //logger
  protected final Logger traceLogger; //logger for trace file
  protected boolean enableTrace;
  protected boolean enableProgressOutput = false;

  private String name;            //name of the simulation
  private PriorityQueue<Event> eventQueue;  //contains all future events, in order
  private long simulationTime;        //current time, in milliseconds
  private long lastUpdate;          //in milliseconds
  private long duration;
  //duration of the entire simulation, at which point it terminates
  private long metricRecordStart;
  private boolean recordingMetrics;
  private long eventSendCount = 0;
  protected SimulationMetrics simulationMetrics;

  private long randomSeed;
  private boolean complete = false;

  private final Map<Host, Rack> host2Rack = new HashMap<>();
  private final Map<Rack, Cluster> rack2Cluster = new HashMap<>();
  private final Map<Cluster, DataCentre> cluster2DataCentre = new HashMap<>();

  private List<Application> applications = new ArrayList<>();
  private SaviCloudNetwork datacenterManager;

  private Multiset<String> uniqueIds;

  private final Path tempLqnModelFile;
  private final Path tempLqnsOutputFile;
  {
    try {
      tempLqnModelFile = Files.createTempFile(null, ".lqn");
      tempLqnsOutputFile = Files.createTempFile(null, ".out");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void initializeLogging() {

    PatternLayout consoleLayout = new PatternLayout();
    consoleLayout.setConversionPattern(DEFAULT_CONSOLE_CONVERSION_PATTERN);
    consoleAppender = new ConsoleAppender();
    consoleAppender.setLayout(consoleLayout);
    consoleAppender.setWriter(new OutputStreamWriter(System.out));
    consoleAppender.setName("console");

    //configure root logger
    Logger.getRootLogger().addAppender(consoleAppender);
    Logger.getRootLogger().setLevel(Level.INFO); //Jeevithan change from .ERROR
    simLogger.addAppender(consoleAppender);

    //configure main file root logging
    boolean enableConsoleLogFile = false;
    if (getProperties().getProperty("enableConsoleLogFile") != null) {
      enableConsoleLogFile =
          Boolean.parseBoolean(getProperties().getProperty("enableConsoleLogFile"));
    }

    if (enableConsoleLogFile) {
      PatternLayout consoleFileLayout = new PatternLayout();
      consoleFileLayout.setConversionPattern(DEFAULT_MAINFILE_CONVERSION_PATTERN);
      mainFileAppender = new LogPerRunFileAppender();
      mainFileAppender.setLayout(consoleFileLayout);
      mainFileAppender.setFile(getLogDirectory() + "/" + DEFAULT_MAINFILE_FILE_NAME);
      mainFileAppender.setDateFormat(DEFAULT_LOGGER_DATE_FORMAT);
      mainFileAppender.setName("mainFile");
      mainFileAppender.activateOptions();

      Logger.getRootLogger().addAppender(mainFileAppender);
    }
  }

  private static Properties getProperties() {
    if (Simulation.properties == null) {
      /* Load configuration properties from file SIMULATION_RUN_MONITORS_EVENT*/
      Properties properties = new Properties();

      try {
        properties.load(
            new FileInputStream(Simulation.getConfigDirectory() + "/simulation.properties"));
      } catch (IOException e) {
        throw new RuntimeException("Properties file could not be loaded", e);
      }
      Simulation.properties = properties;
    }

    return properties;
  }

  public Simulation(String name, long randomSeed) {
    this(name);
    this.setRandomSeed(randomSeed); //override Random seed with specified value
  }

  public Simulation(String name) {
    eventQueue = new PriorityQueue<>(1000, EventComparator.eventComparator);
    simulationTime = 0;
    lastUpdate = 0;
    this.name = name;

    //configure simulation logger
    logger = Logger.getLogger(name);
    logger.setLevel(Level.DEBUG); //Jeevithan commented out debug
    //logger.setLevel(Level.INFO);
    
    Logger.getLogger("simLogger").setAdditivity(false);

    //default the simLogger to OFF until we attach an appender
    Logger.getLogger("simLogger").setLevel(Level.OFF);

    boolean enableFileLogging = true;
    if (getProperties().getProperty("enableSimulationLogFile") != null) {
      enableFileLogging =
          Boolean.parseBoolean(getProperties().getProperty("enableSimulationLogFile"));
    }

    String conversionPattern = DEFAULT_LOGGER_CONVERSION_PATTERN;
    String dateFormat = DEFAULT_LOGGER_DATE_FORMAT;

    if (enableFileLogging) {

      Logger.getLogger("simLogger").setLevel(Level.DEBUG); //Jeevithan commented out
      //Logger.getLogger("simLogger").setLevel(Level.INFO);
      
      SimulationFileAppender simAppender = new SimulationFileAppender();

      SimulationPatternLayout patternLayout = new SimulationPatternLayout(this);
      patternLayout.setConversionPattern(conversionPattern);
      simAppender.setAppend(false);
      simAppender.setLayout(patternLayout);
      simAppender.setSimName(name);
      simAppender.setDateFormat(dateFormat);
      simAppender.setFile( getLogDirectory() + "/" + name + ".log");
      simAppender.activateOptions();
      logger.addAppender(simAppender);
    }

    //if detailedConsole
    if (getProperties().getProperty("detailedConsole") != null &&
        Boolean.parseBoolean(getProperties().getProperty("detailedConsole"))) {

      Logger.getLogger("simLogger").setLevel(Level.DEBUG); //Jeevithan commented out
      //Logger.getLogger("simLogger").setLevel(Level.INFO);
      
      logger.addAppender(consoleAppender);

      //if outputting console to main file
      if (getProperties().getProperty("enableConsoleLogFile") != null &&
          Boolean.parseBoolean(getProperties().getProperty("enableConsoleLogFile")) &&
          mainFileAppender != null) {
        logger.addAppender(mainFileAppender);
      }
    }

    //check for 'enableProgressOutput' flag (determines if console messages indicate progression through sim time)
    if (getProperties().getProperty("enableProgressOutput") != null &&
        Boolean.parseBoolean(getProperties().getProperty("enableProgressOutput"))) {
      enableProgressOutput = true;
    }

    //configure simulation trace logger
    enableTrace = false;
    if (getProperties().getProperty("enableTrace") != null) {
      enableTrace = Boolean.parseBoolean(getProperties().getProperty("enableTrace"));
    }

    //create a logger between root and individual trace loggers with additivity false, to
    //prevent trace logs from being passed to the root logger
    Logger.getLogger("traceLogger").setAdditivity(false);

    traceLogger = Logger.getLogger("traceLogger." + name);
    if (enableTrace) {
      traceLogger.setLevel(Level.INFO);

      SimulationFileAppender simAppender = new SimulationFileAppender();

      SimulationPatternLayout patternLayout = new SimulationPatternLayout(this);
      patternLayout.setConversionPattern(DEFAULT_TRACE_CONVERSION_PATTERN);
      simAppender.setLayout(patternLayout);
      simAppender.setSimName(name);
      simAppender.setDateFormat(DEFAULT_TRACE_DATE_FORMAT);
      simAppender.setFile(getLogDirectory() + "/" + DEFAULT_TRACE_FILE_NAME);
      simAppender.activateOptions();
      traceLogger.addAppender(simAppender);

    } else {
      traceLogger.setLevel(Level.OFF);
    }

    //initialize Random
    setRandomSeed(new Random().nextLong());

    Enumeration allAppenders = logger.getAllAppenders();

    simulationMetrics = new SimulationMetrics(this);
    uniqueIds = HashMultiset.create();

  }

  public final SimulationMetrics run(long duration, long metricRecordStart) {

	System.out.println("in Simulation.SimulationMetrics(). Duration: "+ duration + ". metricRecordStart: " + metricRecordStart);
    //ensure this simulation hasn't been run yet
    if (complete) {
      throw new IllegalStateException("Simulation has already been run");
    }

    //Initialize
    List<Host> hosts = getHostList();
    List<Cluster> clusters = getClusterList();

    Event e;

    //configure simulation duration
    this.duration = duration;
    sendEvent(new TerminateSimulationEvent(this),
              duration); //this event runs at the last possible time in the simulation to ensure simulation updates

    if (metricRecordStart > 0) {
      recordingMetrics = false;
      this.metricRecordStart = metricRecordStart;
      sendEvent(new RecordMetricsEvent(this), metricRecordStart);
    } else {
      recordingMetrics = true;
    }

    System.out.println("Test. Starting DCSim");
    simLogger.info("Starting DCSim");
    simLogger.info("Random Seed: " + this.getRandomSeed());

    long nSteps = 0;

    //main event loop
    while (!eventQueue.isEmpty() && simulationTime < duration) {

      simulationMetrics.incrementNSteps();

//			System.out.println(SimTime.toHumanReadable(simulationTime));

      //peak at next event
      e = eventQueue.peek();

      System.out.println("1 event name: "+ e.getClass().getName()); 
      //make sure that the event is in the future
      if (e.getTime() < simulationTime) {
        throw new IllegalStateException(
            "Encountered event (" + e.getClass() + ") with time < current simulation time");
      }

      if (e.getTime() == simulationTime && e.getTime() != 0) {
        throw new IllegalStateException(
            "Encountered event with time == current simulation time when advance in time was expected. This should never occur.");
      }

      //ensure that we are not at time 0. If we are, we do not need to advance the simulation yet, only run events
      if (e.getTime() != 0) {
        //Simulation time is advancing

        //schedule/allocate resources
        scheduleResources(hosts);

        //revise/amend
        postScheduling();

        //get the next event, which may have changed during the revise step
        e = eventQueue.peek();
        System.out.println("2 event name: "+ e.getClass().getName()); 
        
        //make sure that the event is in the future
        if (e.getTime() < simulationTime) {
          throw new IllegalStateException("Encountered post-scheduling event (" + e.getClass()
                                          + ") with time < current simulation time");
        }

        //advance to time e.getTime()
        lastUpdate = simulationTime;
        simulationTime = e.getTime();
        advanceSimulation(hosts);

        // Show progression over time.
        if (enableProgressOutput && simulationTime % SimTime.hours(1) == 0) {
          logger.info(SimTime.toHumanReadable(simulationTime));
        }

        if (this.isRecordingMetrics()) {
          //update host metrics
          simulationMetrics.recordHostMetrics(hosts);
          // If data centre is organized in Clusters, update Cluster metrics.
          if (clusters.size() > 0) {
            simulationMetrics.recordClusterMetrics(clusters);
          }
          //update application metrics
          simulationMetrics.recordApplicationMetrics(applications);
          //generic call to custom metrics to record
          simulationMetrics.recordMetrics();
        }
      }

      // Log current state
      for (DataCentre dc : datacentres()) {
        dc.logState();  //Jeevithan commented out 
        logger.info("Power Consumed Datacentre #" + dc.id() + " " +
                    dc.getCurrentPowerConsumption() + "W");
      }
      logWorkload();
      logResponseTime();

      //execute current events
      try{
    	  
      
	      while (!eventQueue.isEmpty() && (eventQueue.peek().getTime() == simulationTime)) {
	         
	        e = eventQueue.poll();
	        logger.info("in while loop event name: "+ e.getClass().getName());  
	        
	        e.preExecute();
	        e.getTarget().handleEvent(e);  //the target handles the event
	        e.triggerPostExecute();        //run any additional logic required by the event
	        e.triggerCallback();      //trigger any objects awaiting a post-event callback
	      }
      }
      catch(Exception e1)
      {
    	  logger.info("before exception is thrown ========= ");
    	  e1.printStackTrace(); 
    	  logger.info(e1.getStackTrace());
    	  logger.info("cause for exception: " + e1.getCause()); 
      }

    }

    System.out.println("------ simulation completed"); 
    //Simulation is now completed
    simulationMetrics.completeSimulation();
    completeSimulation(duration);

    simLogger.info("");
    simLogger.info("Completed simulation " + name);

    complete = true;

    return simulationMetrics;

  }
  private void logResponseTime() {
    double wkld;
    for (DataCentre dc : datacentres()) {
      wkld = dc.getResponseTime();
      if (wkld >= 0) {
        logger.info("ResponseTime: " + wkld);
        break;
      }
    }
  }

  private void logWorkload() {
    int wkld;
    for (DataCentre dc : datacentres()) {
      wkld = dc.getWorkload();
      if (wkld >= 0) {
        logger.info("Workload: " + wkld);
        break;
      }
    }
  }

  private List<DataCentre> datacentres() {
    return datacenterManager.dataCentres();
  }

  private void scheduleResources(List<Host> hosts) {

    //reset host schedulers
    for (Host host : hosts) {
      //reset all scheduled resources to zero (subsequently, hosts not 'ON' will not be scheduled and will remain at zero)
      host.getResourceScheduler().resetScheduling();
    }

    //initialize Applications (reset scheduled/demand, set scheduled = size)
    for (Application application : applications) {
      application.initializeScheduling();
    }

    //update application demands (includes solving MVA and updating cpu demand)
    for (Application application : applications) {
      application.updateDemand();
    }

    //while not done
    boolean done = false;
    while (!done) {
      done = true;
      //schedule cpu on all hosts (in no order)
      for (Host host : hosts) {
        //schedule cpu
        if (host.getState() == Host.HostState.ON) {
          host.getResourceScheduler().scheduleResources();
        }
      }
      for (Application application : applications) {
        boolean appUpdate = application.updateDemand();
        done = done && !appUpdate; //stop when no calls to updateDemand result in changes
      }
    }


  }

  private void postScheduling() {
    for (Application app : applications) {
      app.postScheduling();
    }
  }

  /**
   * Run all applications up to current simulation time
   */
  private void advanceSimulation(List<Host> hosts) {
    //execute all applications up to the current simulation time
    for (Application app : applications) {
      app.advanceSimulation();
    }

  }

  public void completeSimulation(long duration) {
    logger.info("DCSim Simulation Complete");

    //log simulation time
    double simTime = this.getDuration();
    double recordedTime = this.getRecordingDuration();
    String simUnits = "ms";
    if (simTime >= 864000000) { //>= 10 days
      simTime = simTime / 86400000;
      recordedTime = recordedTime / 86400000;
      simUnits = " days";
    } else if (simTime >= 7200000) { //>= 2 hours
      simTime = simTime / 3600000;
      recordedTime = recordedTime / 3600000;
      simUnits = "hrs";
    } else if (simTime >= 600000) { //>= 2 minutes
      simTime = simTime / 60000d;
      recordedTime = recordedTime / 60000d;
      simUnits = "mins";
    } else if (simTime >= 10000) { //>= 10 seconds
      simTime = simTime / 1000d;
      recordedTime = recordedTime / 1000d;
      simUnits = "s";
    }
    logger.info("Simulation Time: " + simTime + simUnits);
    logger.info("Recorded Time: " + recordedTime + simUnits);

  }

  public final long sendEvent(Event event, long time) {
    event.initialize(this);
    event.setSendOrder(++eventSendCount);
    event.setTime(time);
    eventQueue.add(event);

    return event.getSendOrder();
  }

  public final long sendEvent(Event event) {
    return sendEvent(event, getSimulationTime());
  }

  public final void dequeueEvent(Event event) {
    eventQueue.remove(event);
  }

  @Override
  public final void handleEvent(Event e) {

    if (e instanceof TerminateSimulationEvent) {
      //nothing to do, just let the simulation terminate
    } else if (e instanceof RecordMetricsEvent) {
      recordingMetrics = true;
    } else if (e instanceof RunMonitorsEvent) {
      //nothing to do, this will ensure that the simulation processes the monitors in case no other event is scheduled
    } else {
      throw new RuntimeException("Simulation received unknown event type");
    }

  }

  public final Logger getLogger() {
    return logger;
  }

  public final Logger getTraceLogger() {
    return traceLogger;
  }

  public final boolean isTraceEnabled() {
    return enableTrace;
  }

  public int nextId(String name) {
    uniqueIds.add(name);
    return uniqueIds.count(name);
  }

  public final String getName() {
    return name;
  }

  public final Random getRandom() {
    if (random == null) {
      random = new Random();
      setRandomSeed(random.nextLong());
    }

    return random;
  }

  public final long getRandomSeed() {
    return randomSeed;
  }

  public final void setRandomSeed(long seed) {
    randomSeed = seed;
    random = new Random(randomSeed);
  }

  public final long getSimulationTime() {
    return simulationTime;
  }

  public final long getDuration() {
    return duration;
  }

  public final long getMetricRecordStart() {
    return metricRecordStart;
  }

  public final long getRecordingDuration() {
    return duration - metricRecordStart;
  }

  public final long getLastUpdate() {
    return lastUpdate;
  }

  public final long getElapsedTime() {
    return simulationTime - lastUpdate;
  }

  public final double getElapsedSeconds() {
    return getElapsedTime() / 1000d;
  }

  public final boolean isRecordingMetrics() {
    return recordingMetrics;
  }

  public final SimulationMetrics getSimulationMetrics() {
    return simulationMetrics;
  }

  /**
   * Helper functions
   */

  /**
   * Get the directory of the manager application
   *
   * @return The directory of the manager application
   */
  public static String getHomeDirectory() {
    if (homeDirectory == null) {
      File dir = new File(".");
      try {
        homeDirectory = dir.getCanonicalPath();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return homeDirectory;
  }

  /**
   * Get the directory that contains log files
   *
   * @return The directory that contains log files.
   */
  public static String getLogDirectory() {
    return getHomeDirectory() + LOG_DIRECTORY;
  }

  /**
   * Get the directory that contains configuration files
   *
   * @return The directory that contains configuration files.
   */
  public static String getConfigDirectory() {
    return getHomeDirectory() + CONFIG_DIRECTORY;
  }

  public static boolean hasProperty(String name) {

    if (System.getProperty(name) != null || getProperties().getProperty(name) != null) {
      return true;
    }
    return false;
  }

  /**
   * Retrieve an application property from the configuration file or command line options. If a
   * property is specified in both, then the command line overrides the properties file.
   *
   * @param name Name of property.
   *
   * @return The value of the property.
   */
  public static String getProperty(String name) {
    String prop = null;
    if (System.getProperty(name) != null) {
      prop = System.getProperty(name);
    } else {
      prop = getProperties().getProperty(name);
    }

    if (prop == null) {
      throw new RuntimeException("Simulation property '" + name + "' not found");
    }

    return prop;
  }

  /**
   * Determine if the property specifying the precision that metrics should be reported with has
   * been set
   *
   * @return True, if metric precision has been set
   */
  public static boolean isMetricPrecisionSet() {
    return hasProperty(METRIC_PRECISION_PROP);
  }

  /**
   * Get the precision that metrics should be reported with
   *
   * @return The precision of metrics, or -1 if none has been set
   */
  public static int getMetricPrecision() {
    if (isMetricPrecisionSet()) {
      return Integer.parseInt(getProperty(METRIC_PRECISION_PROP));
    }
    return -1;
  }

  /**
   * Round double values to the specified metric precision
   */
  public static double roundToMetricPrecision(double value) {
    if (isMetricPrecisionSet()) {
      return Utility.roundDouble(value, getMetricPrecision());
    }
    return value;
  }

  /**
   * Add a DataCentre to the simulation
   */
  private void addDatacentre(DataCentre dc) {
    for (Cluster cluster : dc.clusters()) {
      cluster2DataCentre.put(cluster, dc);
      addCluster(cluster);
    }
  }

  private void addCluster(Cluster cluster) {
    for (Rack rack : cluster.racks()) {
      rack2Cluster.put(rack, cluster);
      addRack(rack);
    }
  }

  private void addRack(Rack rack) {
    for (Host host : rack.hosts()) {
      host2Rack.put(host, rack);
    }
  }

  public void addApplication(Application application) {
    applications.add(application);
  }

  public void removeApplication(Application application) {
    applications.remove(application);
  }

  /**
   * Get a list of all of the Hosts within the simulation
   */
  private List<Host> getHostList() {

    int nHosts = 0;
    for (DataCentre dc : datacentres()) {
      nHosts += dc.numHosts();
    }

    ArrayList<Host> hosts = new ArrayList<Host>(nHosts);

    for (DataCentre dc : datacentres()) {
      for (Cluster cluster : dc.clusters()) {
        for (Rack rack : cluster.racks()) {
          hosts.addAll(rack.hosts());
        }
      }
    }

    return hosts;
  }

  /**
   * Get a list of all of the Hosts within the simulation
   */
  private List<Cluster> getClusterList() {
    int nClusters = 0;
    for (DataCentre dc : datacentres()) {
      nClusters += dc.clusters().size();
    }

    ArrayList<Cluster> clusters = new ArrayList<Cluster>(nClusters);

    for (DataCentre dc : datacentres()) {
      clusters.addAll(dc.clusters());
    }

    return clusters;
  }

  /**
   * Return the Rack containing the given host if in a rack
   */
  public Rack host2Rack(Host host) {
    return host2Rack.get(host);
  }

  /**
   * Return the cluster containing a given host if in a cluster.
   */
  public Cluster host2Cluster(Host host) {
    return rack2Cluster(host2Rack(host));
  }

  /**
   * Return the Datacentre containing a given host, if in a datacentre.
   */
  public DataCentre host2DataCentre(Host host) {
    return cluster2DataCentre(host2Cluster(host));
  }

  /**
   * Return the cluster containing a given Rack, if in a cluster.
   */
  public Cluster rack2Cluster(Rack rack) {
    return rack2Cluster.get(rack);
  }

  /**
   * Return the datacentre containing a given cluster, if in a datacentre.
   */
  public DataCentre cluster2DataCentre(Cluster cluster) {
    DataCentre dataCentre = cluster2DataCentre.get(cluster);
    return dataCentre;
  }

  public void addSaviNetwork(SaviCloudNetwork saviNetwork) {
    this.datacenterManager = saviNetwork;
    for (DataCentre dataCentre : saviNetwork.dataCentres()) {
      addDatacentre(dataCentre);
    }
  }

  public SaviCloudNetwork datacenterManager() {
    return datacenterManager;
  }

  public Path getTempLqnsOutputFile() {
    return tempLqnsOutputFile;
  }
  public Path getTempLqnModelFile() {
    return tempLqnModelFile;
  }
}
