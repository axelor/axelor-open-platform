/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
