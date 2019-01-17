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
package com.axelor.i18n;

import static org.junit.Assert.assertEquals;

import com.axelor.JpaTest;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.repo.MetaTranslationRepository;
import com.google.inject.persist.Transactional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

public class I18nTest extends JpaTest {

  @Inject private MetaTranslationRepository translations;

  @Before
  @Transactional
  public void setup() {
    if (translations.all().count() > 0) {
      return;
    }
    MetaTranslation obj;

    obj = new MetaTranslation();
    obj.setKey("Hello World!!!");
    obj.setMessage("Hello...");
    obj.setLanguage("en");
    obj = translations.save(obj);

    obj = new MetaTranslation();
    obj.setKey("{0} record selected.");
    obj.setMessage("{0} record selected.");
    obj.setLanguage("en");
    obj = translations.save(obj);

    obj = new MetaTranslation();
    obj.setKey("{0} records selected.");
    obj.setMessage("{0} records selected.");
    obj.setLanguage("en");
    obj = translations.save(obj);
  }

  @Test
  public void test() {

    // test simple
    assertEquals("Hello...", I18n.get("Hello World!!!"));

    // test plural
    assertEquals(
        "1 record selected.", I18n.get("{0} record selected.", "{0} records selected.", 1));
    assertEquals(
        "5 records selected.", I18n.get("{0} record selected.", "{0} records selected.", 5));
  }
}
