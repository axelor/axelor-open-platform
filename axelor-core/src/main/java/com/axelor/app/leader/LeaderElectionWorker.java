/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.leader;

import com.axelor.db.internal.DBHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code LeaderElectionWorker} class is responsible for managing a leader election mechanism
 * among multiple instances of a distributed system. This class ensures that only one node acts as
 * the leader at a time using PostgreSQL Session-Level Advisory Lock.
 *
 * <p>The class uses a scheduled task to periodically attempt to acquire leadership or verify the
 * continued validity of the leader's lock. It operates on a single background thread to manage the
 * election process.
 */
public class LeaderElectionWorker {

  private static final Logger log = LoggerFactory.getLogger(LeaderElectionWorker.class);

  private static volatile LeaderElectionWorker instance;

  /** Unique 64-bit ID for the advisory lock. */
  private static final long ADVISORY_LOCK_ID = 6432543124L;

  /** The interval between election attempts in seconds. */
  private static final int ELECTION_INTERVAL_SECONDS = 5;

  /** The scheduler shutdown timeout in seconds. */
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 20;

  /** Indicates if the current node holds the leader lock. */
  private volatile boolean isLeader = false;

  /** The scheduler for the election loop. */
  private volatile ScheduledExecutorService scheduler;

  /** The dedicated database connection used solely for holding the advisory lock. */
  private volatile Connection lockConnection;

  private LeaderElectionWorker() {
    // Prevent instantiation
  }

  /**
   * Retrieves the singleton instance of the LeaderElectionWorker.
   *
   * @return the single instance of the LeaderElectionWorker.
   */
  public static LeaderElectionWorker getInstance() {
    if (instance == null) {
      synchronized (LeaderElectionWorker.class) {
        if (instance == null) {
          instance = new LeaderElectionWorker();
        }
      }
    }
    return instance;
  }

  /**
   * Attempts to assume leadership for the current node by acquiring a lock.
   *
   * <ol>
   *   <li>If already leader, verify connection is still valid.
   *   <li>If connection invalid, resign leadership.
   *   <li>If not leader (or lost connection), acquire a new dedicated connection.
   *   <li>Attempt {@code pg_try_advisory_lock}.
   *   <li>If successful, flag as leader.
   *   <li>If failed, close the connection immediately.
   * </ol>
   */
  private void attemptToBecomeLeader() {
    try {
      // If already the leader, just check if we still hold the lock.
      if (isLeader) {
        if (checkConnection(lockConnection)) {
          return;
        }
        // Connection lost, release leadership
        log.trace("Leader lost connection. Releasing leadership.");
        releaseLock();
      }

      // Try to acquire a new connection if we don't have one
      if (!checkConnection(lockConnection)) {
        closeConnection(); // Close previous (bad) connection if any
        lockConnection = DBHelper.getConnection();
        lockConnection.setAutoCommit(true);
      }

      // Attempt to acquire the lock
      if (acquireLock(lockConnection)) {
        isLeader = true;
        log.trace("This node is now the leader.");
      } else {
        isLeader = false;
        log.trace("This node is a follower. Will retry.");
        // If we failed to get the lock, we don't need to hold the connection.
        closeConnection();
      }
    } catch (Exception e) {
      if (scheduler != null && !scheduler.isShutdown()) {
        log.error("Error during leader election.", e);
      }
      releaseLock();
    }
  }

  /**
   * Executes the PostgreSQL advisory lock query.
   *
   * @param conn The dedicated connection
   * @return {@code true} if the lock was acquired, {@code false} otherwise.
   * @throws Exception if a database access error occurs.
   */
  private boolean acquireLock(Connection conn) throws Exception {
    try (PreparedStatement ps = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
      ps.setLong(1, ADVISORY_LOCK_ID);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  /**
   * Checks if a JDBC connection is open and valid.
   *
   * @param conn The connection to check.
   * @return {@code true} if valid, {@code false} if null, closed, or timed out.
   */
  private boolean checkConnection(Connection conn) {
    try {
      return conn != null && !conn.isClosed() && conn.isValid(2);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Stops the leader election service gracefully.
   *
   * <p>This shuts down the scheduler, interrupts any running thread, and closes the database
   * connection (releasing the lock).
   */
  public synchronized void stop() {
    if (!isRunning()) {
      return;
    }

    log.info("Stopping Leader Election Worker...");
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }

    releaseLock();
    scheduler = null;
    log.info("Leader Election Worker stopped.");
  }

  /** Resets internal leadership state and closes the connection. */
  private void releaseLock() {
    isLeader = false;
    closeConnection();
  }

  /** Safely closes the database connection to release the advisory lock. */
  private void closeConnection() {
    if (lockConnection != null) {
      try {
        lockConnection.close();
      } catch (Exception e) {
        log.error("Error closing lock connection.", e);
      } finally {
        lockConnection = null;
      }
    }
  }

  /**
   * Starts the background leader election service.
   *
   * <p>This initializes a single-thread scheduler with a Daemon thread. The election process runs
   * immediately (0 delay) and repeats every {@value #ELECTION_INTERVAL_SECONDS} seconds.
   */
  public synchronized void start() {
    if (isRunning()) {
      log.trace("Leader Election Worker is already running.");
      return;
    }
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "Leader-Election-Thread");
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleWithFixedDelay(
        this::attemptToBecomeLeader, 0, ELECTION_INTERVAL_SECONDS, TimeUnit.SECONDS);

    log.info("Leader Election Worker started.");
  }

  /**
   * Indicates whether the current instance is the leader.
   *
   * @return {@code true} if the current instance holds leadership, {@code false} otherwise.
   */
  public boolean isLeader() {
    return isLeader;
  }

  /**
   * Checks if the election service is currently active.
   *
   * @return {@code true} if the scheduler is running.
   */
  protected boolean isRunning() {
    return scheduler != null && !scheduler.isShutdown();
  }
}
