package edu.uwo.csd.dcsim.host;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResourceTest {

  @Test
  public void assertParameterOrder() {
    Resource r = Resource.create(1, 2, 3, 4, 5, 6);

    assertEquals(r.numCpus(), 1);
    assertEquals(r.numCores(), 2);
    assertEquals(r.cpu(), 3);
    assertEquals(r.memory(), 4);
    assertEquals(r.bandwidth(), 5);
    assertEquals(r.storage(), 6);

    assertEquals(r.withCpu(0), Resource.create(1, 2, 0, 4, 5, 6));
    assertEquals(r.withBandwidth(0), Resource.create(1, 2, 3, 4, 0, 6));
    assertEquals(r.withMemory(0), Resource.create(1, 2, 3, 0, 5, 6));
    assertEquals(r.withStorage(0), Resource.create(1, 2, 3, 4, 5, 0));

    assertEquals(Resource.copyOf(r), r);
  }

}