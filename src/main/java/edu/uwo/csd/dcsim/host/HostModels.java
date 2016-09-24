package edu.uwo.csd.dcsim.host;

import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.host.power.HostPowerModel;
import edu.uwo.csd.dcsim.host.power.SPECHostPowerModel;

public final class HostModels {

  /*
   * Enforce non-instantiable class
   */
  private HostModels() {
  }

  public static Host.Builder SingleCoreSimple(Simulation simulation) {
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 148, 159, 167, 175, 184, 194, 204, 213, 220, 227, 233);

    return new Host.Builder(simulation)
        .specification(1, 1, 2500, 16384, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder ProLiantDL160G5E5420(Simulation simulation) {
    //Power Efficiency: 85.84 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 148, 159, 167, 175, 184, 194, 204, 213, 220, 227, 233);

    return new Host.Builder(simulation)
        .specification(2, 4, 2500, 16384, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder ProLiantDL360G5E5450(Simulation simulation) {
    //Power Efficiency: 83.33 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 180, 190, 200, 210, 221, 234, 247, 258, 270, 281, 288);

    return new Host.Builder(simulation)
        .specification(2, 4, 3000, 16384, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder ProLiantDL380G5QuadCore(Simulation simulation) {
    //Power Efficiency: 46.51 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 172, 177, 182, 187, 195, 205, 218, 229, 242, 252, 258);

    return new Host.Builder(simulation).specification(2, 2, 3000, 8192, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder ProLiantDL380G6EightCore(Simulation simulation) {
    //Power Efficiency: 102.67 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 63.7, 95.3, 109, 118, 125, 133, 142, 153, 164, 175, 187);

    return new Host.Builder(simulation).specification(2, 4, 2400, 8192, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder ProLiantML110G4(Simulation simulation) {
    //Power Efficiency: 31.79 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 86, 89.4, 92.6, 96, 99.5, 102, 106, 108, 112, 114, 117);

    return new Host.Builder(simulation).specification(1, 2, 1860, 4096, 1310720, 36864)
        .powerModel(powerModel);
  }

  public static Host.Builder ProLiantML110G5(Simulation simulation) {
    //Power Efficiency: 39.41 cpu/watt
    HostPowerModel
        powerModel =
        new SPECHostPowerModel(10, 93.7, 97, 101, 105, 110, 116, 121, 125, 129, 133, 135);

    return new Host.Builder(simulation).specification(1, 2, 2660, 4096, 1310720, 36864)
        .powerModel(powerModel);
  }

}
