package edu.lu.uni.serval.tbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import edu.lu.uni.serval.faultlocalization.FL;
import edu.lu.uni.serval.faultlocalization.SuspiciousCode;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.code.analyser.JavaCodeFileParser;
import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.context.Dictionary;
import edu.lu.uni.serval.tbar.dataprepare.DataPreparer;
import edu.lu.uni.serval.tbar.info.Patch;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import edu.lu.uni.serval.tbar.utils.FileUtils;
import edu.lu.uni.serval.tbar.utils.PathUtils;
import edu.lu.uni.serval.tbar.utils.ShellUtils;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;
import edu.lu.uni.serval.tbar.utils.TestUtils;

/**
 * Abstract Fixer.
 * 
 * @author kui.liu
 *
 */
public abstract class AbstractFixer implements IFixer {
	
	private static Logger log = LoggerFactory.getLogger(AbstractFixer.class);
	
	public String metric = "Ochiai";          // Fault localization metric.
	protected String path = "";
	protected String buggyProject = "";     // The buggy project name.
	public int minErrorTest;                // Number of failed test cases before fixing.
	protected int minErrorTestAfterFix = 0; // Number of failed test cases after fixing
	protected String fullBuggyProjectPath;  // The full path of the local buggy project.
	public String outputPath = "";          // Output path for the generated patches.
	protected DataPreparer dp;              // The needed data of buggy program for compiling and testing.
	
	private String failedTestCaseClasses = ""; // Classes of the failed test cases before fixing.
	// All specific failed test cases after testing the buggy project with defects4j command in Java code before fixing.
	protected List<String> failedTestStrList = new ArrayList<>();
	// All specific failed test cases after testing the buggy project with defects4j command in terminal before fixing.
	protected List<String> failedTestCasesStrList = new ArrayList<>();
	// The failed test cases after running defects4j command in Java code but not in terminal.
	private List<String> fakeFailedTestCasesList = new ArrayList<>();
	private List<String> fakeIgnoredTestCasesList = new ArrayList<>();

	// 0: failed to fix the bug, 1: succeeded to fix the bug. 2: partially succeeded to fix the bug.
	public int fixedStatus = 0;
	public String dataType = "";
	protected int patchId = 0;
//	private TimeLine timeLine;
	protected Dictionary dic = null;
	
	public boolean isTestFixPatterns = false;
	protected String flFilePath;

	public AbstractFixer(String projectPath, List<String> failedTests, String flFilePath, String bugId) {
		// set path to project
		fullBuggyProjectPath = projectPath;
		buggyProject = bugId;
		this.flFilePath = flFilePath;

		// set failed test method names
		// org.apache.commons.lang3.reflect.MethodUtilsTest#testGetMethodsWithAnnotationSearchSupersButNotIgnoreAccess
		fakeFailedTestCasesList.addAll(failedTests);
//		fakeIgnoredTestCasesList.addAll(ignoredTests);

		// set failed test class names
		// org.apache.commons.lang3.reflect.MethodUtilsTest
		for (String failedTest : failedTests) {
			int idx = failedTest.indexOf("#");
			String x = idx > 0 ? failedTest.substring(0, idx) : failedTest;
			if (!failedTestCasesStrList.contains(x)) {
				failedTestCasesStrList.add(x);
			}
		}

		for (String ft : failedTestCasesStrList) {
			failedTestCaseClasses += ft + " ";
		}

		// set number of failed tests
		minErrorTest = failedTests.size();

		// set up DataPreparer
		File projectFile = new File(projectPath);
//		String projectParentPath = projectFile.getParent();
		String projectName = projectFile.getName();
		this.dp = new DataPreparer(projectPath);
		dp.prepareData();

		log.info(projectName + " Failed Tests: " + this.minErrorTest);
	}

