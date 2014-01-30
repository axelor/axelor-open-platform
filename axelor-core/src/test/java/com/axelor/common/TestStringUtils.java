package com.axelor.common;

import org.junit.Assert;
import org.junit.Test;

public class TestStringUtils {

	@Test
	public void testIsEmpty() {
		Assert.assertTrue(StringUtils.isEmpty(null));
		Assert.assertTrue(StringUtils.isEmpty(""));
		Assert.assertFalse(StringUtils.isEmpty(" "));
	}
	
	@Test
	public void testIsBlank() {
		Assert.assertTrue(StringUtils.isBlank(null));
		Assert.assertTrue(StringUtils.isBlank(""));
		Assert.assertTrue(StringUtils.isBlank(" "));
		Assert.assertFalse(StringUtils.isBlank("some value"));
	}
}
