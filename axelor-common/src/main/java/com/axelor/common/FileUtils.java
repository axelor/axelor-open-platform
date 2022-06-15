/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;

/** This class provides some helper methods to deal with files. */
public final class FileUtils {

  /**
   * Get a file from the given path elements.
   *
   * @param first the first path element
   * @param more the additional path elements
   * @return the file
   */
  public static File getFile(String first, String... more) {
    Preconditions.checkNotNull(first, "first element must not be null");
    File file = new File(first);
    if (more != null) {
      for (String name : more) {
        file = new File(file, name);
      }
    }
    return file;
  }

  /**
   * Get a file from the given path elements.
   *
   * @param directory the parent directory
   * @param next next path element
   * @param more additional path elements
   * @return the file
   */
  public static File getFile(File directory, String next, String... more) {
    Preconditions.checkNotNull(directory, "directory must not be null");
    Preconditions.checkNotNull(next, "next element must not be null");
    File file = new File(directory, next);
    if (more != null) {
      for (String name : more) {
        file = new File(file, name);
      }
    }
    return file;
  }

  /**
   * Copy the source directory to the target directory.
   *
   * @param source the source directory
   * @param target the target directory
   * @throws IOException if IO error occurs during copying
   */
  public static void copyDirectory(File source, File target) throws IOException {
    copyDirectory(source.toPath(), target.toPath());
  }

  /**
   * Copy the source directory to the target directory.
   *
   * @param source the source directory
   * @param target the target directory
   * @throws IOException if IO error occurs during copying
   */
  public static void copyDirectory(Path source, Path target) throws IOException {
    if (!Files.isDirectory(source)) {
      throw new IOException("Invalid source directory: " + source);
    }
    if (Files.exists(target) && !Files.isDirectory(target)) {
      throw new IOException("Invalid target directory: " + target);
    }
    if (!Files.exists(target)) {
      Files.createDirectories(target);
    }
    final DirCopier copier = new DirCopier(source, target);
    final EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);
    Files.walkFileTree(source, opts, Integer.MAX_VALUE, copier);
  }

  /**
   * Delete the given directory recursively.
   *
   * @param directory the directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void deleteDirectory(File directory) throws IOException {
    deleteDirectory(directory.toPath());
  }

  /**
   * Delete the given directory recursively.
   *
   * @param directory the directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void deleteDirectory(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new IOException("Invalid directory: " + directory);
    }
    final DirCleaner cleaner = new DirCleaner();
    final EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);
    Files.walkFileTree(directory, opts, Integer.MAX_VALUE, cleaner);
  }

  static class DirCopier extends SimpleFileVisitor<Path> {

    private final Path source;
    private final Path target;

    DirCopier(Path source, Path target) {
      this.source = source;
      this.target = target;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      final Path dest = target.resolve(source.relativize(file));
      Files.copy(file, dest, COPY_ATTRIBUTES, REPLACE_EXISTING);
      return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      Path dest = target.resolve(source.relativize(dir));
      try {
        Files.copy(dir, dest, COPY_ATTRIBUTES);
      } catch (FileAlreadyExistsException e) {
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (exc == null) {
        Path dest = target.resolve(source.relativize(dir));
        try {
          FileTime time = Files.getLastModifiedTime(dir);
          Files.setLastModifiedTime(dest, time);
        } catch (IOException e) {
        }
      }
      return CONTINUE;
    }
  }

  static class DirCleaner extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      Files.delete(dir);
      return CONTINUE;
    }
  }

  private static final char ILLEGAL_FILENAME_CHARS_REPLACE = '-';
  private static final Character[] INVALID_FILENAME_CHARS = {
    '?', '[', ']', '/', '\\', '=', '<', '>', ':', ';', ',', '\'',
    '"', '&', '$', '#', '*', '(', ')', '|', '~', '`', '!', '{',
    '}', '%', '+', '’', '«', '»', '”', '“', 0x7F, '\n', '\r', '\t'
  };

  /**
   * Sanitizes a filename, replacing with dash
   *
   * <ul>
   *   <li>Removes special characters that are illegal in filenames on certain operating systems
   *   <li>Replaces spaces and consecutive underscore with a single dash
   *   <li>Trims dot, dash and underscore from beginning and end of filename
   *       <ul>
   *
   * @param originalFileName The filename to be sanitized
   * @return string The sanitized filename
   */
  public static String safeFileName(String originalFileName) {
    if (StringUtils.isEmpty(originalFileName)) {
      return originalFileName;
    }
    // trim
    String fileName = originalFileName.trim();
    // unaccent
    fileName = StringUtils.stripAccent(fileName);
    // replace illegal and space
    char[] chars = fileName.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (c == '\u0020') {
        chars[i] = ILLEGAL_FILENAME_CHARS_REPLACE;
        continue;
      }
      for (char illegal : INVALID_FILENAME_CHARS) {
        if (c == illegal) {
          chars[i] = ILLEGAL_FILENAME_CHARS_REPLACE;
          break;
        }
      }
    }
    fileName = new String(chars);
    // consecutive underscore
    fileName =
        fileName.replaceAll(
            "(" + ILLEGAL_FILENAME_CHARS_REPLACE + ")+",
            String.valueOf(ILLEGAL_FILENAME_CHARS_REPLACE));
    // start or end with dot, dash or underscore
    fileName = fileName.replaceAll("(?:^[-_.]+)|(?:[-_.]+$)", "");
    return fileName;
  }
}
