/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.meta.db.MetaFile;

public class TestFiles extends MetaTest {

	@Inject
	private MetaFiles files;

	@Test
	public void testUpload() throws IOException {

		Path tmp = Files.createTempFile("test", null);
		Path tmp2 = Files.createTempFile("test2", null);

		Files.write(tmp, "Hello...".getBytes());
		Files.write(tmp2, "World...".getBytes());

		MetaFile metaFile = files.upload(tmp.toFile());
		Assert.assertNotNull(metaFile);
		Assert.assertNotNull(metaFile.getId());
		Assert.assertEquals("text/plain", metaFile.getFileType());

		// upload again
		MetaFile metaFile2 = files.upload(tmp.toFile());

		// make sure upload path are not same
		Assert.assertNotEquals(metaFile.getFilePath(), metaFile2.getFilePath());

		// test update existing file
		String text1 = new String(Files.readAllBytes(MetaFiles.getPath(metaFile2)));
		String path1 = metaFile2.getFilePath();

		metaFile2 = files.upload(tmp2.toFile(), metaFile2);

		String text2 = new String(Files.readAllBytes(MetaFiles.getPath(metaFile2)));
		String path2 = metaFile2.getFilePath();

		Assert.assertEquals("Hello...", text1);
		Assert.assertEquals("World...", text2);
		Assert.assertEquals(path1, path2);

		Files.deleteIfExists(tmp);
		Files.deleteIfExists(tmp2);
		Files.deleteIfExists(MetaFiles.getPath(metaFile));
		Files.deleteIfExists(MetaFiles.getPath(metaFile2));
	}
}
