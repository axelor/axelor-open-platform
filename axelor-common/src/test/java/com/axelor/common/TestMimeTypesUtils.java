package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class TestMimeTypesUtils {

  @Test
  public void testDefaultContentType() {
    String defaultContentType = "application/octet-stream";
    assertEquals(defaultContentType, MimeTypesUtils.getExtensionContentType(null));
    assertEquals(defaultContentType, MimeTypesUtils.getExtensionContentType(""));

    assertEquals(defaultContentType, MimeTypesUtils.getContentType((String) null));
    assertEquals(defaultContentType, MimeTypesUtils.getContentType(""));

    assertEquals(defaultContentType, MimeTypesUtils.getContentType((File) null));
    assertEquals(defaultContentType, MimeTypesUtils.getContentType(new File("dontexist")));

    assertEquals(defaultContentType, MimeTypesUtils.getContentType((Path) null));
    assertEquals(defaultContentType, MimeTypesUtils.getContentType(Paths.get("dontexist")));
  }

  @Test
  public void testDetectContentType() {
    _check("test.jpg", "jpg", null, "image/jpeg");
    _check("test.zip", "zip", null, "application/zip");
    _check("test.txt", "txt", "test.txt", "text/plain");
    _check("text.csv", "csv", "grades.csv", "text/csv");
    _check("text.html", "html", "test.html", "text/html");
    _check("test.doc", "doc", null, "application/msword");
    _check("text.pdf", "pdf", "test.pdf", "application/pdf");
    _check(
        "test.docx",
        "docx",
        null,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    _check(
        "test.pptx",
        "pptx",
        null,
        "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    _check("test.another", "fake", null, "application/octet-stream");
  }

  private void _check(String fileName, String extension, String file, String expected) {
    assertEquals(expected, MimeTypesUtils.getContentType(fileName));
    assertEquals(expected, MimeTypesUtils.getExtensionContentType(extension));
    if (StringUtils.notBlank(file)) {
      assertEquals(
          expected,
          MimeTypesUtils.getContentType(new File(ResourceUtils.getResource(file).getFile())));
    }
  }
}
