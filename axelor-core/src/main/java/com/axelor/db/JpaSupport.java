/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import com.axelor.db.JPA.JDBCWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class provides some useful methods in JPA context, can be used as a base class for services
 * to make it more convenient.
 */
public abstract class JpaSupport {

  /** The jdbc task interface. */
  protected interface JDBCTask extends JDBCWork {}

  /**
   * Get the instance of current {@link EntityManager}.
   *
   * @return the {@link EntityManager} instance
   */
  protected EntityManager getEntityManager() {
    return JPA.em();
  }

  /**
   * Get the {@link Query} instance for the given model class.
   *
   * @param <T> the type of the model
   * @param modelClass the model class
   * @return an instance of {@link Query}
   */
  protected <T extends Model> Query<T> all(Class<T> modelClass) {
    return Query.of(modelClass);
  }

  /**
   * Run the given task inside a transaction that is committed after the task is completed.
   *
   * @param task the task to run
   */
  protected void inTransaction(Runnable task) {
    JPA.runInTransaction(task);
  }

  /**
   * Perform JDBC related task using the {@link Connection} managed by the current {@link
   * EntityManager}.
   *
   * @param task The task to be performed
   * @throws PersistenceException Generally indicates wrapped {@link SQLException}
   */
  protected void jdbcTask(JDBCTask task) {
    JPA.jdbcWork(task);
  }
}
