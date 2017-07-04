/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;

/**
 * The {@link Inflector} provides various methods to transform words to plural,
 * singular, titles, class names, table names etc.
 * 
 */
public final class Inflector {

	private static final Inflections INFLECTIONS_EN = Inflections.getInstance();
	private static final Inflector INSTANCE = new Inflector();

	private Inflector() {
		initEnglishRules();
	}
	
	public static Inflector getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the plural form of the word in the given string.
	 * 
	 * <pre>
	 * inflection.pluralize(&quot;post&quot;); // &quot;posts&quot;
	 * inflection.pluralize(&quot;child&quot;); // &quot;children&quot;
	 * inflection.pluralize(&quot;octopus&quot;); // &quot;octopi&quot;
	 * inflection.pluralize(&quot;sheep&quot;); // &quot;sheep&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the string to pluralize
	 * @return pluralized string
	 */
	public String pluralize(String word) {
		return INFLECTIONS_EN.pluralize(word);
	}
	
	/**
	 * Returns the singular form of the word in the given string.
	 * 
	 * <pre>
	 * inflection.singularize(&quot;posts&quot;); // &quot;post&quot;
	 * inflection.singularize(&quot;children&quot;); // &quot;child&quot;
	 * inflection.singularize(&quot;octopi&quot;); // &quot;octopus&quot;
	 * inflection.singularize(&quot;sheep&quot;); // &quot;sheep&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the string to singularize
	 * @return singularized string
	 */
	public String singularize(String word) {
		return INFLECTIONS_EN.singularize(word);
	}
	
	/**
	 * Convert the given word to camel case.
	 * 
	 * <pre>
	 * inflection.camelcase(&quot;address_book&quot;, false); // &quot;AddressBook&quot;
	 * inflection.camelcase(&quot;address-book&quot;, true); // &quot;addressBook&quot;
	 * inflection.camelcase(&quot;Address book&quot;, false); // &quot;AddressBook&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the word to convert
	 * @param lower
	 *            whether to create lower camel case
	 * @return camel case string
	 */
	public String camelize(String word, boolean lower) {
		final CaseFormat target = lower ? CaseFormat.LOWER_CAMEL : CaseFormat.UPPER_CAMEL;
		return CaseFormat.LOWER_UNDERSCORE.to(target, underscore(word));
	}
	
	/**
	 * Convert the given word to camel case.
	 * 
	 * <pre>
	 * inflection.camelcase(&quot;address_book&quot;); // &quot;AddressBook&quot;
	 * inflection.camelcase(&quot;address-book&quot;); // &quot;AddressBook&quot;
	 * inflection.camelcase(&quot;Address book&quot;); // &quot;AddressBook&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the word to convert
	 * @return camel case string
	 */
	public String camelize(String word) {
		return camelize(word, false);
	}
	
	/**
	 * Convert the given string to underscored, lowercase form.
	 * 
	 * <pre>
	 * inflection.underscore(&quot;AddressBook&quot;); // &quot;address_book&quot;
	 * inflection.underscore(&quot;address-book&quot;); // &quot;address_book&quot;
	 * inflection.underscore(&quot;Address book&quot;); // &quot;address_book&quot;
	 * </pre>
	 * 
	 * @param camelCase
	 *            the string to convert
	 * @return converted string
	 */
	public String underscore(String camelCase) {
		Preconditions.checkNotNull(camelCase);
		return camelCase.trim()
				   .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
				   .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
				   .replaceAll("[-\\s]+", "_")
				   .toLowerCase();
	}

	/**
	 * Convert the given word to human readable form.
	 * 
	 * <pre>
	 * inflection.humanize(&quot;contact_id&quot;); // &quot;Contact&quot;
	 * inflection.humanize(&quot;address_list&quot;); // &quot;Addresses&quot;
	 * inflection.humanize(&quot;_id&quot;); // &quot;Id&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the string to convert
	 * @return converted string
	 */
	public String humanize(String word) {
		Preconditions.checkNotNull(word);
		String result = underscore(word)
				.replaceAll("_id$", "")
				.replaceAll("\\A_+", "")
				.replaceAll("[_\\s]+", " ");
		return capitalize(result);
	}
	
	/**
	 * Convert the given string to nicer looking title string.
	 * 
	 * <pre>
	 * inflection.titleize(&quot;address_book&quot;); // &quot;Address Book&quot;
	 * inflection.titleize(&quot;My address_book&quot;); // &quot;My Address Book&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the string to convert
	 * @return converted string
	 */
	public String titleize(String word) {
		return capitalize(humanize(underscore(word)));
	}
	
	/**
	 * Convert the given word to table name.
	 * 
	 * <pre>
	 * inflection.tableize(&quot;AddressBook&quot;); // &quot;address_books&quot;
	 * inflection.tableize(&quot;Contact&quot;); // &quot;contacts&quot;
	 * </pre>
	 * 
	 * @param camelCase
	 *            the string to convert
	 * @return converted string
	 */
	public String tableize(String camelCase) {
		return pluralize(underscore(camelCase));
	}

	/**
	 * Convert the given word to class name.
	 * 
	 * <pre>
	 * inflection.tableize(&quot;address_books&quot;); // &quot;AddressBook&quot;
	 * inflection.tableize(&quot;contacts&quot;); // &quot;Contact&quot;
	 * </pre>
	 * 
	 * @param text
	 *            the string to convert
	 * @return converted string
	 */
	public String classify(String text) {
		return camelize(underscore(singularize(text)));
	}
	
