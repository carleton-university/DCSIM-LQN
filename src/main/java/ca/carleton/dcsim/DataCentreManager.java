package ca.carleton.dcsim;

import java.util.Map;

import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.host.Host;

/**
 * @author Derek Hawker
 */
abstract public class DataCentreManager {

  /**
   * A map for finding the datacentre a host resides in
   *
   * @return A map of host -> datacentre
   */
  public abstract Map<Host, DataCentre> hostLocationMap();
}
