package ca.carleton.dcsim.application;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Iterator;

import edu.uwo.csd.dcsim.application.InteractiveApplication;
import edu.uwo.csd.dcsim.application.InteractiveTask;
import edu.uwo.csd.dcsim.application.TaskInstance;

/**
 * @author Michael Tighe
 */
public class InteractiveLatencyApplication extends InteractiveApplication {

  public InteractiveLatencyApplication(InteractiveApplication.Builder builder) {
    super(builder);
  }

  @Override
  public boolean updateDemand() {
    boolean res = super.updateDemand();

    return res;
  }

  @Override
  public double getResponseTime() {

    double responseTime = super.getResponseTime();

    for (Pair<InteractiveTask, InteractiveTask> task : new CompareIterator<>(
        tasks)) {
      InteractiveTask parentTask = task.getLeft();
      InteractiveTask childTask = task.getRight();

      double instLatency = 0.0;

      for (TaskInstance parentTaskInstance : parentTask.getInstances()) {
        for (TaskInstance childTaskInstance : childTask.getInstances()) {
          instLatency += DcsimLqnUtilities.taskLatency(simulation, parentTaskInstance,
                                                       childTaskInstance);
        }
      }
      responseTime += instLatency / (parentTask.getInstances().size() +
                                     childTask.getInstances().size());
    }

    return responseTime;
  }
}


/**
 * Iterator for iterating over a collection such that the previous element is returned (left) along
 * with the current element (right)
 */
class CompareIterator<X> implements Iterable<Pair<X, X>> {

  private final Collection<X> xs;

  /**
   * Iterator for iterating over a collection such that the previous element is returned (left)
   * along with the current element (right)
   *
   * @param xs collection to iterate over.
   */
  public CompareIterator(Collection<X> xs) {
    this.xs = xs;
    if (xs.size() <= 1) {
      throw new IllegalArgumentException("Collection xs must have two elements");
    }
  }

  @Override
  public Iterator<Pair<X, X>> iterator() {
    return new PairIterator();
  }

  class PairIterator implements Iterator<Pair<X, X>> {

    private final Iterator<X> iterator;
    private final MutablePair<X, X> _next = new MutablePair<>();
    private int index = 1;

    public PairIterator() {
      iterator = xs.iterator();
      _next.setRight(iterator.next());
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Pair<X, X> next() {
      _next.setLeft(_next.getRight());
      _next.setRight(iterator.next());

      return _next;
    }
  }
}

