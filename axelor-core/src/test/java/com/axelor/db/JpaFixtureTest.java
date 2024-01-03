/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.JpaTestModule;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.FieldTypes;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({JpaTestModule.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JpaFixtureTest extends JpaSupport {

  @Inject private JpaFixture fixture;

  private FieldTypes fieldTypes;

  @BeforeEach
  @Transactional
  public void setUp() {
    if (all(FieldTypes.class).count() == 0) {
      fixture.load("demo-field-types.yml");
    }
  }

  @Test
  public void testString() {
    whenLoadTypeFields();
    expectString();
  }

  private void expectString() {
    assertEquals("Hello, Axelor!", fieldTypes.getString());
  }

  @Test
  public void testLocalDate() {
    whenLoadTypeFields();
    expectLocalDate();
  }

  private void expectLocalDate() {
    assertEquals(LocalDate.parse("2021-04-29"), fieldTypes.getLocalDate());
  }

  @Test
  public void testLocalDateTime() {
    whenLoadTypeFields();
    expectLocalDateTime();
  }

  private void expectLocalDateTime() {
    assertEquals(LocalDateTime.of(2021, 4, 29, 7, 57, 0), fieldTypes.getLocalDateTime());
  }

  @Test
  public void testLocalTime() {
    whenLoadTypeFields();
    expectLocalTime();
  }

  private void expectLocalTime() {
    assertEquals(LocalTime.of(7, 57), fieldTypes.getLocalTime());
  }

  private void whenLoadTypeFields() {
    fieldTypes = all(FieldTypes.class).fetchOne();
  }
}
