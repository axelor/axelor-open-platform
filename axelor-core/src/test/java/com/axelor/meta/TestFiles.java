/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.dms.db.DMSFile;
import com.axelor.file.temp.TempFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class TestFiles extends MetaTest {

  @Inject private MetaFiles files;

  @Inject private ContactRepository contacts;

  @Test
  public void testUpload() throws IOException {

    Path tmp1 = Files.createTempFile("test", null);
    Path tmp2 = Files.createTempFile("test2", null);

    Files.write(tmp1, "Hello...".getBytes());
    Files.write(tmp2, "World...".getBytes());

    MetaFile metaFile = files.upload(tmp1.toFile());
    assertNotNull(metaFile);
    assertNotNull(metaFile.getId());
    assertEquals("text/plain", metaFile.getFileType());

    // upload again
    MetaFile metaFile2 = files.upload(tmp1.toFile());

    // make sure upload path are not same
    assertNotEquals(metaFile.getFilePath(), metaFile2.getFilePath());

    // test update existing file
    String text1 = new String(Files.readAllBytes(MetaFiles.getPath(metaFile2)));
    String path1 = metaFile2.getFilePath();

    metaFile2 = files.upload(tmp2.toFile(), metaFile2);

    String text2 = new String(Files.readAllBytes(MetaFiles.getPath(metaFile2)));
    String path2 = metaFile2.getFilePath();

    assertEquals("Hello...", text1);
    assertEquals("World...", text2);
    assertEquals(path1, path2);

    Files.deleteIfExists(tmp1);
    Files.deleteIfExists(tmp2);
    Files.deleteIfExists(MetaFiles.getPath(metaFile));
    Files.deleteIfExists(MetaFiles.getPath(metaFile2));
  }

  @Test
  @Transactional
  public void testAttach() throws IOException {

    Contact contact = new Contact();
    contact.setFirstName("Test");
    contact.setLastName("DMS");
    contact = contacts.save(contact);

    Path tmp1 = TempFiles.createTempFile();
    Path tmp2 = TempFiles.createTempFile();

    // test tmp file helpers
    assertNotNull(tmp1);
    assertNotNull(tmp2);
    assertNotEquals(tmp1, tmp2);
    assertEquals(tmp1, TempFiles.findTempFile(tmp1.getFileName().toString()));
    assertEquals(tmp2, TempFiles.findTempFile(tmp2.getFileName().toString()));

    Files.write(tmp1, "Hello...".getBytes());
    Files.write(tmp2, "World...".getBytes());

    // attach 1st file, it should create a parent dms directory
    DMSFile dms1 = files.attach(new FileInputStream(tmp1.toFile()), "dms-test1", contact);

    assertNotNull(dms1);
    assertNotNull(dms1.getParent());

    // attach 2nd file, it should re-user existing parent dms directory
    DMSFile dms2 = files.attach(new FileInputStream(tmp2.toFile()), "dms-test2", contact);

    assertNotNull(dms2);
    assertNotNull(dms2.getParent());
    assertEquals(dms1.getParent(), dms2.getParent());

    // attach 2nd file again, it should create new file with name "dms-test2-1"
    DMSFile dms3 = files.attach(new FileInputStream(tmp2.toFile()), "dms-test2", contact);

    assertNotNull(dms3);
    assertEquals("dms-test2-1", dms3.getMetaFile().getFilePath());

    // clean up uploaded files
    files.delete(dms1);
    files.delete(dms2);
    files.delete(dms3);
  }
}
