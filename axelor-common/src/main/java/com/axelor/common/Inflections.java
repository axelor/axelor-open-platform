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
package com.axelor.common;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The {@link Inflections} defines rules for singular, plural inflections. */
public class Inflections {

  private static final Map<String, Inflections> INSTANCES = new ConcurrentHashMap<>();
  private static final String DEFAULT_LANG = "en";

  private Set<String> ignored = new HashSet<>();

  private List<Rule> singulars = new LinkedList<>();
  private List<Rule> plurals = new LinkedList<>();

  private Inflections() {}

  /**
   * Get the default instance of {@link Inflections} for the English.
   *
   * @return an instance of {@link Inflections}
   */
  public static Inflections getInstance() {
    return getInstance(DEFAULT_LANG);
  }

  /**
   * Get the default instance of {@link Inflections} for the given language.
   *
   * @param language the language
   * @return an instance of {@link Inflections}
   */
  public static Inflections getInstance(String language) {
    Inflections instance = INSTANCES.get(language);
    if (instance == null) {
      instance = new Inflections();
      INSTANCES.put(language, instance);
    }
    return instance;
  }

  private String capitalize(String word) {
    return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
  }

  /**
   * Add words to ignore for inflections.
   *
   * @param words the words to ignore.
   */
  public void ignore(String... words) {
    if (words != null) {
      for (String word : words) {
        ignored.add(word.toLowerCase());
      }
    }
  }

  /**
   * Add irregular singular, plural words.
   *
   * @param singular the singular word
   * @param plural the plural word
   */
  public void irregular(String singular, String plural) {
    plurals.add(0, new Rule(singular.toLowerCase(), plural.toLowerCase(), true));
    plurals.add(0, new Rule(capitalize(singular), capitalize(plural), true));
    singulars.add(0, new Rule(plural.toLowerCase(), singular.toLowerCase(), true));
    singulars.add(0, new Rule(capitalize(plural), capitalize(singular), true));
  }

  /**
   * Add rule for converting plurals to singular.
   *
   * @param pattern the pattern to match plural
   * @param replacement the replacement text
   */
  public void singular(String pattern, String replacement) {
    singulars.add(0, new Rule(pattern, replacement, false));
  }

  /**
   * Add rule for converting singular to plural.
   *
   * @param pattern the pattern to match singular
   * @param replacement the replacement text
   */
  public void plural(String pattern, String replacement) {
    plurals.add(0, new Rule(pattern, replacement, false));
  }

  /**
   * Apply the given list of inflection rules on the provided word.
   *
   * @param word the word on which to apply the rules
   * @param rules the inflection rules
   * @return the inflected text
   */
  protected String apply(String word, List<Rule> rules) {
    if (word == null || "".equals(word.trim())) return word;
    if (ignored.contains(word.toLowerCase())) return word;
    for (Rule rule : rules) {
      String result = rule.apply(word);
      if (result != null) return result;
    }
    return word;
  }

  /**
   * Convert the given word to it's singular form.
   *
   * @param word the word to convert
   * @return the converted text
   */
  public String singularize(String word) {
    return apply(word, singulars);
  }

  /**
   * Convert the given word to it's plural form.
   *
   * @param word the word to convert
   * @return the converted text
   */
  public String pluralize(String word) {
    return apply(word, plurals);
  }

  static class Rule {

    private String pattern;
    private String replacement;

    private Pattern regex;

    public Rule(String pattern, String replacement, boolean simple) {
      this.pattern = pattern;
      this.replacement = replacement;
      if (!simple) {
        this.regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
      }
    }

    public String apply(String input) {
      if (input == null) return null;
      if (input.trim().equals("")) return null;
      if (regex == null) {
        return pattern.equals(input) ? replacement : null;
      }
      final Matcher matcher = regex.matcher(input);
      if (matcher.find()) {
        return matcher.replaceAll(replacement);
      }
      return null;
    }

    @Override
    public int hashCode() {
      return pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null) return false;
      if (obj instanceof Rule && pattern.equals(((Rule) obj).pattern)) {
        return true;
      }
      return false;
    }
  }
}
