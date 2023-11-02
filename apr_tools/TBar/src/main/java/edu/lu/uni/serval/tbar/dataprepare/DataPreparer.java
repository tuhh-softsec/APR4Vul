package edu.lu.uni.serval.tbar.dataprepare;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import edu.lu.uni.serval.tbar.utils.JavaLibrary;

/**
 * Prepare data for fault localization, program compiling and testing.
 * 
 * @author kui.liu
 *
 */
public class DataPreparer {

    public String buggyProjectPath;

    public String classPath;
    public String srcPath;
    public String testClassPath;
    public String testSrcPath;
    public List<String> libPaths = new ArrayList<>();
    public boolean validPaths = true;
    public String[] testCases;
    public URL[] classPaths;
    
    public DataPreparer(String path){
        if (!path.endsWith("/")){
            path += "/";
        }
        buggyProjectPath = path;
    }
    
    public void prepareData(){
//		libPath.add(FromString.class.getProtectionDomain().getCodeSource().getLocation().getFile());
//		libPath.add(EasyMock.class.getProtectionDomain().getCodeSource().getLocation().getFile());
//		libPath.add(IOUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		
		loadPaths();
		
		if (!checkProjectDirectories()){
			validPaths = false;
			return;
		}
		
		loadTestCases();
		
		loadClassPaths();
    }

	private void loadPaths() {
		String projectDir = buggyProjectPath;
		classPath = projectDir + Configuration.classPath;
		testClassPath = projectDir + Configuration.testClassPath;
		srcPath = projectDir + Configuration.srcPath;
		testSrcPath = projectDir + Configuration.testSrcPath;

		List<File> libPackages = new ArrayList<>();
		if (new File(projectDir + "/lib/").exists()) {
			libPackages.addAll(FileHelper.getAllFiles(projectDir + "/lib/", ".jar"));
		}
		if (new File(projectDir + "/build/lib/").exists()) {
			libPackages.addAll(FileHelper.getAllFiles(projectDir + "/build/lib/", ".jar"));
		}
		if (new File(projectDir + "/build/libs/").exists()) {
			libPackages.addAll(FileHelper.getAllFiles(projectDir + "/build/libs/", ".jar"));
		}
		if (new File(projectDir + "/build/libs/lib").exists()) {
			libPackages.addAll(FileHelper.getAllFiles(projectDir + "/build/libs/lib", ".jar"));
		}
		if (new File(projectDir + "/build/dependency-libs").exists()) {
			libPackages.addAll(FileHelper.getAllFiles(projectDir + "/build/dependency-libs", ".jar"));
		}
		if (new File(projectDir + "/target/dependency/").exists()) {
			libPackages.addAll(FileHelper.getAllFiles(projectDir + "/target/dependency/", ".jar"));
		}

		for (String extraLibPath : Configuration.extraClasspath.split(File.pathSeparator)) {
			File f = new File(extraLibPath);
			if (f.exists()) {
				if (f.isDirectory()) {
					List<File> newLibs = FileHelper.getAllFiles(extraLibPath, ".jar");
					libPackages.addAll(newLibs);
				}
				libPaths.add(extraLibPath);
			}
		}

		for (File libPackage : libPackages) {
			libPaths.add(libPackage.getAbsolutePath());
		}

//		System.out.println("Dependency List: " + libPaths);
	}
	
	private boolean checkProjectDirectories() {
		if (!new File(classPath).exists()) {
			System.err.println("Class path: " + classPath + " does not exist!");
			return false;
		}
		if (!new File(srcPath).exists()) {
			System.err.println("Source code path: " + srcPath + " does not exist!");
			return false;
		}
		if (!new File(testClassPath).exists()) {
			System.err.println("Test class path: " + testClassPath + " does not exist!");
			return false;
		}
		if (!new File(testSrcPath).exists()) {
			System.err.println("Test source path: " + testSrcPath + " does not exist!");
			return false;
		}
		return true;
	}

	public String getAllTestCaseClasses() {
    	StringBuilder result = new StringBuilder();
    	for (String tc : testCases) {
    		result.append(tc);
    		result.append(" ");
		}
    	return result.toString();
	}

	private void loadTestCases() {
		testCases = new TestClassesFinder().findIn(JavaLibrary.classPathFrom(testClassPath + ":" + classPath), false);
//		List<File> testCasesFiles = FileHelper.getAllFiles(testClassPath, ".class");
////		testCasesFiles.addAll(FileHelper.getAllFiles(testClassPath, "Tests.class"));
//		StringBuilder b = new StringBuilder();
//		List<String> testCaseNames = new ArrayList<>();
//		int i = testCasesFiles.get(0).getPath().indexOf(testClassPath) + testClassPath.length();
//		for (File file : testCasesFiles) {
//			String fileName = file.getName();
//			if (fileName.contains("Test") && !fileName.contains("$")) {
////			if (fileName.startsWith("Test") || fileName.endsWith("Test.class")) {//Time
//				String filePath = file.getPath();
//				filePath = filePath.substring(i, filePath.lastIndexOf(".")).replace("/", ".");
//				testCaseNames.add(filePath);
//				b.append(filePath).append("\n");
//			}
//		}
//		FileHelper.outputToFile("log/testcases_2.txt", b, false);
//		testCases = testCaseNames.toArray(new String[testCaseNames.size()]);
		Arrays.sort(testCases);
	}

	private void loadClassPaths() {
		classPaths = JavaLibrary.classPathFrom(testClassPath);
		classPaths = JavaLibrary.extendClassPathWith(classPath, classPaths);
		if (libPaths != null) {
			for (String lpath : libPaths) {
				classPaths = JavaLibrary.extendClassPathWith(lpath, classPaths);
			}
		}
	}
    
}
