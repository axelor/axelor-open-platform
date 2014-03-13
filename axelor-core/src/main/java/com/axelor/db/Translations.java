/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import javax.inject.Provider;

import com.google.inject.ImplementedBy;

@ImplementedBy(Translations.Dummy.class)
public interface Translations {

	static class Dummy implements Translations, Provider<Translations> {

		@Override
		public Translations get() {
			return new Dummy();
		}

		@Override
		public String get(String key) {
			return key;
		}
		
		@Override
		public String get(String key, String defaultValue) {
			return key;
		}

		@Override
		public String get(String key, String defaultValue, String domain) {
			return key;
		}
		
		@Override
		public String get(String key, String defaultValue, String domain, String type) {
			return key;
		}
	}

	String get(String key);
	
	String get(String key, String defaultValue);
	
	String get(String key, String defaultValue, String domain);
	
	String get(String key, String defaultValue, String domain, String type);
}
