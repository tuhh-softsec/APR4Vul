package edu.lu.uni.serval.tbar.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lu.uni.serval.tbar.config.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ShellUtils {

    public static String shellRun(List<String> asList, String buggyProject) throws IOException {
        return shellRun(asList, buggyProject, null);
    }

    public static void saveScripts(String execDir) {
        String fileName = "/read_test_results.py";
        InputStream is = ShellUtils.class.getResourceAsStream(fileName);
        //Read File Content
        String content = null;
        try {
            content = IOUtils.toString(is, StandardCharsets.UTF_8);
            FileUtils.write(new File(execDir + File.separator + fileName), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> runShell(String command, String workDir) throws IOException, InterruptedException {
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
        int exitCode = processWithTimeout.waitForProcess(7200000); // 2 hours
        streamReaderThread.join();

        if (streamReaderThread.isStreamExceptional()) {
            throw new IllegalStateException("Exception Occurred");
        }

        List<String> output = streamReaderThread.getOutput();
        output.add(0, "exit code: " + exitCode);
        return output;
    }

	public static String shellRun(List<String> asList, String buggyProject, String execDir) throws IOException {
		String fileName;
        StringBuilder cmd;
        if (System.getProperty("os.name").toLowerCase().startsWith("win")){
            fileName = Configuration.TEMP_FILES_PATH + buggyProject + ".bat";
            cmd = new StringBuilder(Configuration.TEMP_FILES_PATH + buggyProject + ".bat");
        }
        else {
            fileName = Configuration.TEMP_FILES_PATH + buggyProject + ".sh";
            cmd = new StringBuilder("bash " + fileName);
        }
        File batFile = new File(fileName);
        if (!batFile.exists()){
        	if (!batFile.getParentFile().exists()) {
        		batFile.getParentFile().mkdirs();
        	}
            boolean result = batFile.createNewFile();
            if (!result){
                throw new IOException("Cannot Create bat file:" + fileName);
            }
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(batFile);
            if (execDir != null) {
                outputStream.write("cd ".getBytes());
                outputStream.write((execDir + ";").getBytes());
            }

            for (String arg: asList){
                outputStream.write(arg.getBytes());
            }
        } catch (IOException e){
            if (outputStream != null){
                outputStream.close();
            }
        }
        batFile.deleteOnExit();
//        StringBuilder fullCmd = new StringBuilder();
//        for (String e : asList)
//            fullCmd.append(e).append(" ");
//        fullCmd.substring(0, fullCmd.length()-1);
//        System.err.println("==> Run command: " + fullCmd.toString());
        Process process= Runtime.getRuntime().exec(cmd.toString());

        String results = ShellUtils.getShellOut(process);
        batFile.delete();
        return results;
	}

	private static String getShellOut(Process process) {
		ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new ReadShellProcess(process));
        String returnString = "";
        try {
            returnString = future.get(Configuration.SHELL_RUN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (TimeoutException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (ExecutionException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } finally {
            shutdownProcess(service, process);
        }
        return returnString;
	}

	private static void shutdownProcess(ExecutorService service, Process process) {
		service.shutdownNow();
        try {
			process.getErrorStream().close();
			process.getInputStream().close();
	        process.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        process.destroy();
	}
}

class ReadShellProcess implements Callable<String> {
    public Process process;

    public ReadShellProcess(Process p) {
        this.process = p;
    }

    public synchronized String call() {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream in = null;
        BufferedReader br = null;
        try {
            String s;
            in = new BufferedInputStream(process.getInputStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (sb.length() < 1000000){
                    if (Thread.interrupted()){
                        return sb.toString();
                    }
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
            in = new BufferedInputStream(process.getErrorStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (Thread.interrupted()){
                    return sb.toString();
                }
                if (sb.length() < 1000000){
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
        } catch (IOException e){
//            e.printStackTrace();
        } finally {
            if (br != null){
                try {
                    br.close();
                } catch (IOException e){
                }
            }
            if (in != null){
                try {
                    in.close();
                } catch (IOException e){
                }
            }
            process.destroy();
        }
        FileHelper.outputToFile("logs/compile_log.log", sb, true);
        return sb.toString();
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