	@Override
	public List<SuspiciousPosition> faultLocalization() {
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();

		if (!this.flFilePath.isEmpty()) { // Use predefined FL positions
			String[] flInfo = FileHelper.readFile(this.flFilePath).split("\n");

			for (String info : flInfo) {
				SuspiciousPosition sp = new SuspiciousPosition();
				sp.classPath = info.split("#")[0].replace("$", ".");
				sp.lineNumber = Integer.parseInt(info.split(":")[1].split(",")[0]);
				suspiciousCodeList.add(sp);
			}
		}
		else { // Compute FL with GZoltar
			FL fl = new FL();
			fl.locateSuspiciousCode(this.dp, this.metric);

			List<SuspiciousCode> suspStmts = fl.suspStmts;
			for (int index = 0, size = suspStmts.size(); index < size; index ++) {
				SuspiciousCode candidate = suspStmts.get(index);
				SuspiciousPosition sp = new SuspiciousPosition();
				sp.classPath = candidate.getClassName();
				sp.lineNumber = candidate.lineNumber;
				suspiciousCodeList.add(sp);
			}
		}

		return suspiciousCodeList;
	}

	protected void checkCompiling(String sourceFilePath, File classFile) {
		try {
			String result = ShellUtils.shellRun(Arrays.asList("javac -Xlint:unchecked -cp "
					+ PathUtils.buildCompileClassPath(dp.classPath, dp.testClassPath, dp.libPaths)
					+ " -d " + dp.classPath + " " + sourceFilePath), buggyProject);
			//log.debug(result);
		} catch (IOException e) {
			e.printStackTrace();
			log.debug(buggyProject + " ---Fixer: fix fail because of javac exception! ");
			System.exit(1);
		}
		if (!classFile.exists()) {
			System.err.println("Please make project compilable first");
			System.exit(1);
		}
	}

	@SuppressWarnings("unused")
	private void createDictionary() {
		dic = new Dictionary();
		List<File> javaFiles = FileHelper.getAllFiles(dp.srcPath, ".java");
		for (File javaFile : javaFiles) {
			JavaCodeFileParser jcfp = new JavaCodeFileParser(javaFile);
			dic.setAllFields(jcfp.fields);
			dic.setImportedDependencies(jcfp.importMaps);
			dic.setMethods(jcfp.methods);
			dic.setSuperClasses(jcfp.superClassNames);
		}
	}

