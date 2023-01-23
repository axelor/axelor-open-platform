/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class defines from static helper methods to deal with Mime Types. */
public class MimeTypesUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MimeTypesUtils.class);

  private static final String defaultType = "application/octet-stream";
  private static final Detector _detector = new DefaultDetector();

  public MimeTypesUtils() {}

  /**
   * Returns the content type from the file.
   *
   * @param file the file
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getContentType(File file) {
    if (file == null) {
      return defaultType;
    }
    return getContentType(file, file.getName());
  }

  /**
   * Returns the content type from the path.
   *
   * @param path the path
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getContentType(Path path) {
    if (path == null) {
      return defaultType;
    }
    return getContentType(path.toFile(), path.getFileName().toString());
  }

  /**
   * Detects the content type of the given file name. The type detection is based on known file name
   * extensions.
   *
   * @param fileName the file name
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getContentType(String fileName) {
    if (StringUtils.isBlank(fileName)) {
      return defaultType;
    }
    return getContentType((InputStream) null, fileName);
  }

  /**
   * Returns the content type from the file extension.
   *
   * @param extension the extension of the file (e.g., "doc")
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getExtensionContentType(String extension) {
    if (StringUtils.isBlank(extension)) {
      return defaultType;
    }

    return getContentType("Z.".concat(extension));
  }

  /**
   * Returns the content type from the file and file name.
   *
   * @param file the file, can be <code>null</code>
   * @param fileName the file name or extension of the file (e.g., "doc")
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getContentType(File file, String fileName) {
    try (InputStream inputStream = new FileInputStream(file)) {
      return getContentType(inputStream, fileName);
    } catch (Exception ex) {
      LOG.warn("Unable to detect content-type of file : {}", ex.toString());
    }

    return getContentType(fileName);
  }

  /**
   * Returns the content type from the file and file name.
   *
   * @param inputStream the input stream, can be <code>null</code>
   * @param fileName the file name or extension of the file (e.g., "doc")
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getContentType(InputStream inputStream, String fileName) {

    Metadata metadata = new Metadata();

    if (!StringUtils.isBlank(fileName)) {
      metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
    }

    if ((inputStream != null) && !inputStream.markSupported()) {
      inputStream = new BufferedInputStream(inputStream);
    }

    try {
      return _detector.detect(inputStream, metadata).toString();
    } catch (IOException ex) {
      LOG.warn("Unable to detect content-type of file : {}", ex.toString());
    }

    return defaultType;
  }
}
