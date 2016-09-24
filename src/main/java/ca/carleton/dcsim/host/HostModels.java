package ca.carleton.dcsim.host;

import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.Host;
import edu.uwo.csd.dcsim.host.power.HostPowerModel;
import edu.uwo.csd.dcsim.host.power.SPECHostPowerModel;


public final class HostModels {

  private HostModels() {
  }

  public static Host.Builder SingleCore(Simulation simulation) {
    HostPowerModel powerModel =
        new SPECHostPowerModel(10, 148, 159, 167, 175, 184, 194, 204, 213, 220, 227, 233);

    return new Host.Builder(simulation).specification(1, 1, 2500, 16384, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder QuadCore(Simulation simulation) {
    //Power Efficiency: 39.41 cpu/watt
    HostPowerModel powerModel =
        new SPECHostPowerModel(10, 93.7, 97, 101, 105, 110, 116, 121, 125, 129, 133, 135);

    return new Host.Builder(simulation).specification(1, 4, 3300, 8192, 1310720, 36864)
        .powerModel(powerModel);
  }
}
