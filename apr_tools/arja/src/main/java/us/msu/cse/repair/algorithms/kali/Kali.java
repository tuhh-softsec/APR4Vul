package us.msu.cse.repair.algorithms.kali;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.tools.JavaFileObject;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import jmetal.core.Solution;
import jmetal.util.JMException;
import us.msu.cse.repair.core.manipulation.InsertReturnManipulation;
import us.msu.cse.repair.core.manipulation.RedirectBranchManipulation;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.AbstractRepairProblem;
import us.msu.cse.repair.core.testexecutors.ITestExecutor;
import us.msu.cse.repair.core.util.IO;

public class Kali extends AbstractRepairProblem {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private long initTime = 0;

	public Kali(Map<String, Object> parameters) throws Exception {
		super(parameters);
		Collections.sort(modificationPoints, new Comparator<ModificationPoint>() {
			@Override
			public int compare(ModificationPoint o1, ModificationPoint o2) {
				Double d1 = new Double(o1.getSuspValue());
				Double d2 = new Double(o2.getSuspValue());
				return d2.compareTo(d1);
			}
		});
	}

	public boolean execute() throws Exception {
		initTime = System.currentTimeMillis();
		if (redirectBranch())
			return true;
		if (insertReturn())
			return true;
		if (deleteStatement())
			return true;

		return false;
	}

	boolean redirectBranch() throws Exception {
		for (int i = 0; i < modificationPoints.size(); i++) {
			ModificationPoint mp = modificationPoints.get(i);
			for (int k = 0; k < 2; k++) {
				boolean status = redirectBranch(mp, k == 0);
				if (status) {
					saveRedirectBranch(mp, k == 0);
					return true;
				}
			}
		}
		return false;
	}

	boolean redirectBranch(ModificationPoint mp, boolean flag) throws Exception {
		ASTRewrite rewriter = getASTRewriter(mp);
		RedirectBranchManipulation manipulation = new RedirectBranchManipulation(mp, null, rewriter);
		manipulation.setCondition(flag);

		if (!manipulation.manipulate())
			return false;

		return runTests(mp, rewriter, 1, flag);
	}

	boolean insertReturn() throws Exception {
		for (int i = 0; i < modificationPoints.size(); i++) {
			for (int k = 0; k < 2; k++) {
				ModificationPoint mp = modificationPoints.get(i);
				boolean status = insertReturn(mp, k == 0);
				if (status) {
					saveInsertReturn(mp, k == 0);
					return true;
				}
			}

		}
		return false;
	}

	boolean insertReturn(ModificationPoint mp, boolean flag) throws Exception {
		ASTRewrite rewriter = getASTRewriter(mp);
		InsertReturnManipulation manipulation = new InsertReturnManipulation(mp, null, rewriter);
		manipulation.setReturnStatus(flag);

		if (!manipulation.manipulate())
			return false;

		return runTests(mp, rewriter, 2, flag);

	}

	boolean deleteStatement() throws Exception {
		for (int i = 0; i < modificationPoints.size(); i++) {
			ModificationPoint mp = modificationPoints.get(i);
			boolean status = deleteStatement(mp);
			if (status) {
				saveDeleteStatement(mp);
				return true;
			}
		}
		return false;
	}

	boolean deleteStatement(ModificationPoint mp) throws Exception {
		Map<String, ASTRewrite> astRewriters = new HashMap<String, ASTRewrite>();
		if (!manipulateOneModificationPoint(mp, "Delete", null, astRewriters))
			return false;
		Map<String, String> modifiedJavaSources = getModifiedJavaSources(astRewriters);
		Map<String, JavaFileObject> compiledClasses = getCompiledClassesForTestExecution(modifiedJavaSources);
		if (compiledClasses != null) {
			boolean flag = invokeTestExecutor(compiledClasses, mp, 3, false, modifiedJavaSources);
			if (flag && diffFormat) {
				IO.savePatch(modifiedJavaSources, srcJavaDir, patchOutputRoot, 0);
			}
			return flag;
		}
		else
			return false;
	}

