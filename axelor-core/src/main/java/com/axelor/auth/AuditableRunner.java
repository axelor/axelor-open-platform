/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.concurrent.Callable;

/** This class can be used to run batch jobs that requires to keep track of audit logs. */
public class AuditableRunner {

  static final ThreadLocal<User> batchUser = new ThreadLocal<>();

  private static final String DEFAULT_BATCH_USER = "admin";

  private UserRepository users;

  @Inject
  public AuditableRunner(UserRepository users) {
    this.users = users;
  }

  /**
   * Get the batch user.
   *
   * @return current user
   */
  public static User batchUser() {
    return batchUser.get();
  }

  /**
   * Run a batch job.
   *
   * @param job the job to run
   */
  public void run(final Runnable job) {
    try {
      run(
          () -> {
            job.run();
            return true;
          });
    } catch (Exception e) {
      // propagate the exception
      throw new RuntimeException(e);
    }
  }

  /**
   * Run a batch job.
   *
   * @param <T> type of the result
   * @param job the job to run
   * @return job result
   * @throws Exception if unable to compute a result
   */
  public <T> T run(Callable<T> job) throws Exception {
    Objects.requireNonNull(job);
    Objects.requireNonNull(users);

    User user = AuthUtils.getUser();
    if (user == null) {
      user = users.findByCode(DEFAULT_BATCH_USER);
    }

    batchUser.set(user);
    try {
      return job.call();
    } finally {
      batchUser.remove();
    }
  }
}
