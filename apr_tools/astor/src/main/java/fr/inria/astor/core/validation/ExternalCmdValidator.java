package fr.inria.astor.core.validation;

import fr.inria.astor.approaches._3sfix.FileProgramVariant;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.validation.results.TestCasesProgramValidationResult;
import fr.inria.astor.core.validation.results.TestResult;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ExternalCmdValidator extends ProgramVariantValidator {

    protected Logger log = Logger.getLogger(Thread.currentThread().getName());

    @Override
    public VariantValidationResult validate(ProgramVariant variant, ProjectRepairFacade projectFacade) {
        String workDir = ConfigurationProperties.getProperty("location");
        String cmdTestFailing = ConfigurationProperties.getProperty("testfailingcmd");
        String cmdTestAll = ConfigurationProperties.getProperty("testallcmd");
        String srcJavaFolder = ConfigurationProperties.getProperty("srcjavafolder");
        String javaHome = ConfigurationProperties.properties.getProperty("jvm4testexecution").replace("/bin", "");

        String originalSrcFile = "";
        String originSrcContent = "";
        try {
            if (variant.getCompilation() != null) {
                // backup origin source code
                originalSrcFile = variant.getModificationPoints().get(0).getCtClass().getPosition().getFile().getAbsolutePath();
                originSrcContent = FileUtils.readFileToString(new File(originalSrcFile), StandardCharsets.UTF_8);

                // apply patch
                MutationSupporter.currentSupporter.saveSourceCodeOnDiskProgramVariant(variant, workDir + File.separator + srcJavaFolder);
            }

            // validate patch on failing tests first
            log.info("Run previously failing tests...");
            TestResult tr1 = runTests(cmdTestFailing, workDir, javaHome);
            log.info("Failed tests: " + tr1.failures);

            if (variant.getCompilation() != null && tr1.wasSuccessful()) {
                // validate patch on the whole testsuite
                log.info("Run whole testsuite...");
                TestResult tr2 = runTests(cmdTestAll, workDir, javaHome);
                log.info("Failed tests: " + tr2.failures);
                return new TestCasesProgramValidationResult(tr2, tr2.wasSuccessful(), true);
            }
            else { // return test result immediately
                return new TestCasesProgramValidationResult(tr1, tr1.wasSuccessful(), false);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            // restore origin source code
            if (variant.getCompilation() != null) {
                try {
                    FileUtils.write(new File(originalSrcFile), originSrcContent, StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private TestResult runTests(String testCmd, String workDir, String javaHome) throws IOException, InterruptedException {
        Map<String, String> envs = new HashMap<>();
        envs.put("JAVA_HOME", javaHome);
        runShell("python3 read_test_results.py clean " + workDir, workDir, envs);
        List<String> output = runShell(testCmd, workDir, envs);
        String exitCode = output.get(0);
        output.remove(0);
        if (!exitCode.equals("exit code: 0")) {
            System.out.println(exitCode);
        }
//        log.info("Test Output: \n" + String.join("\n", output));
        output = runShell("python3 read_test_results.py " + workDir, workDir, envs);
        output.remove("exit code: 0");

        Set<String> failedTests = readTestResultsFromScripts(output);

        if (failedTests.isEmpty() && !exitCode.equals("exit code: 0")) {
            failedTests.add("Test#failed_due_to_exception");
        }

        TestResult tr = new TestResult();
        tr.failures = failedTests.size();
        tr.failTest.addAll(failedTests);

        return tr;
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

    private List<String> runShell(String command, String workDir, Map<String, String> envs) throws IOException, InterruptedException {
        List<String> params = Arrays.asList(command.split("\\s").clone());

        ProcessBuilder builder = new ProcessBuilder(params);
        builder.directory(new File(workDir));
        builder.redirectOutput();
        builder.redirectErrorStream(true);
        builder.directory();
        builder.environment().put("TZ", "America/Los_Angeles");
        for (String key : envs.keySet()) {
            builder.environment().put(key, envs.get(key));
        }

        Process process = builder.start();

        StreamReaderThread streamReaderThread = new StreamReaderThread(process.getInputStream());
        streamReaderThread.start();

        ProcessWithTimeout processWithTimeout = new ProcessWithTimeout(process);
        int waitTime = 7200000; // 2 hours
        int exitCode = processWithTimeout.waitForProcess(waitTime);
        streamReaderThread.join();

        if (streamReaderThread.isStreamExceptional()) {
            throw new IllegalStateException("Exception Occurred");
        }

        List<String> output = streamReaderThread.getOutput();
        output.add(0, "exit code: " + exitCode);
        return output;
    }

    @Override
    public List<String> findTestCasesToExecute(ProjectRepairFacade projectFacade) {
        return Collections.emptyList();
    }
}

class StreamReaderThread extends Thread {

    private BufferedReader reader;
    private List<String> output;

    private boolean isStreamExceptional;

    public StreamReaderThread(InputStream stream) {
        this.reader = new BufferedReader(new InputStreamReader(stream));
        this.output = new ArrayList<String>();
        isStreamExceptional = false;
    }

    public List<String> getOutput() throws InterruptedException {
        return this.output;
    }

    public boolean isStreamExceptional() {
        return this.isStreamExceptional;
    }

    @Override
    public void run() {
        try {
            /* Read the output from the stream. */
            String o = null;
            while ((o = this.reader.readLine()) != null) {
                output.add(o.trim());
            }
        } catch (IOException e) {
            isStreamExceptional = true;
            e.printStackTrace();
        }
    }
}

class ProcessWithTimeout extends Thread {
    private Process m_process;
    private int m_exitCode = Integer.MIN_VALUE;

    public ProcessWithTimeout(Process p_process) {
        m_process = p_process;
    }

    public int waitForProcess(int p_timeoutMilliseconds) {
        this.start();

        try {
            this.join(p_timeoutMilliseconds);
        } catch (InterruptedException e) {
            m_exitCode = -1402;
            this.interrupt();
        }

        m_process.destroy();

        return m_exitCode;
    }

    @Override
    public void run() {
        try {
            m_exitCode = m_process.waitFor();
        } catch (InterruptedException ignore) {
            m_exitCode = -1402;
            this.m_process.destroy();
        } catch (Exception ex) {
            // Unexpected exception
            m_exitCode = -1402;
            this.m_process.destroy();
        }
    }
}