	@Override
	public List<SuspCodeNode> parseSuspiciousCode(SuspiciousPosition suspiciousCode) {
		String suspiciousClassName = suspiciousCode.classPath;
		int buggyLine = suspiciousCode.lineNumber;
		
		log.debug(suspiciousClassName + " ===" + buggyLine);
		if (suspiciousClassName.contains("$")) {
			suspiciousClassName = suspiciousClassName.substring(0, suspiciousClassName.indexOf("$"));
		}
		String suspiciousJavaFile = suspiciousClassName.replace(".", "/") + ".java";
		
		suspiciousClassName = suspiciousJavaFile.substring(0, suspiciousJavaFile.length() - 5).replace("/", ".");
		
		String filePath = dp.srcPath + suspiciousJavaFile;
		File suspCodeFile = new File(filePath);
		if (!suspCodeFile.exists()) return null;
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		scp.parseSuspiciousCode(new File(filePath), buggyLine);
		
		List<Pair<ITree, String>> suspiciousCodePairs = scp.getSuspiciousCode();
		if (suspiciousCodePairs.isEmpty()) {
			log.debug("Failed to identify the buggy statement in: " + suspiciousClassName + " --- " + buggyLine);
			return null;
		}
		
		File targetJavaFile = new File(FileUtils.getFileAddressOfJava(dp.srcPath, suspiciousClassName));
        File targetClassFile = new File(FileUtils.getFileAddressOfClass(dp.classPath, suspiciousClassName));
        File javaBackup = new File(FileUtils.tempJavaPath(suspiciousClassName,  this.dataType + "/" + this.buggyProject));
        File classBackup = new File(FileUtils.tempClassPath(suspiciousClassName, this.dataType + "/" + this.buggyProject));
        try {
        	if (!targetClassFile.exists()) return null;
        	if (javaBackup.exists()) javaBackup.delete();
        	if (classBackup.exists()) classBackup.delete();
			Files.copy(targetJavaFile.toPath(), javaBackup.toPath());
			Files.copy(targetClassFile.toPath(), classBackup.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        List<SuspCodeNode> scns = new ArrayList<>();
		for (Pair<ITree, String> suspCodePair : suspiciousCodePairs) {
			ITree suspCodeAstNode = suspCodePair.getFirst(); //scp.getSuspiciousCodeAstNode();
			String suspCodeStr = suspCodePair.getSecond(); //scp.getSuspiciousCodeStr();
			log.debug("Suspicious Code: \n" + suspCodeStr);
			
			int startPos = suspCodeAstNode.getPos();
			int endPos = startPos + suspCodeAstNode.getLength();
			SuspCodeNode scn = new SuspCodeNode(javaBackup, classBackup, targetJavaFile, targetClassFile, 
	        		startPos, endPos, suspCodeAstNode, suspCodeStr, suspiciousJavaFile, buggyLine);
			scns.add(scn);
		}
        return scns;
	}

	protected List<Patch> triedPatchCandidates = new ArrayList<>();

	private boolean printJunitRunningLog = false;

	protected void testGeneratedPatches(List<Patch> patchCandidates, SuspCodeNode scn) {
		// Testing generated patches.
		for (Patch patch : patchCandidates) {
			patch.buggyFileName = scn.suspiciousJavaFile;
			addPatchCodeToFile(scn, patch);// Insert the patch.
			if (this.triedPatchCandidates.contains(patch)) continue;
			patchId++;
			this.triedPatchCandidates.add(patch);
			
			String buggyCode = patch.getBuggyCodeStr();
			if ("===StringIndexOutOfBoundsException===".equals(buggyCode)) continue;
			String patchCode = patch.getFixedCodeStr1();
			scn.targetClassFile.delete();

			String patchCandidateStr = TestUtils.readPatch(this.fullBuggyProjectPath);
			String patchCandidateLines = "";
			for (int i = 4; i < patchCandidateStr.split("\n").length; i++) {
				String line = patchCandidateStr.split("\n")[i];
				line = line.trim();
				if (line.startsWith("@@") || line.startsWith("+") || line.startsWith("-")) {
					patchCandidateLines += "\n" + line;
				}
			}
			log.debug("Patch Candidate:" + patchCandidateLines);

			log.debug("Compiling");
			try {// Compile patched file.
				String result = ShellUtils.shellRun(Arrays.asList("javac -Xlint:unchecked -cp "
						+ PathUtils.buildCompileClassPath(dp.classPath, dp.testClassPath, dp.libPaths)
						+ " -d " + dp.classPath + " " + scn.targetJavaFile.getAbsolutePath()), buggyProject);
				//log.debug(result);
//				System.out.println("Compile cmd: " + (Arrays.asList("javac -Xlint:unchecked -cp "
//					+ PathUtils.buildCompileClassPath(dp.classPath, dp.testClassPath, dp.libPaths)
//					+ " -d " + dp.classPath + " " + scn.targetJavaFile.getAbsolutePath())));
			} catch (IOException e) {
				e.printStackTrace();
				log.debug(buggyProject + " ---Fixer: fix fail because of javac exception! ");
				continue;
			}
			if (!scn.targetClassFile.exists()) { // fail to compile
				log.debug(buggyProject + " ---Fixer: fix fail because of failed compiling! ");
				continue;
			}
//			log.debug("Finish of compiling.");

			log.debug("Test previously failed test cases.");
			List<String> failedTestsAfterFix = new ArrayList<>();
			int errorTestAfterFix;
			try {
				String results = "";
				String exitCode = "";
				List<String> output = null;
				List<String> tempFailedTestCases = new ArrayList<>();
				if (!Configuration.cmdTestFailing.isEmpty()) {
					ShellUtils.shellRun(Arrays.asList("python3 read_test_results.py clean " + fullBuggyProjectPath), buggyProject, fullBuggyProjectPath);
					output = ShellUtils.runShell(Configuration.cmdTestFailing, fullBuggyProjectPath);
					exitCode = output.get(0);
					output.remove(0);
					if (!exitCode.equals("exit code: 0")) {
						System.out.println(exitCode);
					}
					results = ShellUtils.shellRun(Arrays.asList("python3 read_test_results.py " + fullBuggyProjectPath), buggyProject, fullBuggyProjectPath);
					tempFailedTestCases = readTestResultsFromScripts(results);
				}
				else {
					results = ShellUtils.shellRun(Arrays.asList("java -cp "
							+ PathUtils.buildTestClassPath(dp.classPath, dp.testClassPath, dp.libPaths)
							+ " org.junit.runner.JUnitCore " + this.failedTestCaseClasses), buggyProject, fullBuggyProjectPath);
					if (results.isEmpty()) continue;
					tempFailedTestCases = readTestResults(results);
				}
				failedTestsAfterFix.addAll(tempFailedTestCases);
				// tempFailedTestCases might be different from this.fakeFailedTestCasesList, only class name <-> class#method
//				failedTestsAfterFix.retainAll(this.fakeFailedTestCasesList);
				errorTestAfterFix = failedTestsAfterFix.size();

				if (failedTestsAfterFix.size() > 0) {
					continue;
				} else if (!exitCode.equals("exit code: 0")) {
					System.out.println("Output of Test PoV: " + output);
					continue;
				}

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				log.debug(buggyProject + " ---Fixer: fix fail because of faile passing previously failed test cases! ");
				continue;
			}

			log.debug("Test the whole test suite.");
			try {
				String results = "";
				String exitCode = "";
				List<String> output = null;
				List<String> tempFailedTestCases = new ArrayList<>();
				if (!Configuration.cmdTestAll.isEmpty()) {
					ShellUtils.shellRun(Arrays.asList("python3 read_test_results.py clean " + fullBuggyProjectPath), buggyProject, fullBuggyProjectPath);
//					System.out.println("Test all cmd: " + Configuration.cmdTestAll);
					output = ShellUtils.runShell(Configuration.cmdTestAll, fullBuggyProjectPath);
					exitCode = output.get(0);
					output.remove(0);
					if (!exitCode.equals("exit code: 0")) {
						System.out.println(exitCode);
					}
					results = ShellUtils.shellRun(Arrays.asList("python3 read_test_results.py " + fullBuggyProjectPath), buggyProject, fullBuggyProjectPath);
					tempFailedTestCases = readTestResultsFromScripts(results);
				}
				else {
					results = ShellUtils.shellRun(Arrays.asList("java -cp "
							+ PathUtils.buildTestClassPath(dp.classPath, dp.testClassPath, dp.libPaths)
							+ " org.junit.runner.JUnitCore " + dp.getAllTestCaseClasses()), buggyProject, fullBuggyProjectPath);
					if (results.isEmpty()) continue;
					tempFailedTestCases = readTestResults(results);
				}
				failedTestsAfterFix.addAll(tempFailedTestCases);
				failedTestsAfterFix.removeAll(this.fakeFailedTestCasesList);

				failedTestsAfterFix.removeAll(this.fakeIgnoredTestCasesList);
				List<String> ignoredTests = new ArrayList<>();
				for (String fMethod : failedTestsAfterFix) {
					for (String fFake: fakeIgnoredTestCasesList) {
						if (fMethod.contains(fFake)) {
							ignoredTests.add(fMethod);
						}
					}
				}
				failedTestsAfterFix.removeAll(ignoredTests);
				errorTestAfterFix = failedTestsAfterFix.size();

				if (failedTestsAfterFix.size() > 0) {
					continue;
				} else if (!exitCode.equals("exit code: 0")) {
					System.out.println("Output of Test All: " + output);
					continue;
				}

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				log.debug(buggyProject + " ---Fixer: fix fail because of faile passing previously failed test cases! ");
				continue;
			}

			if (errorTestAfterFix < minErrorTest) {
				List<String> tmpFailedTestsAfterFix = new ArrayList<>();
				tmpFailedTestsAfterFix.addAll(failedTestsAfterFix);
				tmpFailedTestsAfterFix.removeAll(this.failedTestStrList);
				if (tmpFailedTestsAfterFix.size() > 0) { // Generate new bugs.
					log.debug(buggyProject + " ---Generated new bugs: " + tmpFailedTestsAfterFix.size());
					continue;
				}
				
				// Output the generated patch.
				{
					fixedStatus = 1;
					log.info("Succeeded to fix the bug " + buggyProject + "====================");
					String patchStr = TestUtils.readPatch(this.fullBuggyProjectPath);
					System.out.println(patchStr);
					if (patchStr == null || !patchStr.startsWith("diff")) {
						FileHelper.outputToFile(Configuration.outputPath + this.dataType + "/FixedBugs/" + buggyProject + "/Patch_" + patchId + ".txt",
								"//**********************************************************\n//" + scn.suspiciousJavaFile + " ------ " + scn.buggyLine
								+ "\n//**********************************************************\n"
								+ "===Buggy Code===\n" + buggyCode + "\n\n===Patch Code===\n" + patchCode, false);
					} else {
						FileHelper.outputToFile(Configuration.outputPath + this.dataType + "/FixedBugs/" + buggyProject + "/Patch_" + patchId + ".txt", patchStr, false);
					}

					if (!isTestFixPatterns) {
						this.minErrorTest = 0;
						break;
					}
				}
			}
			else {
				log.debug("Failed Tests after fixing: " + errorTestAfterFix + " " + buggyProject);
			}
		}
		
		try {
			scn.targetJavaFile.delete();
			scn.targetClassFile.delete();
			Files.copy(scn.javaBackup.toPath(), scn.targetJavaFile.toPath());
			Files.copy(scn.classBackup.toPath(), scn.targetClassFile.toPath());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private List<String> readTestResultsFromScripts(String results) {
		Set<String> failingTests = new HashSet<>();
		String[] testResults = results.split("\n");
		for (String testResult : testResults) {
			if (testResult.isEmpty()) continue;
			failingTests.add(testResult.trim());
		}
		return new ArrayList<>(failingTests);
	}
	
	private List<String> readTestResults(String results) {
		List<String> failedTeatCases = new ArrayList<>();
		String[] testResults = results.split("\n");
		for (String testResult : testResults) {
			if (testResult.isEmpty()) continue;
			
			if (NumberUtils.isDigits(testResult.substring(0, 1))) {
				int index = testResult.indexOf(") ");
				if (index <= 0) continue;
				int indexOfLeftParenthesis = testResult.indexOf("(");
				if (indexOfLeftParenthesis < 0) {
					testResult = testResult.substring(index + 1).trim();
					failedTeatCases.add(testResult);
				}
				else {
					testResult = testResult.substring(index + 1, testResult.length() - 1).trim();
					indexOfLeftParenthesis = testResult.indexOf("(");
					String testCase = testResult.substring(0, indexOfLeftParenthesis);
					String testClass = testResult.substring(indexOfLeftParenthesis + 1);
					failedTeatCases.add(testClass + "#" + testCase);
				}
			}
		}
		return failedTeatCases;
	}

	private void addPatchCodeToFile(SuspCodeNode scn, Patch patch) {
        String javaCode = FileHelper.readFile(scn.javaBackup);
        
		String fixedCodeStr1 = patch.getFixedCodeStr1();
		String fixedCodeStr2 = patch.getFixedCodeStr2();
		int exactBuggyCodeStartPos = patch.getBuggyCodeStartPos();
		int exactBuggyCodeEndPos = patch.getBuggyCodeEndPos();
		String patchCode = fixedCodeStr1;
		boolean needBuggyCode = false;
		if (exactBuggyCodeEndPos > exactBuggyCodeStartPos) {
			if ("MOVE-BUGGY-STATEMENT".equals(fixedCodeStr2)) {
				// move statement position.
			} else if (exactBuggyCodeStartPos != -1 && exactBuggyCodeStartPos < scn.startPos) {
				// Remove the buggy method declaration.
			} else {
				needBuggyCode = true;
				if (exactBuggyCodeStartPos == 0) {
					// Insert the missing override method, the buggy node is TypeDeclaration.
					int pos = scn.suspCodeAstNode.getPos() + scn.suspCodeAstNode.getLength() - 1;
					for (int i = pos; i >= 0; i --) {
						if (javaCode.charAt(i) == '}') {
							exactBuggyCodeStartPos = i;
							exactBuggyCodeEndPos = i + 1;
							break;
						}
					}
				} else if (exactBuggyCodeStartPos == -1 ) {
					// Insert generated patch code before the buggy code.
					exactBuggyCodeStartPos = scn.startPos;
					exactBuggyCodeEndPos = scn.endPos;
				} else {
					// Insert a block-held statement to surround the buggy code
				}
			}
		} else if (exactBuggyCodeStartPos == -1 && exactBuggyCodeEndPos == -1) {
			// Replace the buggy code with the generated patch code.
			exactBuggyCodeStartPos = scn.startPos;
			exactBuggyCodeEndPos = scn.endPos;
		} else if (exactBuggyCodeStartPos == exactBuggyCodeEndPos) {
			// Remove buggy variable declaration statement.
			exactBuggyCodeStartPos = scn.startPos;
		}
		
		patch.setBuggyCodeStartPos(exactBuggyCodeStartPos);
		patch.setBuggyCodeEndPos(exactBuggyCodeEndPos);
        String buggyCode;
		try {
			buggyCode = javaCode.substring(exactBuggyCodeStartPos, exactBuggyCodeEndPos);
			if (needBuggyCode) {
	        	patchCode += buggyCode;
	        	if (fixedCodeStr2 != null) {
	        		patchCode += fixedCodeStr2;
	        	}
	        }
			
			File newFile = new File(scn.targetJavaFile.getAbsolutePath() + ".temp");
	        String patchedJavaFile = javaCode.substring(0, exactBuggyCodeStartPos) + patchCode + javaCode.substring(exactBuggyCodeEndPos);
	        FileHelper.outputToFile(newFile, patchedJavaFile, false);
	        newFile.renameTo(scn.targetJavaFile);
		} catch (StringIndexOutOfBoundsException e) {
			log.debug(exactBuggyCodeStartPos + " ==> " + exactBuggyCodeEndPos + " : " + javaCode.length());
			e.printStackTrace();
			buggyCode = "===StringIndexOutOfBoundsException===";
		}
        
        patch.setBuggyCodeStr(buggyCode);
        patch.setFixedCodeStr1(patchCode);
	}
	
	public class SuspCodeNode {

		public File javaBackup;
		public File classBackup;
		public File targetJavaFile;
		public File targetClassFile;
		public int startPos;
		public int endPos;
		public ITree suspCodeAstNode;
		public String suspCodeStr;
		public String suspiciousJavaFile;
		public int buggyLine;
		
		public SuspCodeNode(File javaBackup, File classBackup, File targetJavaFile, File targetClassFile, int startPos,
				int endPos, ITree suspCodeAstNode, String suspCodeStr, String suspiciousJavaFile, int buggyLine) {
			this.javaBackup = javaBackup;
			this.classBackup = classBackup;
			this.targetJavaFile = targetJavaFile;
			this.targetClassFile = targetClassFile;
			this.startPos = startPos;
			this.endPos = endPos;
			this.suspCodeAstNode = suspCodeAstNode;
			this.suspCodeStr = suspCodeStr;
			this.suspiciousJavaFile = suspiciousJavaFile;
			this.buggyLine = buggyLine;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (obj instanceof SuspCodeNode) {
				SuspCodeNode suspN = (SuspCodeNode) obj;
				if (startPos != suspN.startPos) return false;
				if (endPos != suspN.endPos) return false;
				if (suspiciousJavaFile.equals(suspN.suspiciousJavaFile)) return true;
			}
			return false;
		}
	}
}
