package fr.inria.astor.core.faultlocalization;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

public class PerfectFaultLocalization implements FaultLocalizationStrategy {

    @Override
    public FaultLocalizationResult searchSuspicious(ProjectRepairFacade projectToRepair, List<String> testToRun) throws Exception {
        String perfectDataDir = ConfigurationProperties.getProperty("perfectdata");

        Set<String> positiveTestMethods;
        Set<String> negativeTestMethods;

        positiveTestMethods = new HashSet<String>();
        negativeTestMethods = new HashSet<String>();
        File testFile = new File(perfectDataDir, "tests.json");
        Gson gson = new Gson();
        JsonObject jsonObj = gson.fromJson(FileUtils.readFileToString(testFile, "UTF-8"), JsonObject.class);
        JsonObject testsObj = jsonObj.getAsJsonObject("tests");
        JsonArray passingTests = testsObj.getAsJsonArray("passing_tests");
        JsonArray failures = testsObj.getAsJsonArray("failures");

        for (JsonElement item : passingTests) {
            String passingTest = item.getAsString();
            positiveTestMethods.add(passingTest);
        }

        for (JsonElement item : failures) {
            JsonObject failure = item.getAsJsonObject();
            String testClass = failure.get("test_class").getAsString();
            String testMethod = failure.get("test_method").getAsString();
            String failingTest = testClass + "#" + testMethod;
            negativeTestMethods.add(failingTest);
        }

        List<SuspiciousCode> faultyLines = new ArrayList<>();
        File spectraFile = new File(perfectDataDir, "spectra.txt");
        List<String> fLines = FileUtils.readLines(spectraFile, "UTF-8");
        for (int i = 0; i < fLines.size(); i++) {
            String line = fLines.get(i).trim();

            int startIndex = 0;
            int endIndex = line.indexOf('#');
            String className = line.substring(startIndex, endIndex).replace("$", ".");

            String[] info = line.split(":")[1].split(",");
            int lineNumber = Integer.parseInt(info[0].trim());
            double suspValue = Double.parseDouble(info[1].trim());

            SuspiciousCode suspiciousCode = new SuspiciousCode(className, null, suspValue);
            suspiciousCode.setLineNumber(lineNumber);
            faultyLines.add(suspiciousCode);
        }

        return new FaultLocalizationResult(faultyLines, new ArrayList<>(negativeTestMethods));
    }

    @Override
    public List<String> findTestCasesToExecute(ProjectRepairFacade projectFacade) {
        return Collections.emptyList();
    }
}
