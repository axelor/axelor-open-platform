package com.axelor.common;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class TestFileUtils {

	@Test
	public void testGetFile() {
		
		File file = FileUtils.getFile("file.text");
		Assert.assertEquals("file.text", file.getPath());
		
		file = FileUtils.getFile("my", "dir", "file.text");
		Assert.assertEquals("my/dir/file.text".replace("/", File.separator), file.getPath());
	}
}
