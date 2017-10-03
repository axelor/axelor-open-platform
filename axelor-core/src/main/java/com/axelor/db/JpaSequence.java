/**
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
package com.axelor.db;

import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaSequence;
import com.axelor.meta.db.repo.MetaSequenceRepository;
import com.google.common.base.Strings;

/**
 * This class provides some helper static methods to deal with custom sequences.
 * 
 */
public final class JpaSequence {

	private JpaSequence() {
	}

	private static MetaSequence find(String name) {
		final MetaSequenceRepository repo = Beans.get(MetaSequenceRepository.class);
		final MetaSequence sequence =  repo.findByName(name);
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

		JPA.em().persist(sequence);

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
		JPA.em().persist(sequence);
	}
}
