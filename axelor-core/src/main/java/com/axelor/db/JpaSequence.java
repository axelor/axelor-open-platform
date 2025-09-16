/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import com.axelor.common.StringUtils;
import com.axelor.meta.db.MetaSequence;
import com.google.common.base.Strings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

/** This class provides some helper static methods to deal with custom sequences. */
public final class JpaSequence {

  private JpaSequence() {}

  private static MetaSequence find(EntityManager em, String name) {
    final MetaSequence sequence =
        em
            .createQuery(
                "SELECT self FROM MetaSequence self WHERE self.name = :name", MetaSequence.class)
            .setParameter("name", name)
            .setFlushMode(FlushModeType.COMMIT)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    if (sequence == null) {
      throw new IllegalArgumentException("No such sequence: " + name);
    }
    return sequence;
  }

  /**
   * Get the next sequence value of the given sequence.<br>
   * <br>
   * This method must be called inside a running transaction as it updates the sequence details in
   * database.
   *
   * @param name the name of the sequence
   * @return next sequence value
   */
  public static String nextValue(String name) {
    return nextValue(JPA.em(), name);
  }

  /**
   * Get the next sequence value of the given sequence.<br>
   * <br>
   * This method must be called inside a running transaction as it updates the sequence details in
   * database.
   *
   * @param em the entity manager
   * @param name the name of the sequence
   * @return next sequence value
   */
  public static String nextValue(EntityManager em, String name) {
    final MetaSequence sequence = find(em, name);
    final Long next = sequence.getNext();
    final String prefix = sequence.getPrefix();
    final String suffix = sequence.getSuffix();
    final Integer padding = sequence.getPadding();

    String value = "" + next;
    if (padding > 0) {
      value = Strings.padStart(value, padding, '0');
    }
    if (!StringUtils.isBlank(prefix)) {
      value = prefix + value;
    }
    if (!StringUtils.isBlank(suffix)) {
      value = value + suffix;
    }

    sequence.setNext(next + sequence.getIncrement());

    em.persist(sequence);

    return value;
  }

  /**
   * Set the next numeric value for the given sequence.<br>
   * <br>
   * This method must be called inside a running transaction as it updates the sequence details in
   * the database. <br>
   * <br>
   * This method is generally used to reset the sequence. It may cause duplicates if given next
   * number is less then the last next value of the sequence.
   *
   * @param name the name of the sequence
   * @param next the next sequence number
   */
  public static void nextValue(final String name, final long next) {
    final MetaSequence sequence = find(JPA.em(), name);
    sequence.setNext(next);
    JPA.em().persist(sequence);
  }

  /**
   * Set the next numeric value for the given sequence.<br>
   * <br>
   * This method must be called inside a running transaction as it updates the sequence details in
   * the database. <br>
   * <br>
   * This method is generally used to reset the sequence. It may cause duplicates if given next
   * number is less then the last next value of the sequence.
   *
   * @param em the entity manager
   * @param name the name of the sequence
   * @param next the next sequence number
   */
  public static void nextValue(final EntityManager em, final String name, final long next) {
    final MetaSequence sequence = find(em, name);
    sequence.setNext(next);
    JPA.em().persist(sequence);
  }
}
