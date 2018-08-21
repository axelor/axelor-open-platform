/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import com.axelor.db.JPA.JDBCWork;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

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
