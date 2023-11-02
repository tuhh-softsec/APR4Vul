package edu.lu.uni.serval.tbar.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.lu.uni.serval.tbar.AbstractFixer;
import edu.lu.uni.serval.tbar.TBarFixer;
import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.utils.ShellUtils;

/**
 * Fix bugs with Fault Localization results.
 * 
 * @author kui.liu
 *
 */
public class Main {

	public static void main(String[] args) {
//		args = Bugs.KB_809;

		if (args.length < 6 || args.length > 10) {
			System.err.println("Arguments: \n"
					+ "\t<Project_Folder>: the directory of target project. \n"
					+ "\t<Failing_Tests>: list of failing test cases, e.g., a.b.C#m1,e.m.N#m2. \n"
					+ "\t<Source_Path>: path to java source folder, e.g., src/main/java/. \n"
					+ "\t<Test_Source_Path>: path to java test folder, e.g., /src/test/java/. \n"
					+ "\t<Class_Path>: path to java source class folder, e.g., /target/classes/. \n"
					+ "\t<Test_Class_Path>: path to java test class folder, e.g., /target/test-classes/. \n"
					+ "\t<FL_File_Path>: (optional) path to the GZoltar FL result file, e.g., /path/to/fl.txt. \n"
					+ "\t<Test_Failing_CMD>: (optional) command to run failing tests. \n"
					+ "\t<Test_All_CMD>: (optional) command to run all tests. \n"
					+ "\t<Extra_Class_Path>: (optional) list of extra class paths, e.g., class path for external libraries. \n");
			System.exit(0);
		}

		String projectFolder = args[0];
		String failedTestsStr = args[1];

		Configuration.srcPath = args[2];
		Configuration.testSrcPath = args[3];
		Configuration.classPath = args[4];
		Configuration.testClassPath = args[5];

		String flFilePath = args.length > 6 ? args[6] : "";
		Configuration.cmdTestFailing = args.length > 7 ? args[7] : "";
		Configuration.cmdTestAll = args.length > 8 ? args[8] : "";
		Configuration.extraClasspath = args.length > 9 ? args[9] : "";

//		ShellUtils.saveScripts(projectFolder);
		List<String> failedTests = new ArrayList<>();

		for (String failedTest : failedTestsStr.split(",")) {
			if (!failedTests.contains(failedTest)) {
				failedTests.add(failedTest);
			}
		}

		if (!flFilePath.isEmpty() && !new File(flFilePath).isFile()) {
			System.err.println("FL File not found!");
			System.exit(1);
		}

		AbstractFixer fixer = new TBarFixer(projectFolder, failedTests, flFilePath, "BuggyProject");
		fixer.dataType = "TBar";

		fixer.fixProcess();

		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
			case 0:
				System.out.println("Failed to fix bug");
				break;
			case 1:
				System.out.println("Succeeded to fix bug");
				break;
			case 2:
				System.out.println("Partial succeeded to fix bug");
				break;
		}
	}
}
