/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.quartz;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

public class JobCleaner extends JobListenerSupport {

  private static final String NAME = "JobCleaner";

  @Inject private Provider<EntityManager> emp;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    emp.get().clear();
  }
}
