package us.msu.cse.repair.core.faultlocalizer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import us.msu.cse.repair.core.parser.LCNode;

public class GZoltarFaultLocalizer2 implements IFaultLocalizer {
	Set<String> positiveTestMethods;
	Set<String> negativeTestMethods;

	Map<LCNode, Double> faultyLines;

	public GZoltarFaultLocalizer2(String gzoltarDataDir) throws IOException {
		positiveTestMethods = new HashSet<String>();
		negativeTestMethods = new HashSet<String>();
		File testFile = new File(gzoltarDataDir, "tests.json");
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

		faultyLines = new HashMap<LCNode, Double>();
		File spectraFile = new File(gzoltarDataDir, "spectra.txt");
		List<String> fLines = FileUtils.readLines(spectraFile, "UTF-8");
		for (int i = 0; i < fLines.size(); i++) {
			String line = fLines.get(i).trim();

			int startIndex = 0;
			int endIndex = line.indexOf('#');
			String className = line.substring(startIndex, endIndex).replace("$", ".");

			String[] info = line.split(":")[1].split(",");
			int lineNumber = Integer.parseInt(info[0].trim());
			double suspValue = Double.parseDouble(info[1].trim());

			LCNode lcNode = new LCNode(className, lineNumber);
			faultyLines.put(lcNode, suspValue);
		}
	}

	@Override
	public Map<LCNode, Double> searchSuspicious(double thr) {
		// TODO Auto-generated method stub
		Map<LCNode, Double> partFaultyLines = new HashMap<LCNode, Double>();
		for (Map.Entry<LCNode, Double> entry : faultyLines.entrySet()) {
			if (entry.getValue() >= thr)
				partFaultyLines.put(entry.getKey(), entry.getValue());
		}
		return partFaultyLines;
	}

	@Override
	public Set<String> getPositiveTests() {
		// TODO Auto-generated method stub
		return this.positiveTestMethods;
	}

	@Override
	public Set<String> getNegativeTests() {
		// TODO Auto-generated method stub
		return this.negativeTestMethods;
	}
}
