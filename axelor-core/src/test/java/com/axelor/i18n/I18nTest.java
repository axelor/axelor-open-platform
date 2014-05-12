package com.axelor.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.meta.db.MetaTranslation;
import com.google.inject.persist.Transactional;

public class I18nTest extends JpaTest {

	@Before
	@Transactional
	public void setup() {
		if (MetaTranslation.all().count() > 0) {
			return;
		}
		MetaTranslation obj;
		
		obj = new MetaTranslation();
		obj.setKey("Hello World!!!");
		obj.setMessage("Hello...");
		obj.setLanguage("en");
		obj.save();

		obj = new MetaTranslation();
		obj.setKey("{0} record selected.");
		obj.setMessage("{0} record selected.");
		obj.setLanguage("en");
		obj.save();

		obj = new MetaTranslation();
		obj.setKey("{0} records selected.");
		obj.setMessage("{0} records selected.");
		obj.setLanguage("en");
		obj.save();
	}

	@Test
	public void test() {
		
		// test simple
		assertEquals("Hello...", I18n.get("Hello World!!!"));
		
		// test plural
		assertEquals("1 record selected.", I18n.get("{0} record selected.", "{0} records selected.", 1));
		assertEquals("5 records selected.", I18n.get("{0} record selected.", "{0} records selected.", 5));
	}
}
