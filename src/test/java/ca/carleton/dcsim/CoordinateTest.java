package ca.carleton.dcsim;

import org.junit.Assert;
import org.junit.Test;

public class CoordinateTest {

  @Test
  public void assertParameterOrder() {
    Coordinate c = Coordinate.create(4, 5);

    Assert.assertEquals(c.x(), 4, 0);
    Assert.assertEquals(c.y(), 5, 0);
  }
}