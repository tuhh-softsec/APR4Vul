package us.msu.cse.repair.core.testexecutors;

import org.apache.commons.io.FileUtils;
import us.msu.cse.repair.core.util.ProcessWithTimeout;
import us.msu.cse.repair.core.util.StreamReaderThread;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RExternalTestExecutor extends ExternalTestExecutor {
  String projectDir;
  String testCommand;
  Map<String, String> modifiedJavaSources;

  public RExternalTestExecutor(Set<String> positiveTests, Set<String> negativeTests,
                               String projectDir, String testCommand, int waitTime,
                               Map<String, String> modifiedJavaSources) {
    this.positiveTests = positiveTests;
    this.negativeTests = negativeTests;
    this.projectDir = projectDir;
    this.testCommand = testCommand;
    this.waitTime = 7200000; // 2 hours
    this.modifiedJavaSources = modifiedJavaSources;
  }

  @Override
  public boolean runTests() throws IOException, InterruptedException {
    Map<String, String> backupJavaSources = new HashMap<>();
    try {
      // Backup Java Sources which will be modified
      for (String javaFile : modifiedJavaSources.keySet()) {
        backupJavaSources.put(javaFile, FileUtils.readFileToString(new File(javaFile), StandardCharsets.UTF_8));
      }

      // Update the patch
      for (String javaFile : modifiedJavaSources.keySet()) {
        FileUtils.write(new File(javaFile), modifiedJavaSources.get(javaFile), StandardCharsets.UTF_8);
      }

      try {
        System.out.println("----------------------------------");
        System.out.println("Patch Candidate:");
        List<String> gitDiff = runShell("git diff", projectDir);
        for (int i = 4; i < gitDiff.size(); i++) {
          String line = gitDiff.get(i);
//        if (line.startsWith("@@") || line.startsWith("+") || line.startsWith("-")) {
          System.out.println(line);
//        }
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      // run tests with Maven/Gradle
      runShell("python3 read_test_results.py clean " + projectDir, projectDir);
      List<String> output = runShell(testCommand, projectDir);
      String exitCode = output.get(0);
      output.remove(0);

      // read test execution results
      output = runShell("python3 read_test_results.py " + projectDir, projectDir);
      output.remove("exit code: 0");
      failedTests = readTestResultsFromScripts(output);

      for (String test : failedTests) {
        if (negativeTests.contains(test))
          failuresInNegative++;
        else
          failuresInPositive++;
      }

      if (failedTests.isEmpty() && !exitCode.equals("exit code: 0")) {
        System.out.println(exitCode);
        failedTests.addAll(negativeTests);
        failuresInNegative = failedTests.size();
      }
    }
    catch (Exception e) {
      isExceptional = true;
      e.printStackTrace();
      failedTests = new HashSet<>();
      failedTests.add("FailedTest#test_executor_timeout");
      failuresInNegative++;
    }
    finally {
      // Restore the original java sources
      for (String javaFile : backupJavaSources.keySet()) {
        FileUtils.write(new File(javaFile), backupJavaSources.get(javaFile), StandardCharsets.UTF_8);
      }
    }
    return failedTests.isEmpty();
  }

  private List<String> runShell(String command, String workDir) throws IOException, InterruptedException {
    List<String> params = Arrays.asList(command.split("\\s").clone());

    ProcessBuilder builder = new ProcessBuilder(params);
    builder.directory(new File(workDir));
    builder.redirectOutput();
    builder.redirectErrorStream(true);
    builder.directory();
    builder.environment().put("TZ", "America/Los_Angeles");

    Process process = builder.start();

    StreamReaderThread streamReaderThread = new StreamReaderThread(process.getInputStream());
    streamReaderThread.start();

    ProcessWithTimeout processWithTimeout = new ProcessWithTimeout(process);
    int exitCode = processWithTimeout.waitForProcess(waitTime);
    streamReaderThread.join();

//    System.out.println("Shell Process results: " + exitCode + ", " + streamReaderThread.isStreamExceptional());

    if (streamReaderThread.isStreamExceptional()) {
      throw new IllegalStateException("Exception Occurred");
    }

    List<String> output = streamReaderThread.getOutput();
    output.add(0, "exit code: " + exitCode);
    return output;
  }

  private Set<String> readTestResultsFromScripts(List<String> output) {
    Set<String> failingTests = new HashSet<>();
    for (String testResult : output) {
      if (testResult.isEmpty()) continue;
//      if (testResult.trim().contains(" ") || testResult.contains("$")) continue;
      failingTests.add(testResult.trim());
    }
    return failingTests;
  }
}
