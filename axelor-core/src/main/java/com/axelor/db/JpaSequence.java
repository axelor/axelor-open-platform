/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.db;

import com.axelor.common.StringUtils;
import com.axelor.meta.db.MetaSequence;
import com.google.common.base.Strings;

/**
 * This class provides some helper static methods to deal with custom sequences.
 * 
 */
public final class JpaSequence {

	private JpaSequence() {
	}

	private static MetaSequence find(String name) {
		MetaSequence sequence = MetaSequence.findByName(name);
		if (sequence == null) {
			throw new IllegalArgumentException("No such sequence: " + name);
		}
		return sequence;
	}

	/**
	 * Get the next sequence value of the given sequence.<br>
	 * <br>
	 * This method must be called inside a running transaction as it updates the
	 * sequence details in database.
	 * 
	 * @param name
	 *            the name of the sequence
	 * @return next sequence value
	 */
	public static String nextValue(String name) {
		final MetaSequence sequence = find(name);
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
		sequence.save();

		return value;
	}
	
	/**
	 * Set the next numeric value for the given sequence.<br>
	 * <br>
	 * This method must be called inside a running transaction as it updates the
	 * sequence details in the database. <br>
	 * <br>
	 * This method is generally used to reset the sequence. It may cause
	 * duplicates if given next number is less then the last next value of the
	 * sequence.
	 * 
	 * @param name
	 *            the name of the sequence
	 * @param next
	 *            the next sequence number
	 */
	public static void nextValue(final String name, final long next) {
		final MetaSequence sequence = find(name);
		sequence.setNext(next);
		sequence.save();
	}
}
