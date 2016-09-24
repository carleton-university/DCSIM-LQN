package edu.uwo.csd.dcsim.host;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class HostSpecificationTest {

  @Test
  public void assertParameterOrder() {
    HostSpecification hs = HostSpecification.create(1, 2, 3, 4, 5, 6);

    assertEquals(hs.numCpu(), 1);
    assertEquals(hs.numCores(), 2);
    assertEquals(hs.coreCapacity(), 3);
    assertEquals(hs.memory(), 4);
    assertEquals(hs.bandwidth(), 5);
    assertEquals(hs.storage(), 6);
  }
}