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
package com.axelor.tomcat;

public class TomcatRunner {

	public static void main(String[] args) {
		final TomcatOptions options = new TomcatOptions();
		try {
			options.parse(args);
		} catch (Exception e) {
			options.usage();
			System.exit(1);
		}
		if (options.hasHelp()) {
			options.usage();
			System.exit(1);
		}

		final TomcatServer server = new TomcatServer(options);
		server.start();
	}
}
