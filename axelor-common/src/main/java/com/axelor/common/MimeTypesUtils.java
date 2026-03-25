/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines static helper methods to deal with MIME types.
 *
 * <p><strong>Security note:</strong> methods in this class fall into two categories:
 *
 * <ul>
 *   <li><strong>Extension-based detection</strong> ({@link #getContentType(String)}, {@link
 *       #getExtensionContentType(String)}): rely solely on the file name or extension. These must
 *       <em>not</em> be used for security validation, as a malicious user can rename any file to an
 *       allowed extension.
 *   <li><strong>Content-based detection</strong> ({@link #getContentType(File, String)}, {@link
 *       #getContentType(InputStream, String)}): inspect the file's magic bytes via Apache Tika.
 *       When a {@code fileName} is provided, it is used as a hint that may influence the result
 *       when content detection is ambiguous. To guarantee purely content-based detection (e.g. for
 *       security validation), pass {@code null} as {@code fileName} so that Tika relies exclusively
 *       on magic bytes.
 * </ul>
 */
public class MimeTypesUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MimeTypesUtils.class);

  private static final String defaultType = "application/octet-stream";
  private static final Detector _detector = new DefaultDetector();

  public MimeTypesUtils() {}

  /**
   * Returns the content type from the file, using both its content (magic bytes) and its name as a
   * hint.
   *
   * <p><strong>Security note:</strong> the file name is passed as a hint to Tika and may influence
   * the result when content detection is ambiguous. For security validation, use {@link
   * #getContentType(File, String)} with {@code null} as the file name to rely solely on magic
   * bytes.
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
   * Returns the content type from the path, using both its content (magic bytes) and its name as a
   * hint.
   *
   * <p><strong>Security note:</strong> the file name is passed as a hint to Tika and may influence
   * the result when content detection is ambiguous. For security validation, use {@link
   * #getContentType(File, String)} with {@code null} as the file name to rely solely on magic
   * bytes.
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
   * Detects the content type of the given file name. The type detection is based solely on known
   * file name extensions, without inspecting file content.
   *
   * <p><strong>Security note:</strong> this method must not be used for security validation, as a
   * malicious user can rename any file to an allowed extension.
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
   * Returns the content type from the file extension, without inspecting file content.
   *
   * <p><strong>Security note:</strong> this method must not be used for security validation, as a
   * malicious user can rename any file to an allowed extension.
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
   * Returns the content type from the file content (magic bytes), optionally using the file name as
   * a hint.
   *
   * <p><strong>Security note:</strong> if {@code fileName} is non-null and non-blank, it is passed
   * to Tika as a hint and may influence the result when content detection is ambiguous. To
   * guarantee purely content-based detection (e.g. for security validation), pass {@code null} as
   * {@code fileName}.
   *
   * @param file the file, can be <code>null</code>
   * @param fileName the file name hint (e.g., "doc"), or {@code null} for content-only detection
   * @return detected content type if it is a supported format or "application/octet-stream" if it
   *     is an unsupported format
   */
  public static String getContentType(File file, String fileName) {
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
      return getContentType(inputStream, fileName);
    } catch (Exception ex) {
      LOG.warn("Unable to detect content-type of file : {}", ex.toString());
    }

    return getContentType(fileName);
  }

  /**
   * Detects the content type based on the input stream and an optional file name hint.
   *
   * <p>This method is designed to be safe for all stream types. It guarantees that the provided
   * {@code InputStream} is never consumed or corrupted.
   *
   * <p>If the stream supports {@link InputStream#markSupported() mark/reset} (e.g., {@link
   * java.io.BufferedInputStream}), this method will peek at the file header to determine the MIME
   * type accurately, and then <b>reset</b> the stream to its original position.
   *
   * <p>If the stream is {@code null} or does <b>not</b> support mark/reset (e.g., a raw {@code
   * FileInputStream}), content inspection is skipped to avoid consuming data. In this case,
   * detection relies solely on the provided {@code fileName} extension.
   *
   * <p><strong>Security note:</strong> if {@code fileName} is non-null and non-blank, it is passed
   * to Tika as a hint and may influence the result when content detection is ambiguous. To
   * guarantee purely content-based detection (e.g. for security validation), pass {@code null} as
   * {@code fileName} and ensure the stream supports mark/reset (e.g. wrap it in a {@link
   * java.io.BufferedInputStream} or use a {@link java.io.ByteArrayInputStream}).
   *
   * @param inputStream the input stream to inspect (can be {@code null} or raw)
   * @param fileName the file name hint (e.g., "doc"), or {@code null} for content-only detection
   * @return the detected content type, or "application/octet-stream" if detection fails
   */
  public static String getContentType(InputStream inputStream, String fileName) {

    Metadata metadata = new Metadata();

    if (!StringUtils.isBlank(fileName)) {
      metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
    }

    TikaInputStream tis = null;
    if ((inputStream != null) && inputStream.markSupported()) {
      tis = TikaInputStream.get(inputStream);
    }

    try {
      return _detector.detect(tis, metadata).toString();
    } catch (IOException ex) {
      LOG.warn("Unable to detect content-type of file : {}", ex.toString());
    } finally {
      if (tis != null) {
        try {
          tis.reset();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    return defaultType;
  }
}
