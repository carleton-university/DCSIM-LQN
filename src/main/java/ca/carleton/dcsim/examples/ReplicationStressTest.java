package ca.carleton.dcsim.examples;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.nio.file.Paths;
import java.util.Map;

import ca.carleton.lqn.LqnGraph;
import ca.carleton.lqn.LqnsResults;
import ca.carleton.lqn.core.tasks.TaskDescription;

/**
 * @author Derek Hawker
 */
public class ReplicationStressTest {

  public static void main(String[] args) {
    LqnGraph lqnGraph = LqnGraph.readLqnModel(
        Paths.get("..", "lqns-files", "hcat-deployable.lqn"));
    LqnsResults results;
    LqnGraph updatedLqn;

    Multimap<TaskDescription, TaskDescription> replications = HashMultimap.create();

    for (int i = 1; i < 1000; i++) {
      updatedLqn = lqnGraph.withNumUsers(i)
          .withReplications(replications);
      results = updatedLqn.solveLqnModel();

      System.out.println("\nTask Utils.");
      for (Map.Entry<String, Double> e : results.taskUtilizations().entrySet()) {
        System.out.println(e);
      }
      System.out.println("\nService Times.");
      for (Map.Entry<String, Double> e : results.taskServiceTimes().entrySet()) {
        System.out.println(e);
      }

      for (Map.Entry<String, TaskDescription> e : lqnGraph.taskDescriptions().entrySet()) {
        if (e.getValue().equals(lqnGraph.referenceTask())) {
          continue;
        }

        double util = 0;
        for (String taskName : results.taskUtilizations().keySet()) {
          util = Math.max(util,
                          results.processorUtilizations().get(
                              updatedLqn.taskDescriptions().get(taskName)
                                  .processor()));
        }
        if (util > 0.8) {
          TaskDescription td = e.getValue();
          replications.put(td,
                           td.copyWithName(td.name() +
                                           Integer.toString(replications.get(td).size() + 1))
                               .copyWithProcessor(
                                   td.processor() + Integer.toString(
                                       replications.get(td).size() + 1)));
          if (replications.get(td).size() == 1) {
            replications.put(td,
                             td.copyWithName(td.name() +
                                             Integer.toString(replications.get(td).size() + 1))
                                 .copyWithProcessor(
                                     td.processor() + Integer.toString(
                                         replications.get(td).size() + 1)));
          }
        }
      }
    }
  }
}