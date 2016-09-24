package ca.carleton.dcsim.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ca.carleton.lqn.LinearizedPair;
import ca.carleton.lqn.LinearizedTask;
import ca.carleton.lqn.LqnGraph;
import ca.carleton.lqn.core.tasks.TaskDescription;

/**
 * @author Derek Hawker
 */
public class LQNSolverMaxUsers {

  public static void main(String[] args) {
    testcases().stream()
        .forEach(testCaseLqnModel -> {
          LqnGraph lqnGraph = LqnGraph.readLqnModel(testCaseLqnModel);
          LinearizedPair linearized = lqnGraph.toLinearizedList();
          System.out.println(
              testCaseLqnModel.getFileName() + "  " +
              linearized.tasks().stream()
                  .collect(Collectors.summingDouble(lt -> lt.demandTime())) / 10000 * 150);
        });
    Path maxTestCase = testcases().stream()
        .collect(Collectors.maxBy(
            new Comparator<Path>() {
              @Override
              public int compare(Path o1, Path o2) {
                LqnGraph lqnGraph1 = LqnGraph.readLqnModel(o1);
                LqnGraph lqnGraph2 = LqnGraph.readLqnModel(o2);
                return Double.compare(sumOf(lqnGraph1.toLinearizedList()),
                                      sumOf(lqnGraph2.toLinearizedList()));

              }
            })).get();
    System.out.println("Max: " + maxTestCase.getFileName() + " - " +
                       LqnGraph.readLqnModel(maxTestCase).toLinearizedList().tasks().stream()
                           .collect(Collectors.summingDouble(LinearizedTask::demandTime)) / 10000
                       * 150);
  }

  public static double sumOf(LinearizedPair lp) {
    return lp.tasks().stream()
               .collect(Collectors.summingDouble(LinearizedTask::demandTime)) / 10000 * 150;
  }


  public static Map<TaskDescription, Integer> testcaseReplications(Path testCaseLqnModel) {

    LqnGraph lqnGraph = LqnGraph.readLqnModel(testCaseLqnModel);
    LinearizedPair linearized = lqnGraph.toLinearizedList();

    return linearized.tasks().stream()
        .filter(t -> ((int) Math.ceil(t.demandTime() / 10000 * 150)) > 1)
        .collect(
            Collectors.toMap(LinearizedTask::task,
                             t -> (int) Math.ceil(t.demandTime() / 10000 * 150)-1));
  }
  private static List<Path> testcases() {
    try {
      List<Path> lqns = Files.list(Paths.get("..", "testcases", "testcases"))
          .filter(new FindLqnModels())
          .sorted(new TestCaseComparator())
          .collect(Collectors.toList());
      return lqns;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}

class FindLqnModels implements Predicate<Path> {

  @Override
  public boolean test(Path path) {
    return path.getFileName().toString().endsWith(".lqn");
  }
}

class TestCaseComparator implements Comparator<Path> {

  @Override
  public int compare(Path o1, Path o2) {
    Integer i1 = Integer.valueOf(o1.getFileName().toString().replace(".lqn", ""));
    Integer i2 = Integer.valueOf(o2.getFileName().toString().replace(".lqn", ""));
    return i1.compareTo(i2);
  }
}
