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
package com.axelor;

import com.axelor.db.JpaFixture;
import com.axelor.db.JpaSupport;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@RunWith(GuiceRunner.class)
@GuiceModules({JpaTestModule.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class JpaTest extends JpaSupport {

  @Inject private JpaFixture fixture;

  @Before
  @Transactional
  public void setUp() {
    if (all(Contact.class).count() == 0) {
      fixture.load("demo-data.yml");
    }
  }

  protected void fixture(String name) {
    fixture.load(name);
  }
}
