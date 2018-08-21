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
package com.axelor.inject.logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

@RunWith(GuiceRunner.class)
@GuiceModules({LoggerModule.class})
public class TestLogger {

  private Logger log1;

  @Inject private Logger log2;

  @Inject private TestLoggerService service;

  @Inject
  public TestLogger(Logger log1) {
    this.log1 = log1;
  }

  @Test
  public void testContructorInject() {
    assertNotNull(log1);
    assertEquals(TestLogger.class.getName(), log1.getName());
  }

  @Test
  public void testMemberInject() {
    assertNotNull(log2);
    assertEquals(TestLogger.class.getName(), log2.getName());
  }

  @Test
  public void testServiceInject() {
    assertNotNull(service);
    assertNotNull(service.getLog());
    assertEquals(TestLoggerService.class.getName(), service.getLog().getName());
  }
}
