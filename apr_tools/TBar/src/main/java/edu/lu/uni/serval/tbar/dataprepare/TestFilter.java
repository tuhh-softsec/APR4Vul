package edu.lu.uni.serval.tbar.dataprepare;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import junit.framework.TestCase;

public class TestFilter implements ClassFilter {

	private final boolean searchInJars;

	private final TestType[] testTypes;

	public TestFilter() {
		this.searchInJars = true;
		this.testTypes = new TestType[] { TestType.JUNIT38_TEST_CLASSES, TestType.RUN_WITH_CLASSES,
				TestType.TEST_CLASSES };
	}

	public TestFilter(boolean searchInJars) {
		this.searchInJars = searchInJars;
		this.testTypes = new TestType[] { TestType.JUNIT38_TEST_CLASSES, TestType.RUN_WITH_CLASSES,
				TestType.TEST_CLASSES };
	}

	public TestFilter(TestType[] suiteTypes) {
		this.searchInJars = true;
		this.testTypes = suiteTypes;
	}

	public TestFilter(boolean searchInJars, TestType[] suiteTypes) {
		this.searchInJars = searchInJars;
		this.testTypes = suiteTypes;
	}

	public boolean acceptClass(Class<?> clazz) {
		// We directly ignore abstract class
		if (isAbstractClass(clazz)) {
			return false;
		}
		// --
		if (isInSuiteTypes(TestType.TEST_CLASSES)) {
			if (acceptTestClass(clazz)) {
				return true;
			}

		}
		if (isInSuiteTypes(TestType.JUNIT38_TEST_CLASSES)) {
			if (acceptJUnit38Test(clazz)) {
				return true;
			}
		}
		if (isInSuiteTypes(TestType.RUN_WITH_CLASSES)) {
			return acceptRunWithClass(clazz);
		}

		return false;
	}

	private boolean acceptJUnit38Test(Class<?> clazz) {
		if (isAbstractClass(clazz)) {
			return false;
		}
		return TestCase.class.isAssignableFrom(clazz);
	}

	private boolean acceptRunWithClass(Class<?> clazz) {
		return clazz.isAnnotationPresent(RunWith.class);
	}

	private boolean isInSuiteTypes(TestType testType) {
		return Arrays.asList(testTypes).contains(testType);
	}

	private boolean acceptTestClass(Class<?> clazz) {
		if (isAbstractClass(clazz)) {
			return false;
		}
		try {
			for (Method method : clazz.getMethods()) {
				if (method.getAnnotation(Test.class) != null) {
					return true;
				}
			}
		} catch (NoClassDefFoundError ignore) {
		}
		return false;
	}

	private boolean isAbstractClass(Class<?> clazz) {
		return (clazz.getModifiers() & Modifier.ABSTRACT) != 0;
	}

	public boolean acceptInnerClass() {
		return true;
	}

	public boolean searchInJars() {
		return searchInJars;
	}

	public boolean acceptClassName(String className) {
		return true;
	}

}
