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
package com.axelor.shell.core;

import java.lang.reflect.InvocationTargetException;

public class SimpleExecutor implements Executor {

	@Override
	public Object execute(ParserResult result) throws RuntimeException {
		return invoke(result);
	}

	@Override
	public void stop() {

	}
	
	protected Object invoke(ParserResult result) {
		if (result == null || result.getMethod() == null
				|| result.getInstance() == null) {
			return null;
		}
		try {
			return result.getMethod().invoke(result.getInstance(),
					result.getArguments());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
