package ca.carleton.dcsim.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Derek Hawker
 */
public class GraphUtilities {

  public static void createPlots(Path simDataFile)
      throws InterruptedException, IOException {

    execute("python3 ./../scripts/gen_dcsim_plots.py " + simDataFile);
    execute("geeqie " + simDataFile);
  }

  public static void execute(String command) throws InterruptedException, IOException {
    System.out.println(command);

    Process solver = null;
    try {
      solver = Runtime.getRuntime().exec(command);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("solver didn't execute");
    }

    // read the output from the command
    solver.waitFor();

    System.out.println("Return value: " + solver.exitValue());
    if (solver.exitValue() == 0) {
      outputProcessStream(new BufferedReader(new InputStreamReader(solver.getInputStream())));
    } else {
      System.out.println("ret code = " + solver.exitValue());
      outputProcessStream(new BufferedReader(new InputStreamReader(solver.getErrorStream())));
    }
  }

  private static void outputProcessStream(BufferedReader stream) throws IOException {
    while (true) {
      String s = stream.readLine();
      if (s == null) {
        return;
      } else {
        System.out.println(s);
      }
    }
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    createPlots(Paths.get(".'"));
  }
}
