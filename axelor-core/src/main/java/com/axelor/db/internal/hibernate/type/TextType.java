/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.db.internal.hibernate.type;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;

import com.axelor.db.internal.DBHelper;

public class TextType extends AbstractSingleColumnStandardBasicType<String> {

	private static final long serialVersionUID = 4588030861360758449L;

	public static final TextType INSTANCE = new TextType();

	public TextType() {
		super(DBHelper.isOracle() ? ClobTypeDescriptor.DEFAULT : LongVarcharTypeDescriptor.INSTANCE,
				StringTypeDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "text";
	}
}