	boolean invokeTestExecutor(Map<String, JavaFileObject> compiledClasses, ModificationPoint mp, int type, boolean flag1,
														 Map<String, String> modifiedJavaSources) throws Exception {
		Set<String> samplePosTests = getSamplePositiveTests();
		ITestExecutor testExecutor = getTestExecutor(compiledClasses, samplePosTests, modifiedJavaSources);

		boolean status = testExecutor.runTests();
		if (status && percentage != null && percentage < 1) {
			testExecutor = getTestExecutor(compiledClasses, positiveTests, modifiedJavaSources);
			status = testExecutor.runTests();
			if (!testExecutor.isExceptional())
				System.out.println("Number of failed tests: "
						+ testExecutor.getFailureCountInNegative() + ", " + testExecutor.getFailureCountInPositive());

//				if (testExecutor.getFailureCountInNegative() == 0) {
//					if (type == 0) {
//						saveRedirectBranch(mp, flag1);
//					}
//					else if (type == 1) {
//						saveInsertReturn(mp, flag1);
//					}
//					else if (type == 2) {
//						saveDeleteStatement(mp);
//					}
//				}
		}
		else {
			System.out.println("Number of failed tests: "
					+ testExecutor.getFailureCountInNegative() + ", " + testExecutor.getFailureCountInPositive());

//			if (testExecutor.getFailureCountInNegative() == 0) {
//				if (type == 0) {
//					saveRedirectBranch(mp, flag1);
//				}
//				else if (type == 1) {
//					saveInsertReturn(mp, flag1);
//				}
//				else if (type == 2) {
//					saveDeleteStatement(mp);
//				}
//			}
		}
		return status;
	}

	boolean runTests(ModificationPoint mp, ASTRewrite rewriter, int type, boolean flag1) throws Exception {
		Map<String, ASTRewrite> astRewriters = new HashMap<String, ASTRewrite>();
		astRewriters.put(mp.getSourceFilePath(), rewriter);

		Map<String, String> modifiedJavaSources = getModifiedJavaSources(astRewriters);
		Map<String, JavaFileObject> compiledClasses = getCompiledClassesForTestExecution(modifiedJavaSources);

		if (compiledClasses != null) {
			boolean flag = invokeTestExecutor(compiledClasses, mp, type, flag1, modifiedJavaSources);
			if (flag && diffFormat) {
				IO.savePatch(modifiedJavaSources, srcJavaDir, patchOutputRoot, globalID);
			}
			return flag;
		}
		else
			return false;
	}

	ASTRewrite getASTRewriter(ModificationPoint mp) {
		String sourceFilePath = mp.getSourceFilePath();
		CompilationUnit unit = sourceASTs.get(sourceFilePath);
		ASTRewrite rewriter = ASTRewrite.create(unit.getAST());
		return rewriter;
	}

	@Override
	public void evaluate(Solution solution) throws JMException {
		// TODO Auto-generated method stub

	}

	void saveRedirectBranch(ModificationPoint mp, boolean flag) throws IOException {
		Statement faulty = mp.getStatement();
		String data = "RedirectBranch " + flag + " " + mp.getSourceFilePath() + " " + mp.getLCNode().getLineNumber()
				+ " " + mp.getSuspValue() + "\n";
		data += faulty.toString();
		data += "**************************************************\n";

		long estimatedTime = System.currentTimeMillis() - initTime;
		data += "EstimatedTime: " + estimatedTime + "\n";
		savePatch(data);
	}

	void saveInsertReturn(ModificationPoint mp, boolean flag) throws IOException {
		Statement faulty = mp.getStatement();
		String data = "InsertReturn " + flag + " " + mp.getSourceFilePath() + " " + mp.getLCNode().getLineNumber() + " "
				+ mp.getSuspValue() + "\n";
		;
		data += faulty.toString();
		data += "**************************************************\n";

		long estimatedTime = System.currentTimeMillis() - initTime;
		data += "EstimatedTime: " + estimatedTime + "\n";
		savePatch(data);
	}

	void saveDeleteStatement(ModificationPoint mp) throws IOException {
		Statement faulty = mp.getStatement();
		String data = "Delete " + mp.getSourceFilePath() + " " + mp.getLCNode().getLineNumber() + " "
				+ mp.getSuspValue() + "\n";
		;
		data += faulty.toString();
		data += "**************************************************\n";

		long estimatedTime = System.currentTimeMillis() - initTime;
		data += "EstimatedTime: " + estimatedTime + "\n";
		savePatch(data);
	}

	void savePatch(String data) throws IOException {
		File file = new File(patchOutputRoot, "Patch_" + (globalID++) + ".txt");
		if (file.exists())
			file.delete();

		FileUtils.writeByteArrayToFile(file, data.getBytes());

	}

}
