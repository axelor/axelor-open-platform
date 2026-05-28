/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report.tool;

import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides some helper methods to deal with Image in Reports */
public class ImageTool {

  private static final Logger LOG = LoggerFactory.getLogger(ImageTool.class);

  /**
   * Get the bytes of the given MetaFile identified by the id
   *
   * @param metaFileId id of the MetaFile
   * @return the bytes read from the file
   */
  public static byte[] getImageBytes(Long metaFileId) {
    return getImageBytes(metaFileId, false);
  }

  /**
   * Get the bytes of the given MetaFile identified by the id
   *
   * @param metaFileId id of the MetaFile
   * @param cache whether to cache the file
   * @return the bytes read from the file
   */
  public static byte[] getImageBytes(Long metaFileId, boolean cache) {
    try {
      MetaFile metaFile = getMetaFile(metaFileId);
      if (metaFile == null) {
        return null;
      }
      return getBytes(metaFile.getFilePath(), cache);
    } catch (Exception e) {
      LOG.error("Unable to retrieve the MetaFile with id {}", metaFileId, e);
      return null;
    }
  }

  private static MetaFile getMetaFile(Long metaFileId)
      throws ExecutionException, InterruptedException {
    if (metaFileId == null || metaFileId <= 0) {
      return null;
    }
    Callable<MetaFile> callableTask = () -> Beans.get(MetaFileRepository.class).find(metaFileId);
    return ReportExecutor.submit(callableTask).get();
  }

  private static byte[] getBytes(String filePath, boolean cache) throws IOException {
    Store store = FileStoreFactory.getStore();
    InputStream stream = store.getStream(filePath, cache);
    return IOUtils.toByteArray(stream);
  }
}
