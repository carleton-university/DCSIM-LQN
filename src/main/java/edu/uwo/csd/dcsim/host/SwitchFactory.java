package edu.uwo.csd.dcsim.host;

import edu.uwo.csd.dcsim.common.ObjectFactory;

/**
 * Constructs Switch instances.
 *
 * @author Gaston Keller
 */
public class SwitchFactory implements ObjectFactory<Switch> {

  private int bandwidth = 0;                        // in KB
  private int nPorts = 0;
  private int power = 0;

  /**
   * Creates an instance of SwitchFactory.
   */
  public SwitchFactory(int bandwidth, int ports, int power) {
    this.bandwidth = bandwidth;
    this.nPorts = ports;
    this.power = power;
  }

  @Override
  public Switch newInstance() {
    return new Switch(bandwidth, nPorts, power);
  }

  public static SwitchFactory switch40g24p() {
    return new SwitchFactory(40000000, 24, 100);
  }

  public static SwitchFactory switch10g48p() {
    return new SwitchFactory(10000000, 48, 100);
  }
}
