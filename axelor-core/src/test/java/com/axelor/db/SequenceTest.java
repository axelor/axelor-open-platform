/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.JpaTest;
import com.axelor.meta.db.MetaSequence;
import com.google.inject.persist.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SequenceTest extends JpaTest {

  @BeforeEach
  public void setUp() {
    if (Query.of(MetaSequence.class).count() == 0) {
      fixture("sequence-data.yml");
    }
  }

  @Test
  @Transactional
  public void test() {
    assertEquals("EMP_00001_ID", JpaSequence.nextValue("seq.emp.id"));
    assertEquals("EMP_00002_ID", JpaSequence.nextValue("seq.emp.id"));
    assertEquals("EMP_00003_ID", JpaSequence.nextValue("seq.emp.id"));

    JpaSequence.nextValue("seq.emp.id", 100);

    assertEquals("EMP_00100_ID", JpaSequence.nextValue("seq.emp.id"));
  }
}
