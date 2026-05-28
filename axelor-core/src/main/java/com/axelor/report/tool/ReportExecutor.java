/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report.tool;

import com.axelor.db.JPA;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReportExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(ReportExecutor.class);

  private final ExecutorService sExecutor;

  private static ReportExecutor sInstance;

  static int defaultCorePoolSize = 1;
  static int defaultMaximumPoolSize = 1;
  static long defaultKeepAliveTime = 1;
  static TimeUnit defaultTimeUnit = TimeUnit.MINUTES;

  private ReportExecutor() {
    sExecutor =
        new ReportThreadPoolExecutor(
            defaultCorePoolSize,
            defaultMaximumPoolSize,
            defaultKeepAliveTime,
            defaultTimeUnit,
            new LinkedBlockingQueue<>());
    ;
  }

  private static synchronized ReportExecutor getInstance() {
    if (sInstance == null) {
      sInstance = new ReportExecutor();
    }
    return sInstance;
  }

  private ExecutorService getExecutor() {
    return sExecutor;
  }

  public static <T> Future<T> submit(Callable<T> task) {
    return getInstance().getExecutor().submit(task);
  }

  public static void shutdown() {
    if (sInstance == null) {
      return;
    }

    ExecutorService es = sInstance.getExecutor();
    es.shutdown();

    try {
      if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
        es.shutdownNow();
      }
    } catch (InterruptedException e) {
      LOG.error("Unable to stop the report executor...");
    }

    LOG.info("Report executor stopped.");
  }

  static class ReportThreadPoolExecutor extends ThreadPoolExecutor {

    public ReportThreadPoolExecutor(
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        @NotNull TimeUnit unit,
        @NotNull BlockingQueue<Runnable> workQueue) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /** Clear JPA cache after execution to avoid any inconsistency */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      JPA.clear();
      super.afterExecute(r, t);
    }
  }
}
