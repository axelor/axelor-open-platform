package com.axelor.common;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class TestClassUtils {

	@Test
	public void testFindClass() {
		Class<?> cls = ClassUtils.findClass("com.axelor.common.StringUtils");
		Assert.assertEquals(cls, StringUtils.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindClassFail() {
		ClassUtils.findClass("com.axelor.common.StringUtil");
	}

	@Test
	public void testClassFinder() {
		Assert.assertNotNull(ClassUtils.finderOf(Map.class).within("com.google").find());
	}
}