	/**
	 * Convert the give word to dasherized form.
	 * 
	 * <pre>
	 * inflection.tableize(&quot;address_books&quot;); // &quot;address-book&quot;
	 * inflection.tableize(&quot;AddressBook&quot;); // &quot;address-book&quot;
	 * </pre>
	 * 
	 * @param word
	 *            the string to convert
	 * @return converted string
	 */
	public String dasherize(String word) {
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, underscore(word));
	}

	/**
	 * Capitalize the given string.
	 * 
	 * @param word
	 *            the string to convert
	 * @return converted string
	 */
	public String capitalize(String word) {
		Preconditions.checkNotNull(word);
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}
	
	/**
	 * Return ordinal suffixed string for the given number.
	 * 
	 * <pre>
	 * inflection.ordinalize(1); // &quot;1st&quot;
	 * inflection.ordinalize(2); // &quot;2nd&quot;
	 * inflection.ordinalize(3); // &quot;3rd&quot;
	 * inflection.ordinalize(100); // &quot;100th&quot;
	 * inflection.ordinalize(103); // &quot;103rd&quot;
	 * inflection.ordinalize(10013); // &quot;10013th&quot;
	 * </pre>
	 * 
	 * @param number
	 *            the number to suffix
	 * @return the converted string
	 */
	public String ordinalize(int number) {
		int mod100 = number % 100;
	    if (mod100 == 11 || mod100 == 12 || mod100 == 13) {
	      return String.valueOf(number) + "th";
	    }
		switch (number % 10) {
		case 1: return number + "st";
		case 2: return number + "nd";
		case 3: return number + "rd";
		}
		return number + "th";
	}

	/**
	 * Shorten the given text upto the length by appending three ellipses.
	 * 
	 * @param text text to shorten
	 * @param length desired length 
	 * @return shortened text
	 */
	public String ellipsize(String text, int length) {
		Preconditions.checkNotNull(text);
		if (text.length() <= length) return text;
		if (length < 4) return "...";
		return text.substring(0, length - 3) + "...";
	}
	
	/**
	 * Simplify the text to its unaccented version.
	 * <p>
	 * It uses
	 * {@link Normalizer#normalize(CharSequence, java.text.Normalizer.Form)}
	 * with {@link Form#NFD} normalization and then replaces accented characters
	 * with their equivalent unaccented characters.
	 * 
	 * @param text
	 *            the text to normalize
	 * @return normalized text
	 */
	public String simplify(String text) {
		Preconditions.checkNotNull(text);
		return Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}", "");
	}
	
	private void initEnglishRules() {
		
		Inflections inflect = INFLECTIONS_EN;
		
		inflect.plural("$", "s");
	    inflect.plural("s$", "s");
	    inflect.plural("^(ax|test)is$", "$1es");
	    inflect.plural("(octop|vir)us$", "$1i");
	    inflect.plural("(octop|vir)i$", "$1i");
	    inflect.plural("(alias|status)$", "$1es");
	    inflect.plural("(bu)s$", "$1ses");
	    inflect.plural("(buffal|tomat)o$", "$1oes");
	    inflect.plural("([ti])um$", "$1a");
	    inflect.plural("([ti])a$", "$1a");
	    inflect.plural("sis$", "ses");
	    inflect.plural("(?:([^f])fe|([lr])f)$", "$1$2ves");
	    inflect.plural("(hive)$", "$1s");
	    inflect.plural("([^aeiouy]|qu)y$", "$1ies");
	    inflect.plural("(x|ch|ss|sh)$", "$1es");
	    inflect.plural("(matr|vert|ind)(?:ix|ex)$", "$1ices");
	    inflect.plural("^(m|l)ouse$", "$1ice");
	    inflect.plural("^(m|l)ice$", "$1ice");
	    inflect.plural("^(ox)$", "$1en");
	    inflect.plural("^(oxen)$", "$1");
	    inflect.plural("(quiz)$", "$1zes");

		inflect.singular("s$", "");
	    inflect.singular("(ss)$", "$1");
	    inflect.singular("(n)ews$", "$1ews");
	    inflect.singular("([ti])a$", "$1um");
	    inflect.singular("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)(sis|ses)$", "$1sis");
	    inflect.singular("(^analy)(sis|ses)$", "$1sis");
	    inflect.singular("([^f])ves$", "$1fe");
	    inflect.singular("(hive)s$", "$1");
	    inflect.singular("(tive)s$", "$1");
	    inflect.singular("([lr])ves$", "$1f");
	    inflect.singular("([^aeiouy]|qu)ies$", "$1y");
	    inflect.singular("(s)eries$", "$1eries");
	    inflect.singular("(m)ovies$", "$1ovie");
	    inflect.singular("(x|ch|ss|sh)es$", "$1");
	    inflect.singular("^(m|l)ice$", "$1ouse");
	    inflect.singular("(bus)(es)?$", "$1");
	    inflect.singular("(o)es$", "$1");
	    inflect.singular("(shoe)s$", "$1");
	    inflect.singular("(cris|test)(is|es)$", "$1is");
	    inflect.singular("^(a)x[ie]s$", "$1xis");
	    inflect.singular("(octop|vir)(us|i)$", "$1us");
	    inflect.singular("(alias|status)(es)?$", "$1");
	    inflect.singular("^(ox)en", "$1");
	    inflect.singular("(vert|ind)ices$", "$1ex");
	    inflect.singular("(matr)ices$", "$1ix");
	    inflect.singular("(quiz)zes$", "$1");
	    inflect.singular("(database)s$", "$1");

	    inflect.irregular("person", "people");
	    inflect.irregular("man", "men");
	    inflect.irregular("child", "children");
	    inflect.irregular("sex", "sexes");
	    inflect.irregular("move", "moves");
	    inflect.irregular("zombie", "zombies");
	    inflect.irregular("stadium", "stadiums");
	    
	    inflect.ignore("equipment information rice money species series fish sheep jeans police data".split(" "));
	}
}
