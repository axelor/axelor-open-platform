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
package com.axelor.meta.web;

import com.axelor.app.internal.AppFilter;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaSchedule;
import com.axelor.quartz.JobRunner;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.quartz.CronExpression;

public class JobController {

  @Inject private JobRunner jobRunner;

  public void validate(ActionRequest request, ActionResponse response) {
    String cronExpression = request.getContext().asType(MetaSchedule.class).getCron();
    try {
      CronExpression.validateExpression(cronExpression);

      response.setNotify(
          I18n.get("Valid cron. Next execution dates are :")
              + "<br/>"
              + getNextSchedule(cronExpression)
                  .stream()
                  .map(this::format)
                  .collect(Collectors.joining("<br/>")));
    } catch (Exception e) {
      response.setError(I18n.get("Invalid cron :") + " " + cronExpression);
    }
  }

  public void update(ActionRequest request, ActionResponse response) {
    try {
      jobRunner.update(request.getContext().asType(MetaSchedule.class));
      response.setNotify(I18n.get("Job has been updated."));
    } catch (Exception e) {
      response.setError(e.getMessage());
    }
  }

  public void restart(ActionRequest request, ActionResponse response) {
    try {
      jobRunner.restart();
      response.setNotify(I18n.get("All jobs have been restarted."));
    } catch (Exception e) {
      response.setError(e.getMessage());
    }
  }

  public void stop(ActionRequest request, ActionResponse response) {
    try {
      jobRunner.stop();
      response.setNotify(I18n.get("The scheduler service has been stopped."));
    } catch (Exception e) {
      response.setError(e.getMessage());
    }
  }

  private String format(Date date) {
    DateFormat dateFormat =
        SimpleDateFormat.getDateTimeInstance(
            DateFormat.DEFAULT, DateFormat.DEFAULT, AppFilter.getLocale());
    return dateFormat.format(date);
  }

  private List<Date> getNextSchedule(String cronExpression) throws ParseException {
    CronExpression expression = new CronExpression(cronExpression);
    List<Date> nextTriggerDates = new ArrayList<>();
    Date date = new Date();
    for (int i = 0; i < 5; i++) {
      Date next = expression.getNextValidTimeAfter(date);
      nextTriggerDates.add(next);
      date = next;
    }
    return nextTriggerDates;
  }
}
