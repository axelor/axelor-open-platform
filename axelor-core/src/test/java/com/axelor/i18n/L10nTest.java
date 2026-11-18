/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.Test;

public class L10nTest {

  private final L10n us = L10n.getInstance(Locale.US);
  private final L10n fr = L10n.getInstance(Locale.FRANCE);

  @Test
  public void testDecimalKeepsSignificantDigits() {
    assertEquals("60.9512", us.format(new BigDecimal("60.9512000000"), false));
    assertEquals("60,9512", fr.format(new BigDecimal("60.9512000000"), false));
    assertEquals("0.0000000001", us.format(new BigDecimal("0.0000000001"), false));
    assertEquals("-1.23456789", us.format(new BigDecimal("-1.23456789"), false));
  }

  @Test
  public void testDecimalWithoutFraction() {
    assertEquals("100", us.format(new BigDecimal("100.0000000000"), false));
    assertEquals("100", us.format(new BigDecimal("1E+2"), false));
    assertEquals("0", us.format(new BigDecimal("0E-10"), false));
  }

  @Test
  public void testDecimalGrouping() {
    assertEquals("1,234,567.891", us.format(new BigDecimal("1234567.8910"), true));
    assertEquals("1234567.891", us.format(new BigDecimal("1234567.8910"), false));
  }

  @Test
  public void testOtherNumbersUnchanged() {
    assertEquals("0.3", us.format(0.1 + 0.2, false));
    assertEquals("1.235", us.format(1.23456d, false));
    assertEquals("1234", us.format(1234, false));
    assertEquals("1,234", us.format(1234L, true));
  }

  @Test
  public void testGroupingDoesNotLeak() {
    assertEquals("1,234", us.format(1234));
    assertEquals("1234", us.format(1234, false));
    assertEquals("1,234", us.format(1234));
  }
}
